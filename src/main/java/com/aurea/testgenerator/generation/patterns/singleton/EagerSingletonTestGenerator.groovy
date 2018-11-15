package com.aurea.testgenerator.generation.patterns.singleton

import com.aurea.testgenerator.config.ProjectConfiguration
import com.aurea.testgenerator.generation.AbstractMethodTestGenerator
import com.aurea.testgenerator.generation.TestGeneratorResult
import com.aurea.testgenerator.generation.TestType
import com.aurea.testgenerator.generation.names.NomenclatureFactory
import com.aurea.testgenerator.reporting.CoverageReporter
import com.aurea.testgenerator.reporting.TestGeneratorResultReporter
import com.aurea.testgenerator.source.Unit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

import static SingletonFunctions.tryFindUniquelyReturnedFieldValue
import static com.aurea.testgenerator.generation.patterns.singleton.SingletonFunctions.hasSingletonSignature
import static com.aurea.testgenerator.generation.patterns.singleton.SingletonFunctions.isValueInitializedByCreatingObjectOfType

@Component
@Profile("manual")
@Log4j2
class EagerSingletonTestGenerator extends AbstractMethodTestGenerator {

    SingletonCommonTestGenerator singletonCommonTestGenerator

    @Autowired
    EagerSingletonTestGenerator(JavaParserFacade solver,
                                TestGeneratorResultReporter reporter,
                                CoverageReporter visitReporter,
                                NomenclatureFactory nomenclatures,
                                SingletonCommonTestGenerator singletonCommonTestGenerator,
                                ProjectConfiguration projectConfiguration) {
        super(solver, reporter, visitReporter, nomenclatures, projectConfiguration)
        this.singletonCommonTestGenerator = singletonCommonTestGenerator
    }

    @Override
    protected TestGeneratorResult generate(MethodDeclaration method, Unit unitUnderTest) {
        singletonCommonTestGenerator.addSameInstanceTest(
                method,
                unitUnderTest,
                new TestGeneratorResult())
    }

    @Override
    protected TestType getType() {
        SingletonTypes.SAME_INSTANCE
    }

    @Override
    protected boolean shouldBeVisited(Unit unit, MethodDeclaration method) {
        super.shouldBeVisited(unit, method) && hasSingletonSignature(method) && isEager(method)
    }

    private boolean isEager(MethodDeclaration method) {
        tryFindUniquelyReturnedFieldValue(method, solver).filter {
            isValueInitializedByCreatingObjectOfType(it, method.type)
        }.present
    }
}
