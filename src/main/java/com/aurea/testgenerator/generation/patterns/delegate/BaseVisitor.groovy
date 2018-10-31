package com.aurea.testgenerator.generation.patterns.delegate

import com.github.javaparser.ast.Node

abstract class BaseVisitor {

    abstract boolean process(final Node node)

    void visit(Node node) {
        if (!process(node)) {
            return
        }

        node.getChildNodes().forEach { child -> visit(child) }
    }
}
