package com.aurea.testgenerator.generation.patterns.delegate

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement

class DelegateMethodCallVisitor extends BaseVisitor {

    List<MethodCallExpr> calls = new ArrayList<>()
    ReturnStmt returnStatement

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
}
