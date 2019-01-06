package com.aurea.testgenerator.generation.patterns.delegate

import com.aurea.testgenerator.source.Unit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.Type

class DelegateMethodCallVisitor extends BaseVisitor {

    List<MethodCallExpr> calls = new ArrayList<>()
    ReturnStmt returnStatement
    List<ReturnStmt> returnStmtList
    List<MethodCallExpr> methodCallList
    List<VariableDeclarationExpr> variableDeclarationList
    private final MethodDeclaration method
    boolean selfFound = false
    private boolean methodDelegationFound = false
    Map<String, Type> methodParamMap
    Map<String, Type> unitFieldMap

    DelegateMethodCallVisitor(Unit unit, MethodDeclaration method) {
        returnStmtList = []
        methodCallList = []
        variableDeclarationList = []
        this.method = method
        methodParamMap = processMethodParams()
        unitFieldMap = processUnitFields(unit)
        initialize()
    }

    @Override
    boolean process(Node node) {
        if (node instanceof Statement) {
            def stmt = node as Statement

            //skip conditional statements
            if (stmt.isBlockStmt() || stmt.isExpressionStmt() || stmt.isLabeledStmt() || stmt.isReturnStmt() || stmt.isSynchronizedStmt()) {
                if (stmt.isReturnStmt() && stmt.asReturnStmt().expression.present) {
                    returnStatement = stmt.asReturnStmt()
                }
                return true
            }
        } else if (node instanceof MethodCallExpr) {
            def methodCall = node as MethodCallExpr
            if (!methodCall.scope.present || methodCall.scope.get().isThisExpr() || methodCall.scope.get().isNameExpr() || methodCall.scope.get().isFieldAccessExpr()) {
                calls.add(methodCall)
            }
        } else if (node instanceof VariableDeclarator || node instanceof Expression) {
            return true
        }
        return false
    }

    void collectCalls(List<Node> childNodes) {
        if (childNodes != null &&
                !childNodes.isEmpty()) {
            childNodes.each { node ->
                if (node instanceof VariableDeclarationExpr) {
                    VariableDeclarationExpr variableDeclarationExpr = node as VariableDeclarationExpr
                    processVariableList(variableDeclarationExpr)
                } else if (node instanceof MethodCallExpr) {
                    MethodCallExpr callExpr = node as MethodCallExpr
                    processMethodCallList(callExpr)
                } else if (node instanceof Statement) {
                    Statement statement = node as Statement
                    processStatements(statement)
                }
            }
        }
    }

    private Map<String, Type> processUnitFields(Unit unit) {
        Map<String, Type> map = [:]
        unit.cu.types.findAll().each { type ->
            if (type.isClassOrInterfaceDeclaration()) {
                type.members.each { mems ->
                    if (mems.isFieldDeclaration()) {
                        def field = mems.asFieldDeclaration()
                        field.variables.each { var ->
                            map.put(var.name, var.type)
                        }
                    }
                }
            }
        }
        return map
    }

    private Map<String, Type> processMethodParams() {
        return method.parameters.collectEntries() {
            p ->
                [p.name.asString(),
                 p.type]
        }
    }

    private final void initialize() {
        if (method.body.present) {
            collectCalls(method.body.get().childNodes)
        }
    }

    boolean isMethodDelegationFound() {
        return methodDelegationFound
    }

    private void processVariableList(VariableDeclarationExpr node) {
        variableDeclarationList << node.asVariableDeclarationExpr()
        VariableDeclarationExpr vd = node as VariableDeclarationExpr
        vd.variables.each { var ->
            if (var.initializer.present) {
                if (var.initializer.get().isMethodCallExpr()) {
                    MethodCallExpr methodCall = var.initializer.get().asMethodCallExpr()
                    if (!selfFound) {
                        if (methodCall.scope.present) {
                            if (methodCall.scope.get().isThisExpr()) {
                                selfFound = true
                            }
                        } else {
                            selfFound = true
                        }
                    }
                }
            }
        }
    }

    private void processMethodCallList(MethodCallExpr callExpr) {
        if (!callExpr.scope.present
                || callExpr.scope.get().isThisExpr()
                || callExpr.scope.get().isNameExpr()
                || callExpr.scope.get().isFieldAccessExpr()) {
            methodCallList << callExpr
            methodDelegationFound = true
        }

        checkSelfMethodExist(callExpr)
    }

    private void processStatements(Statement statement) {
        if (statement.isBlockStmt() || statement.isReturnStmt() || statement.isExpressionStmt()
                || statement.isIfStmt()) {
            if (statement.isReturnStmt() && statement.asReturnStmt().expression.present) {
                ReturnStmt returnStmt = statement.asReturnStmt()
                if (returnStmt != null && returnStmt.expression.present) {
                    returnStmtList << returnStmt
                    if (returnStmt.expression.get().isMethodCallExpr()) {
                        methodDelegationFound = true
                        checkSelfMethodExist(returnStmt.expression.get().asMethodCallExpr())
                    } else if (returnStmt.expression.get().isObjectCreationExpr()) {
                        return
                    }
                }
                if (methodCallList.empty) {
                    return
                }

                return
            }
            collectCalls(statement.getChildNodes())
        }
    }

    private void checkSelfMethodExist(MethodCallExpr callExpr) {
        if (!selfFound) {
            if (callExpr.scope.present) {
                if (callExpr.scope.get().isThisExpr()) {
                    selfFound = true
                }
            } else {
                selfFound = true
            }
        }
    }

    double getRandomDoubleBetween1and100() {
        Math.random() * 100 + 1
    }

    String getRandomStringByPrimitiveType(String type) {
        switch (type) {
            case "int":
                int random = 40
                return random.toString()
            case "double":
                double random = (double) getRandomDoubleBetween1and100()
                return random.toString()
            case "float":
                float random = (float) getRandomDoubleBetween1and100()
                return random.toString()
            case "long":
                long random = 50
                return random.toString()
            case "short":
                short random = (short) getRandomDoubleBetween1and100()
                return random.toString()
            case "boolean":
                boolean random = (Math.random() * 1) == 0
                return random.toString()
            case "char":
                char random = (char) getRandomDoubleBetween1and100()
                return random.toString()
            default:
                return ""
        }
    }

}
