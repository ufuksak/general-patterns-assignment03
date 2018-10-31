package com.aurea.testgenerator.generation.patterns.delegate

import com.aurea.testgenerator.source.Unit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration
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
        this.params = testGenerator.getMethodParamsCode(method)
    }

    String generateArrange(MethodCallExpr call, Unit unit) {
        ResolvedFieldDeclaration fieldDecl = null
        ResolvedParameterDeclaration paramDecl = null
        if (call.scope.present && (call.scope.get().isNameExpr() || call.scope.get().isFieldAccessExpr())) {
            def solvedScope = testGenerator.solver.solve(call.scope.get())
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

        def solved = testGenerator.solver.solve(call)
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
            expected.put(call, "expected_${counter}");
        }

        def mockMethodCall = ".${call.name.toString()}(${testGenerator.getMethodCallMockParams(call)});\n"
        if (fieldDecl != null) {
            callingObj = "delegate_${fieldDecl.name}"
            mockBlock += ".when(${callingObj})${mockMethodCall}"
            if (fieldDecl.getType().isReferenceType()) {
                def typeName = testGenerator.getTypeReference(fieldDecl.getType(), unit)
                mockBlock = "${typeName} delegate_${fieldDecl.name} = mock(${typeName}.class);\n" + mockBlock + "\nReflectionTestUtils.setField(classInstance, \"${fieldDecl.name}\", delegate_${fieldDecl.name});"
            } else {
                mockBlock = mockBlock + "\nReflectionTestUtils.setField(classInstance, \"${fieldDecl.name}\", delegate_${fieldDecl.name});"
            }
        } else if (paramDecl != null) {
            callingObj = "delegate_${paramDecl.name}"
            mockBlock += ".when(${callingObj})${mockMethodCall}"
            def typeName = testGenerator.getTypeReference(paramDecl.getType(), unit)
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
                throw new NotImplementedException("Cannot handle this case");
            }

            def solved = testGenerator.solver.solve(visitor.returnStatement.expression.get())
            def expectedVar = "expected_${counter}"
            if (solved.correspondingDeclaration.isVariable()) {
                def varInit = solved.correspondingDeclaration.wrappedNode.initializer.get();
                if (visitor.calls.contains(varInit)) {
                    expectedVar = expected.get(varInit)
                } else {
                    throw new NotImplementedException("")
                }
            }

            actBlock = "${method.type.toString()} actual = classInstance.${method.name}(${parentMethodParams});\n" +
                    "assertEquals(${expectedVar}, actual);"
        }
        actBlock
    }

    String generateAssert(MethodCallExpr call) {
        "verify(${callingObj}, atLeast(1)).${call.name.toString()}(${testGenerator.getMethodVerifyParams(call, params)});\n"
    }
}
