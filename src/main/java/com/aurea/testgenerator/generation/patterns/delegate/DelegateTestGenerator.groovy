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
import com.aurea.testgenerator.value.ValueFactory
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.resolution.types.ResolvedVoidType
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@SuppressWarnings("unused")
@Component
@Profile("manual")
@Log4j2
class DelegateTestGenerator extends AbstractMethodTestGenerator {

    def factory
    def methodCounter = [:]

    @Autowired
    DelegateTestGenerator(JavaParserFacade solver, TestGeneratorResultReporter reporter,
                          CoverageReporter visitReporter, NomenclatureFactory nomenclatures,
                          ValueFactory factory) {
        super(solver, reporter, visitReporter, nomenclatures)
        this.factory = factory
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

        def instanceCode = """${unit.className} classInstance = spy((${unit.className})ValidationHelper.getBasicInstance(PojoClassFactory.getPojoClass(${unit.className}.class)));"""
        def callsMock = ""
        def verifyBlock = ""
        def params = this.getMethodParamsCode(method)
        def counter = 0
        calls.forEach {call ->
            def returnType = solver.solve(call).correspondingDeclaration.returnType
            def isVoid = ResolvedVoidType.INSTANCE == returnType
            if (isVoid) {
                callsMock += "doNothing()"
            } else {
                def objName = returnType.asReferenceType().typeDeclaration.name
                callsMock += "${objName} expected${++counter} = (${objName}) ValidationHelper.getBasicInstance(PojoClassFactory.getPojoClass(${objName}.class));\n"
                callsMock += "doReturn(expected${counter})"
            }
            callsMock += ".when(classInstance).${call.name.toString()}(${this.getMethodCallParams(call)});\n"
            verifyBlock += "verify(classInstance, atLeast(1)).${call.name.toString()}(${this.getMethodVerifyParams(call, params)});\n"
        }

        def parentMethodParams = params.collect { p -> p.value }.join(", ")
        def actBlock
        if (method.type.isVoidType()) {
            actBlock = "classInstance.${method.name}(${parentMethodParams});"
        } else {
            actBlock = "${method.type.toString()} actual = classInstance.${method.name}(${parentMethodParams});\n" +
                    "assertEquals(actual, expected${counter});"
        }

        def methodBody = """@Test
            public void test${this.generateMethodName(method, unit)}() {
                // arrange
                ${instanceCode}    
                ${callsMock}   

                // act                    
                ${actBlock}

                // assert    
                ${verifyBlock}
            }"""

        addTest(result, method, methodBody)

        result
    }

    private void visitStatement(Node node, List<MethodCallExpr> calls) {
        if (node instanceof Statement) {
            def stmt = node as Statement

            //skip conditional statements
            if (stmt.isBlockStmt() || stmt.isExpressionStmt() || stmt.isLabeledStmt() || stmt.isReturnStmt() || stmt.isSynchronizedStmt()) {
                node.getChildNodes().forEach {child -> visitStatement(child, calls)}
            }
            return
        }
        if (node instanceof MethodCallExpr) {
            def methodCall = node as MethodCallExpr
            if (!methodCall.scope.present || methodCall.scope.get().isThisExpr()) {
                calls.add(methodCall)
            }
            return
        }
        if (node instanceof VariableDeclarator || node instanceof Expression) {
            node.getChildNodes().forEach {child -> visitStatement(child, calls)}
        }
    }

    @Override
    protected TestType getType() {
        return DelegateTypes.METHOD
    }

    protected static TestGeneratorResult addTest(TestGeneratorResult result, MethodDeclaration method, String testCode) {
        def testMethod = new DependableNode<>()
        testMethod.node = JavaParser.parseBodyDeclaration(testCode).asMethodDeclaration()
        testMethod.dependency.imports.addAll(method.static ? [Imports.JUNIT_TEST] : [Imports.JUNIT_TEST, Imports.OPEN_POJO_VALIDAION_HELPER, Imports.OPEN_POJO_POJO_CLASS_FACTORY,
                                                                                     Imports.JUNIT_EQUALS, Imports.MOCKITO_SPY, Imports.MOCKITO_DO_NOTHING, Imports.MOCKITO_VERIFY,
                                                                                     Imports.MOCKITO_ANY, Imports.MOCKITO_DO_RETURN, Imports.MOCKITO_AT_LEAST])
        result.tests << testMethod
        result
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected String getMethodVerifyParams(MethodCallExpr callExpr, Map<String, String> params) {
        return callExpr.arguments.stream().map { a ->
            return a.isLiteralExpr() ? a.asLiteralExpr().toString() : params.get(a.asNameExpr().name)
        }.collect().join(", ")
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected String getMethodCallParams(MethodCallExpr callExpr) {
        return callExpr.arguments.stream().map { "any()" }.collect().join(", ")
    }

    protected Map<String, String> getMethodParamsCode(MethodDeclaration method) {
        return method.parameters.collectEntries {
            p -> [p.name, generateTestValue(p)]
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
