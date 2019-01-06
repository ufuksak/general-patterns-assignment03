package com.aurea.testgenerator.generation.patterns.delegate

import com.aurea.testgenerator.source.Unit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.Type

class DelegateTestCaseGenerator {

    private static final String EXPECTED_TRUE = "expectedtrue"
    private static final String EXPECTED_FALSE = "expectedfalse"
    private static final String EXPECTED_NULL = "expectednull"
    private static final String TRUE = "true"
    private static final String FALSE = "false"
    private static final String NULL = "null"
    private static final String EXPECTED = "expected"
    private static final String ACTUAL = "actual"
    private static final String CLASS_PROPERTY = "class_property_"
    private static final String CLASS_INSTANCE = "classInstance"
    private static final String MOCK_STR = "mock"
    private static final String DO_NOTHING = "doNothing"
    private static final String VERIFY = "verify"
    private static final String DO_RETURN = "verify"
    private final DelegateTestGenerator testGenerator
    private final MethodDeclaration method
    private final DelegateMethodCallVisitor visitor
    private List<String> variableDeclaredList
    private String lines = ""

    DelegateTestCaseGenerator(final DelegateTestGenerator testGenerator, MethodDeclaration method,
                              DelegateMethodCallVisitor visitor) {
        this.testGenerator = testGenerator
        this.method = method
        this.visitor = visitor
        variableDeclaredList = []
        lines = ""
    }

    String generateArrange(Unit unit) {

        lines += getMockStringByMethodParams()

        lines += getMockStringByMethodType()

        lines += getMockStringByUnitFields()

        lines += getFieldsAndInstances(unit)

        lines += getMockStringAndDoReturnByVariables()

        lines += getDoNothingAndDoReturn()

        return lines
    }

    String getFieldsAndInstances(Unit unit) {
        String lines = """${unit.className} ${CLASS_INSTANCE} = """
        if (visitor.selfFound) {
            lines += """ mock(${unit.className}.class);"""
        } else {
            lines += """ new ${unit.className}();"""
        }

        visitor.unitFieldMap.each { field ->
            lines += """Field field_${field.key} = ${unit.className}.class.getDeclaredField("${field.key}");"""
            lines += """field_${field.key}.setAccessible(true);"""
            lines += """field_${field.key}.set(${CLASS_INSTANCE}, ${CLASS_PROPERTY}${field.key});"""
        }

        return lines
    }

    String getMockStringByMethodParams() {
        String lines = ""
        visitor.methodParamMap.each { param ->
            String className = ""
            String mockString = ""

            if (param.value.isPrimitiveType()) {
                className = """${param.value.asPrimitiveType().asString()}"""
                mockString = """${visitor.getRandomStringByPrimitiveType(className)}"""

            } else if (param.value.isClassOrInterfaceType()) {
                className = """${param.value.asClassOrInterfaceType().name}"""
                if (param.value.asClassOrInterfaceType().name.asString().equals("String")) {
                    mockString = """ \"Vice Versa\" """
                } else {
                    mockString = """ ${MOCK_STR}(${className}.class)"""
                }
            }
            lines += """${className} ${param.key} = ${mockString};"""
        }
        return lines
    }

    private String getMockStringByMethodType() {
        String lines = ""
        Type type = method.getType()
        if (!method.getType().isVoidType()) {
            String className = ""
            String mockString = ""

            if (type.isPrimitiveType()) {
                className = """${type.asPrimitiveType().asString()}"""
                mockString = """${visitor.getRandomStringByPrimitiveType(type.asPrimitiveType().toString())}"""

            } else if (type.isClassOrInterfaceType()) {
                className = """${type.asClassOrInterfaceType().name}"""
                if (type.asClassOrInterfaceType().name.asString() == "String") {
                    mockString = """ \"Vice Versa\" """
                } else {
                    mockString = """ ${MOCK_STR}(${className}.class)"""
                }
            }
            lines += """${className} ${EXPECTED} = ${mockString};"""
        }
        return lines
    }

    private String getMockStringByUnitFields() {
        String lines = ""
        visitor.unitFieldMap.each { field ->
            String className = ""
            String mockString = ""

            if (field.value.isPrimitiveType()) {
                className = """${field.value.asPrimitiveType().asString()}"""
                if (field.value != null) {
                    mockString = """${
                        visitor.getRandomStringByPrimitiveType(field.value.asPrimitiveType().toString())
                    }"""
                } else {
                    mockString = """${NULL}"""
                }

            } else if (field.value.isClassOrInterfaceType()) {
                className = """${field.value.asClassOrInterfaceType().name}"""
                if (field.value.asClassOrInterfaceType().name.asString().equals("String")) {
                    mockString = """ \"Vice Versa\" """
                } else {
                    mockString = """ ${MOCK_STR}(${className}.class)"""
                }
            }
            lines += """${className} ${CLASS_PROPERTY}${field.key} = ${mockString};"""
        }
        return lines
    }

    private String getMockStringAndDoReturnByVariables() {
        String lines = ""
        visitor.variableDeclarationList.each { variableDeclarationExpr ->
            variableDeclarationExpr.asVariableDeclarationExpr().variables.each { variable ->
                String className = ""
                String mockOrInstance = ""

                if (variable.type.isPrimitiveType()) {
                    className = """${variable.type.asPrimitiveType().asString()}"""
                    mockOrInstance = """${
                        visitor.getRandomStringByPrimitiveType(variable.type.asPrimitiveType().toString())
                    }"""

                } else if (variable.type.isClassOrInterfaceType()) {
                    className = """${variable.type.asClassOrInterfaceType().name}"""
                    if (variable.type.asClassOrInterfaceType().name.asString().equals("String")) {
                        mockOrInstance = """ \"Vice Versa\" """
                    } else {
                        mockOrInstance = """ ${MOCK_STR}(${className}.class)"""
                    }
                }
                lines += """${className} ${EXPECTED}${variable.name} = ${mockOrInstance};"""
                variableDeclaredList << """${EXPECTED}${variable.name}"""

                if (variable.initializer.present) {
                    if (variable.initializer.get().isMethodCallExpr()) {
                        MethodCallExpr methodCall = variable.initializer.get().asMethodCallExpr()
                        lines += """doReturn(${EXPECTED}${variable.name})"""

                        lines += """.when(${findObjectCall(methodCall)})"""

                        String argStr = getArgsByMethodCall(methodCall, DO_RETURN)
                        lines += """.${methodCall.name}(${argStr});"""
                    }
                }
            }
        }
        lines = lines.replace(EXPECTED_NULL, NULL).replace(EXPECTED_TRUE, TRUE)
                .replace(EXPECTED_FALSE, FALSE)
        return lines
    }

    private String getDoNothingAndDoReturn() {
        String lines = ""
        visitor.methodCallList.each { methodCall ->
            lines += """doNothing().when(${findObjectCall(methodCall)})"""

            String argStr = getArgsByMethodCall(methodCall, DO_NOTHING)
            lines += """.${methodCall.name}(${argStr});"""
        }

        visitor.returnStmtList.each { returnStmt ->
            if (returnStmt != null &&
                    returnStmt.expression.present) {
                def expression = returnStmt.expression.get()
                if (expression.isMethodCallExpr()) {
                    MethodCallExpr methodCall = expression.asMethodCallExpr()
                    lines += """doReturn(${EXPECTED})"""

                    lines += """.when(${findObjectCall(methodCall)})"""

                    String argStr = getArgsByMethodCall(methodCall, DO_RETURN)
                    lines += """.${methodCall.name}(${argStr});"""
                }
            }
        }
        lines = lines.replace(EXPECTED_NULL, NULL).replace(EXPECTED_TRUE, TRUE)
                .replace(EXPECTED_FALSE, FALSE)
        return lines
    }

    static boolean isNameExprOrFieldAccessExpr(MethodCallExpr callExpr) {
        if (callExpr.scope.present) {
            if (callExpr.scope.get().isNameExpr()
                    || callExpr.scope.get().isFieldAccessExpr()) {
                return true
            }
        }
        return false
    }

    String findMockObject(String param) {
        String argStr = ""
        visitor.unitFieldMap.each { field ->
            if (field.key.toString() == param
                    && field.value instanceof ClassOrInterfaceType) {
                argStr = """mock(${field.value}.class)"""
            }
        }
        return argStr
    }

    String getArgsByMethodCall(MethodCallExpr methodCall, String behaviour) {
        String argStr = ""
        boolean flag = false
        methodCall.arguments.each { arg ->
            if (flag) {
                argStr += ","
            }
            flag = true
            String mockString = ""
            if (arg.isNameExpr()) {
                mockString = findMockObject(arg.asNameExpr().name.toString())
            } else if (arg.isClassExpr()) {
                mockString = """mock(${arg.asClassExpr().toString()}.class)"""
            }

            if (arg.isNameExpr() && visitor.methodParamMap.containsKey(arg.asNameExpr().name.toString())) {
                argStr += """${arg.asNameExpr().name.toString()}"""
            } else if (arg.isFieldAccessExpr()) {
                argStr += """${CLASS_PROPERTY}${arg.asFieldAccessExpr().name.toString()}"""
            } else if (mockString != "") {
                argStr += mockString
            } else {
                argStr += """${EXPECTED}${arg.toString()}"""
            }

        }
        return argStr
    }

    String findObjectCall(MethodCallExpr callExpr) {
        if (isNameExprOrFieldAccessExpr(callExpr)) {
            def scope = callExpr.scope.get()
            if (scope.isNameExpr()) {
                if (visitor.methodParamMap.containsKey(scope.asNameExpr().name.toString())) {
                    scope.asNameExpr().name
                } else {
                    if (variableDeclaredList.contains("""${EXPECTED}${scope.asNameExpr().name}""")) {
                        """${EXPECTED}${scope.asNameExpr().name}"""
                    } else if (callExpr instanceof MethodCallExpr
                            && !lines.contains(scope.asNameExpr().name.toString() + " =")) {
                        """mock(${scope.asNameExpr().name}.class)"""
                    } else {
                        """${CLASS_PROPERTY}${scope.asNameExpr().name}"""
                    }
                }
            } else {
                """${CLASS_PROPERTY}${scope.asFieldAccessExpr().name}"""
            }
        } else {
            CLASS_INSTANCE
        }
    }

    String generateAct() {
        String retValue = ""

        def type = method.type
        if (!type.voidType) {
            String className = ""
            if (type.isPrimitiveType()) {
                className = """${type.asPrimitiveType().asString()}"""
            } else if (type.isClassOrInterfaceType()) {
                className = """${type.asClassOrInterfaceType().name}"""
            }
            retValue += """${className} ${ACTUAL} = """
        }

        boolean flag = false
        String args = ""
        method.parameters.each { param ->
            if (flag) {
                args += ","
            }
            args += """${param.name}"""
            flag = true
        }
        retValue += """classInstance.${method.name}(${args});"""

        return retValue
    }

    String generateAssert(DelegateMethodCallVisitor visitor) {
        String lines = ""
        def type = method.type
        if (!type.voidType) {
            String expectedName = ""

            visitor.returnStmtList.each { returnStmt ->
                if (returnStmt != null &&
                        returnStmt.expression.present) {
                    def expression = returnStmt.expression.get()
                    def name = ""
                    if (expression.isNameExpr()) {
                        name = expression.asNameExpr().name
                    } else if (expression.isFieldAccessExpr()) {
                        name = expression.asFieldAccessExpr().name
                    }
                    if (visitor.methodParamMap.containsKey(name.toString())) {
                        expectedName = """${name.toString()}"""
                    } else if (variableDeclaredList.contains("""${EXPECTED}${name.toString()}""")) {
                        expectedName = """${EXPECTED}${name.toString()}"""
                    } else {
                        expectedName = """${EXPECTED}"""
                    }
                }
            }

            lines += """assertEquals(${expectedName},${ACTUAL});"""
        }
        return lines
    }

    String generateVerify() {
        String lines = ""

        visitor.variableDeclarationList.each { varList ->
            varList.asVariableDeclarationExpr().variables.each { var ->
                if (var.initializer.present) {
                    if (var.initializer.get().isMethodCallExpr()) {
                        MethodCallExpr methodCall = var.initializer.get().asMethodCallExpr()

                        lines += """verify(${findObjectCall(methodCall)},atLeast(1))"""
                        String argStr = getArgsByMethodCall(methodCall, VERIFY)
                        lines += """.${methodCall.name}(${argStr});"""
                    }
                }
            }
        }

        visitor.methodCallList.each { methodCall ->
            lines += """verify(${findObjectCall(methodCall)},atLeast(1))"""

            String argStr = getArgsByMethodCall(methodCall, VERIFY)
            lines += """.${methodCall.name}(${argStr});"""
        }

        visitor.returnStmtList.each { returnStmt ->
            if (returnStmt != null &&
                    returnStmt.expression.present) {
                def expression = returnStmt.expression.get()
                if (expression.isMethodCallExpr()) {
                    MethodCallExpr methodCall = expression.asMethodCallExpr()

                    lines += """verify(${findObjectCall(methodCall)},atLeast(1))"""
                    String argStr = getArgsByMethodCall(methodCall, VERIFY)
                    lines += """.${methodCall.name}(${argStr});"""
                }
            }
        }
        lines = lines.replace(EXPECTED_NULL, NULL).replace(EXPECTED_TRUE, TRUE)
                .replace(EXPECTED_FALSE, FALSE)
        return lines
    }

}
