package com.aurea.testgenerator.generation.patterns.delegate

import com.aurea.testgenerator.generation.AbstractMethodTestGenerator
import com.aurea.testgenerator.generation.TestGeneratorResult
import com.aurea.testgenerator.generation.TestType
import com.aurea.testgenerator.generation.ast.DependableNode
import com.aurea.testgenerator.generation.names.NomenclatureFactory
import com.aurea.testgenerator.generation.source.Imports
import com.aurea.testgenerator.reporting.CoverageReporter
import com.aurea.testgenerator.reporting.TestGeneratorResultReporter
import com.aurea.testgenerator.source.Unit
import com.aurea.testgenerator.value.MockValueFactory
import com.aurea.testgenerator.value.ValueFactory
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import groovy.util.logging.Log4j2
import org.apache.commons.lang3.NotImplementedException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@SuppressWarnings("unused")
@Component
@Profile("manual")
@Log4j2
class DelegateTestGenerator extends AbstractMethodTestGenerator {

    ValueFactory factory
    MockValueFactory mockFactory
    def methodCounter = [:]

    @Autowired
    DelegateTestGenerator(JavaParserFacade solver, TestGeneratorResultReporter reporter,
                          CoverageReporter visitReporter, NomenclatureFactory nomenclatures,
                          ValueFactory factory, MockValueFactory mockFactory) {
        super(solver, reporter, visitReporter, nomenclatures)
        this.factory = factory
        this.mockFactory = mockFactory
    }

    @Override
    protected TestGeneratorResult generate(MethodDeclaration method, Unit unit) {
        def result = new TestGeneratorResult()

        if (!method.body.present) {
            return result
        }

        List<MethodCallExpr> calls = new ArrayList<>()
        visitStatement(method.body.get(), calls)
        if (calls.isEmpty()) {
            return result
        }

        def instanceCode = """${unit.className} classInstance = spy(${unit.className}.class);"""
        def callsMock = ""
        def verifyBlock = ""
        def params = getMethodParamsCode(method)
        def counter = 0
//        def testCase = new DelegateTestCaseGenerator();
        calls.forEach { call ->

            ResolvedFieldDeclaration fieldDecl = null
            ResolvedParameterDeclaration paramDecl = null
            if (call.scope.present && (call.scope.get().isNameExpr() || call.scope.get().isFieldAccessExpr())) {
                def solvedScope = solver.solve(call.scope.get());
                if (solvedScope.correspondingDeclaration.isField()) {
                    fieldDecl = solvedScope.correspondingDeclaration.asField()
                } else if (solvedScope.correspondingDeclaration.isParameter()) {
                    paramDecl = solvedScope.correspondingDeclaration.asParameter()
                } else {
                    throw new NotImplementedException('Cannot handle: ' + call.scope);
                }
            }

            def solved = solver.solve(call)
            if (!solved.solved) {
                throw new IllegalStateException('Cannot handle: ' + call);
            }

            def returnType = solved.correspondingDeclaration.returnType
            def mockBlock = "";
            if (returnType.isVoid()) {
                mockBlock += "doNothing()"
            } else {
                def objName = returnType.isPrimitive() ? returnType.asPrimitive().describe() : returnType.asReferenceType().typeDeclaration.name
                mockBlock += "${objName} expected_${++counter} = ${this.factory.getExpression(returnType).get()};\n"
                mockBlock += "doReturn(expected_${counter})"
            }

            def mockMethodCall = ".${call.name.toString()}(${this.getMethodCallMockParams(call)});\n";
            def callingObj;
            if (fieldDecl != null) {
                callingObj = "delegate_${fieldDecl.name}";
                mockBlock += ".when(${callingObj})${mockMethodCall}";
                if (fieldDecl.getType().isReferenceType()) {
                    def typeName = getTypeReference(fieldDecl.getType(), unit)
                    mockBlock = "${typeName} delegate_${fieldDecl.name} = mock(${typeName}.class);\n" + mockBlock + "\nReflectionTestUtils.setField(classInstance, \"${fieldDecl.name}\", delegate_${fieldDecl.name});"
                } else {
                    mockBlock = mockBlock + "\nReflectionTestUtils.setField(classInstance, \"${fieldDecl.name}\", delegate_${fieldDecl.name});"
                }
            } else if (paramDecl != null) {
                callingObj = "delegate_${paramDecl.name}";
                mockBlock += ".when(${callingObj})${mockMethodCall}";
                def typeName = getTypeReference(paramDecl.getType(), unit)
                mockBlock = "${typeName}  delegate_${paramDecl.name} = mock(${typeName}.class);\n" + mockBlock
                params.put(paramDecl.name, "delegate_${paramDecl.name}")
            } else {
                callingObj = "classInstance"
                mockBlock += ".when(${callingObj})${mockMethodCall}";
            }

            callsMock += mockBlock
//            callsMock += testCase.generateArrange()
            verifyBlock += "verify(${callingObj}, atLeast(1)).${call.name.toString()}(${this.getMethodVerifyParams(call, params)});\n"
//            verifyBlock += testCase.generateAssert()
        }

        def parentMethodParams = params.collect { p -> p.value }.join(", ")
        def actBlock
        if (method.type.isVoidType()) {
            actBlock = "classInstance.${method.name}(${parentMethodParams});"
        } else {
            actBlock = "${method.type.toString()} actual = classInstance.${method.name}(${parentMethodParams});\n" +
                    "assertEquals(expected_${counter}, actual);"
        }

        return addTest(result, method, """@Test
            public void test${this.generateMethodName(method, unit)}() {
                // arrange
                ${instanceCode}    
                ${callsMock}   

                // act                    
                ${actBlock}

                // assert    
                ${verifyBlock}
            }""")
    }

    private static String getTypeReference(ResolvedType resolvedType, Unit unit) {
        def typeDeclaration = resolvedType.asReferenceType().typeDeclaration
        if (typeDeclaration.qualifiedName == unit.packageName + "." + typeDeclaration.name) {
            return typeDeclaration.name;
        }
        return typeDeclaration.qualifiedName
    }

    private void visitStatement(Node node, List<MethodCallExpr> calls) {
        if (node instanceof Statement) {
            def stmt = node as Statement

            //skip conditional statements
            if (stmt.isBlockStmt() || stmt.isExpressionStmt() || stmt.isLabeledStmt() || stmt.isReturnStmt() || stmt.isSynchronizedStmt()) {
                node.getChildNodes().forEach { child -> visitStatement(child, calls) }
            }
            return
        }
        if (node instanceof MethodCallExpr) {
            def methodCall = node as MethodCallExpr
            if (!methodCall.scope.present || methodCall.scope.get().isThisExpr() || methodCall.scope.get().isNameExpr() || methodCall.scope.get().isFieldAccessExpr()) {
                calls.add(methodCall)
            }
            return
        }
        if (node instanceof VariableDeclarator || node instanceof Expression) {
            node.getChildNodes().forEach { child -> visitStatement(child, calls) }
        }
    }

    @Override
    protected TestType getType() {
        return DelegateTypes.METHOD
    }

    protected static TestGeneratorResult addTest(TestGeneratorResult result, MethodDeclaration method, String testCode) {
        def testMethod = new DependableNode<>()
        testMethod.node = JavaParser.parseBodyDeclaration(testCode).asMethodDeclaration()
        testMethod.dependency.imports.addAll(method.static ? [Imports.JUNIT_TEST] : [Imports.JUNIT_TEST, Imports.JUNIT_EQUALS,  Imports.MOCKITO_ANY, Imports.REFLECTION_UTILS])
        result.tests << testMethod
        result
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected String getMethodVerifyParams(MethodCallExpr callExpr, Map<String, String> params) {
        return callExpr.arguments.stream().map { a ->
            if (a.isLiteralExpr()) {
                return a.asLiteralExpr().toString();
            }
            def declaration = solver.solve(a).correspondingDeclaration
            if (declaration.isField()) {
                return mockFactory.getExpression(declaration.getType())
            }
            return params.get(a.asNameExpr().name.asString())
        }.collect().join(", ")
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected String getMethodCallMockParams(MethodCallExpr callExpr) {
        return callExpr.arguments.stream().map {
            arg -> arg.isLiteralExpr() ? arg.asLiteralExpr().toString() : mockFactory.getExpression(solver.solve(arg).correspondingDeclaration.getType())
        }.collect().join(", ")
    }

    protected Map<String, String> getMethodParamsCode(MethodDeclaration method) {
        return method.parameters.collectEntries {
            p -> [p.name.asString(), generateTestValue(p)]
        }
    }

    protected String generateTestValue(Parameter param) {
        return factory.getExpression(param.type.resolve()).get()
    }

    protected String generateMethodName(MethodDeclaration method, Unit unit) {
        def capitalized = method.getNameAsString().capitalize()
        def key = unit.fullName + "#" + capitalized
        def counter = methodCounter.getOrDefault(key, 0)
        methodCounter.put(key, counter + 1)
        return counter == 0 ? capitalized : (capitalized + "_" + (counter + 1));
    }
}
