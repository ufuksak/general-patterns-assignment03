package com.aurea.testgenerator.generation

import com.aurea.testgenerator.config.ProjectConfiguration
import com.aurea.testgenerator.generation.names.NomenclatureFactory
import com.aurea.testgenerator.reporting.CoverageReporter
import com.aurea.testgenerator.reporting.TestGeneratorResultReporter
import com.aurea.testgenerator.source.Unit
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade

abstract class MethodLevelTestGeneratorWithClassContext<T extends CallableDeclaration> extends
        MethodLevelTestGenerator<T> {

    MethodLevelTestGeneratorWithClassContext(JavaParserFacade solver, TestGeneratorResultReporter reporter, CoverageReporter coverageReporter,
                                             NomenclatureFactory nomenclatures, ProjectConfiguration projectConfiguration) {
        super(solver, reporter, coverageReporter, nomenclatures, projectConfiguration)
    }

    @Override
    protected VoidVisitorAdapter<JavaParserFacade> createVisitor(Unit unit, List<TestGeneratorResult> results) {
        new VoidVisitorAdapter<JavaParserFacade>() {
            @Override
            void visit(MethodDeclaration methodDeclaration, JavaParserFacade javaParserFacade) {
                visit(methodDeclaration, unit, results)
            }

            @Override
            void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, JavaParserFacade javaParserFacade) {
                if (!shouldBeVisited(unit, classOrInterfaceDeclaration)) {
                    return
                }
                super.visit(classOrInterfaceDeclaration, javaParserFacade)
                visitClass(classOrInterfaceDeclaration, results)
            }
        }
    }

    protected abstract void visitClass(ClassOrInterfaceDeclaration classDeclaration, List<TestGeneratorResult> results)

    abstract boolean shouldBeVisited(Unit unit, ClassOrInterfaceDeclaration classOrInterfaceDeclaration)
}
