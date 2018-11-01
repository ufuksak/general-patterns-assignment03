package com.aurea.testgenerator.generation.patterns.delegate

import com.aurea.testgenerator.source.Unit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration
import com.github.javaparser.resolution.types.ResolvedType
import org.apache.commons.lang3.NotImplementedException

class DelegateTestCaseGenerator {

    def counter = 0
    def callingObj
    private final DelegateTestGenerator testGenerator
    private final MethodDeclaration method
    private final Map<String, String> params
    private final Map<MethodCallExpr, String> expected = [:]

    DelegateTestCaseGenerator(final DelegateTestGenerator testGenerator, MethodDeclaration method) {
        this.testGenerator = testGenerator
        this.method = method
        this.params = getMethodParams(method)
    }

    String generateArrange(MethodCallExpr call, Unit unit) {
        ResolvedFieldDeclaration fieldDecl = null
        ResolvedParameterDeclaration paramDecl = null
        if (call.scope.present && (call.scope.get().isNameExpr() || call.scope.get().isFieldAccessExpr())) {
            def solvedScope = testGenerator.solve(call.scope.get())
            if (solvedScope.solved) {
                if (solvedScope.correspondingDeclaration.isField()) {
                    fieldDecl = solvedScope.correspondingDeclaration.asField()
                } else if (solvedScope.correspondingDeclaration.isParameter()) {
                    paramDecl = solvedScope.correspondingDeclaration.asParameter()
                } else {
                    throw new NotImplementedException('Cannot handle: ' + call.scope)
                }
            }
        }

        def solved = testGenerator.solve(call)
        if (!solved.solved) {
            throw new IllegalStateException('Cannot handle: ' + call)
        }

        def returnType = solved.correspondingDeclaration.returnType
        def mockBlock = ""
        if (returnType.isVoid()) {
            mockBlock += "doNothing()"
        } else {
            def objName = returnType.isPrimitive() ? returnType.asPrimitive().describe() : returnType.asReferenceType().typeDeclaration.name
            mockBlock += "${objName} expected_${++counter} = ${testGenerator.factory.getExpression(returnType).get()};\n"
            mockBlock += "doReturn(expected_${counter})"
            expected.put(call, "expected_${counter}")
        }

        def mockMethodCall = ".${call.name.toString()}(${getMethodCallMockParams(call)});\n"
        if (fieldDecl != null) {
            callingObj = "delegate_${fieldDecl.name}"
            mockBlock += ".when(${callingObj})${mockMethodCall}"
            if (fieldDecl.getType().isReferenceType()) {
                def typeName = getTypeReference(fieldDecl.getType(), unit)
                mockBlock = "${typeName} delegate_${fieldDecl.name} = mock(${typeName}.class);\n" + mockBlock + "\nReflectionTestUtils.setField(classInstance, \"${fieldDecl.name}\", delegate_${fieldDecl.name});"
            } else {
                mockBlock = mockBlock + "\nReflectionTestUtils.setField(classInstance, \"${fieldDecl.name}\", delegate_${fieldDecl.name});"
            }
        } else if (paramDecl != null) {
            callingObj = "delegate_${paramDecl.name}"
            mockBlock += ".when(${callingObj})${mockMethodCall}"
            def typeName = getTypeReference(paramDecl.getType(), unit)
            mockBlock = "${typeName}  delegate_${paramDecl.name} = mock(${typeName}.class);\n" + mockBlock
            params.put(paramDecl.name, "delegate_${paramDecl.name}")
        } else {
            callingObj = "classInstance"
            mockBlock += ".when(${callingObj})${mockMethodCall}"
        }
        mockBlock
    }

    String generateAct(DelegateMethodCallVisitor visitor) {
        def parentMethodParams = params.collect { p -> p.value }.join(", ")
        def actBlock
        if (method.type.isVoidType()) {
            actBlock = "classInstance.${method.name}(${parentMethodParams});"
        } else {
            if (visitor.returnStatement == null) {
                throw new NotImplementedException("Cannot handle this case")
            }

            def solved = testGenerator.solve(visitor.returnStatement.expression.get())
            def expectedVar = "expected_${counter}"
            if (solved.correspondingDeclaration.isVariable()) {
                def varInit = solved.correspondingDeclaration.wrappedNode.initializer.get()
                if (visitor.calls.contains(varInit)) {
                    expectedVar = expected.get(varInit)
                } else if (varInit instanceof LiteralExpr) {
                    expectedVar = ((LiteralExpr) varInit).toString()
                }
            }

            actBlock = "${method.type.toString()} actual = classInstance.${method.name}(${parentMethodParams});\n" +
                    "assertEquals(${expectedVar}, actual);"
        }
        actBlock
    }

    String generateAssert(MethodCallExpr call) {
        "verify(${callingObj}, atLeast(1)).${call.name.toString()}(${getMethodVerifyParams(call, params)});\n"
    }

    protected static String getTypeReference(ResolvedType resolvedType, Unit unit) {
        def typeDeclaration = resolvedType.asReferenceType().typeDeclaration
        if (typeDeclaration.qualifiedName == unit.packageName + "." + typeDeclaration.name) {
            return typeDeclaration.name
        }
        return typeDeclaration.qualifiedName
    }

    protected String getMethodVerifyParams(MethodCallExpr callExpr, Map<String, String> params) {
        return callExpr.arguments.stream().map { a ->
            if (a.isLiteralExpr()) {
                return a.asLiteralExpr().toString()
            } else if (a.isBinaryExpr()) {
                return "any()"
            }
            def declaration = testGenerator.solve(a).correspondingDeclaration
            if (declaration.isField()) {
                return testGenerator.mockFactory.getExpression(declaration.getType())
            }
            def type = testGenerator.solve(a).correspondingDeclaration.getType()
            return params.getOrDefault(a.asNameExpr().name.asString(), testGenerator.mockFactory.getExpression(type))
        }.collect().join(", ")
    }

    protected String getMethodCallMockParams(MethodCallExpr callExpr) {
        return callExpr.arguments.stream().map {
            arg ->
                if (arg.isBinaryExpr()) {
                    return "any()"
                } else if (arg.isLiteralExpr()) {
                    return arg.asLiteralExpr().toString()
                } else {
                    def type = testGenerator.solve(arg).correspondingDeclaration.getType()
                    return testGenerator.mockFactory.getExpression(type)
                }
        }.collect().join(", ")
    }

    protected Map<String, String> getMethodParams(MethodDeclaration method) {
        return method.parameters.collectEntries {
            p -> [p.name.asString(), testGenerator.factory.getExpression(p.type.resolve()).get()]
        }
    }
}
