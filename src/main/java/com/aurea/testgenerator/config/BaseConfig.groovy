package com.aurea.testgenerator.config

import com.aurea.testgenerator.coverage.CoverageService
import com.aurea.testgenerator.coverage.JacocoCoverageRepository
import com.aurea.testgenerator.coverage.JacocoCoverageService
import com.aurea.testgenerator.coverage.NoCoverageService
import com.aurea.testgenerator.source.SourceFilter
import com.aurea.testgenerator.source.SourceFilters
import com.github.javaparser.JavaParser
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

import javax.annotation.PostConstruct

@Configuration
@EnableAspectJAutoProxy
class BaseConfig {

    @PostConstruct
    void disableCommentNodes() {
        JavaParser.getStaticConfiguration().setStoreTokens(false)
        JavaParser.getStaticConfiguration().setAttributeComments(false)
    }

    @Bean
    StopWatchAspect stopWatchAspect() {
        new StopWatchAspect()
    }

    @Bean
    SourceFilter sourceFilter() {
        SourceFilters.empty()
    }

    @Bean
    TypeSolver combinedTypeSolver(ProjectConfiguration projectConfiguration) {
        def solver = new CombinedTypeSolver(new ReflectionTypeSolver())

        def file = new File(projectConfiguration.src)
        if (file.isDirectory()) {
            solver.add(new JavaParserTypeSolver(file))
        }

        projectConfiguration.resolvePaths
                .collect { new File(it) }
                .findAll { it.exists() && it.isDirectory() }
                .each { solver.add(new JavaParserTypeSolver(it)) }

        projectConfiguration.resolveJars.stream()
                .map { new File(it) }
                .filter { it.exists() }
                .peek { addIndividualJarFileSolver(solver, it) }
                .filter { it.isDirectory() }
                .each {
                    it.traverse {
                        addIndividualJarFileSolver(solver, it)
                    }
                }

        solver
    }

    static void addIndividualJarFileSolver(CombinedTypeSolver solver, File file) {
        if (file.isFile() && file.name.toLowerCase().endsWith('.jar')) {
            solver.add(new JarTypeSolver(file.path))
        }
    }

    @Bean
    JavaParserFacade javaParserFacade(TypeSolver solver) {
        JavaParserFacade.get(solver)
    }

    @Bean
    CoverageService coverageService(ProjectConfiguration projectConfiguration) {
        if (projectConfiguration.jacoco) {
            return new JacocoCoverageService(JacocoCoverageRepository.fromFile(projectConfiguration.jacocoPath))
        } else {
            return new NoCoverageService()
        }
    }
}
