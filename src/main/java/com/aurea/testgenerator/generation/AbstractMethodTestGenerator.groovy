package com.aurea.testgenerator.generation

import com.aurea.testgenerator.config.ProjectConfiguration
import com.aurea.testgenerator.generation.names.NomenclatureFactory
import com.aurea.testgenerator.reporting.CoverageReporter
import com.aurea.testgenerator.reporting.TestGeneratorResultReporter
import com.aurea.testgenerator.source.Unit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import org.springframework.stereotype.Component

@Component
abstract class AbstractMethodTestGenerator extends MethodLevelTestGenerator<MethodDeclaration> {

    AbstractMethodTestGenerator(JavaParserFacade solver, TestGeneratorResultReporter reporter,
                                CoverageReporter visitReporter, NomenclatureFactory nomenclatures,
                                ProjectConfiguration projectConfiguration) {
        super(solver, reporter, visitReporter, nomenclatures, projectConfiguration)
    }

    protected VoidVisitorAdapter<JavaParserFacade> createVisitor(Unit unit, List<TestGeneratorResult> results) {
        new VoidVisitorAdapter<JavaParserFacade>() {
            @Override
            void visit(MethodDeclaration methodDeclaration, JavaParserFacade javaParserFacade) {
                visit(methodDeclaration, unit, results)
            }
        }
    }
}
