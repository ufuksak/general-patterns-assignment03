package com.aurea.testgenerator.generation.patterns.singleton

import com.aurea.testgenerator.config.ProjectConfiguration
import com.aurea.testgenerator.generation.AbstractMethodTestGenerator
import com.aurea.testgenerator.generation.TestGeneratorResult
import com.aurea.testgenerator.generation.TestType
import com.aurea.testgenerator.generation.names.NomenclatureFactory
import com.aurea.testgenerator.reporting.CoverageReporter
import com.aurea.testgenerator.reporting.TestGeneratorResultReporter
import com.aurea.testgenerator.source.Unit
import com.github.javaparser.ast.body.InitializerDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

import static SingletonFunctions.tryFindUniquelyReturnedFieldValue
import static SingletonFunctions.tryGetAsJPField
import static com.aurea.testgenerator.generation.patterns.singleton.SingletonFunctions.hasSingletonSignature
import static com.aurea.testgenerator.generation.patterns.singleton.SingletonFunctions.isValueInitializedInStaticBlockByCreatingObjectOfType

@Component
@Profile("manual")
@Log4j2
class StaticBlockSingletonTestGenerator extends AbstractMethodTestGenerator {

    SingletonCommonTestGenerator singletonCommonTestGenerator

    @Autowired
    StaticBlockSingletonTestGenerator(JavaParserFacade solver,
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
        super.shouldBeVisited(unit, method) && hasSingletonSignature(method) && initializedInStaticBlock(method)
    }

    private boolean initializedInStaticBlock(MethodDeclaration method) {
        tryFindUniquelyReturnedFieldValue(method, solver).filter {
            isValueInitializedInStaticBlockByCreatingObjectOfType(it, method.type)
        }.present
    }
}
