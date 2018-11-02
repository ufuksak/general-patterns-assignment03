package com.aurea.testgenerator.generation.patterns.delegate

import com.aurea.testgenerator.generation.AbstractMethodTestGenerator
import com.aurea.testgenerator.generation.TestGeneratorResult
import com.aurea.testgenerator.generation.TestType
import com.aurea.testgenerator.generation.ast.DependableNode
import com.aurea.testgenerator.generation.names.NomenclatureFactory
import com.aurea.testgenerator.generation.source.Imports
import com.aurea.testgenerator.reporting.CoverageReporter
import com.aurea.testgenerator.reporting.TestGeneratorResultReporter
import com.aurea.testgenerator.source.Unit
import com.aurea.testgenerator.value.MockValueFactory
import com.aurea.testgenerator.value.ValueFactory
import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("delegates")
@Log4j2
class DelegateTestGenerator extends AbstractMethodTestGenerator {

    ValueFactory factory
    MockValueFactory mockFactory
    def methodCounter = [:]

    @Autowired
    DelegateTestGenerator(JavaParserFacade solver, TestGeneratorResultReporter reporter,
                          CoverageReporter visitReporter, NomenclatureFactory nomenclatures,
                          ValueFactory factory, MockValueFactory mockFactory) {
        super(solver, reporter, visitReporter, nomenclatures)
        this.factory = factory
        this.mockFactory = mockFactory
    }

    @Override
    protected TestGeneratorResult generate(MethodDeclaration method, Unit unit) {
        def result = new TestGeneratorResult()

        if (!method.body.present) {
            return result
        }

        def visitor = new DelegateMethodCallVisitor()
        visitor.visit(method.body.get())
        if (visitor.calls.isEmpty()) {
            return result
        }

        def instanceCode = """${unit.className} classInstance = spy(${unit.className}.class);"""
        def callsMock = ""
        def verifyBlock = ""
        def testCase = new DelegateTestCaseGenerator(this, method)
        visitor.calls.forEach { call ->
            callsMock += testCase.generateArrange(call, unit)
            verifyBlock += testCase.generateAssert(call)
        }

        return addTest(result, method, """@Test
            public void test${this.generateMethodName(method, unit)}() {
                // arrange
                ${instanceCode}    
                ${callsMock}   

                // act                    
                ${testCase.generateAct(visitor)}

                // assert    
                ${verifyBlock}
            }""")
    }

    @Override
    protected TestType getType() {
        return DelegateTypes.METHOD
    }

    protected static TestGeneratorResult addTest(TestGeneratorResult result, MethodDeclaration method, String testCode) {
        def testMethod = new DependableNode<>()

        def configuration = JavaParser.getStaticConfiguration()
        JavaParser.setStaticConfiguration(new ParserConfiguration())

        testMethod.node = JavaParser.parseBodyDeclaration(testCode).asMethodDeclaration()
        testMethod.dependency.imports.addAll(method.static ? [Imports.JUNIT_TEST] : [Imports.JUNIT_TEST, Imports.JUNIT_EQUALS, Imports.MOCKITO_ANY, Imports.REFLECTION_UTILS])

        result.tests << testMethod
        JavaParser.setStaticConfiguration(configuration)
        result
    }

    protected String generateMethodName(MethodDeclaration method, Unit unit) {
        def capitalized = method.getNameAsString().capitalize()
        def key = unit.fullName + "#" + capitalized
        def counter = methodCounter.getOrDefault(key, 0)
        methodCounter.put(key, counter + 1)
        return counter == 0 ? capitalized : (capitalized + "_" + (counter + 1))
    }

    protected SymbolReference<? extends ResolvedValueDeclaration> solve(Expression expression) {
        solver.solve(expression)
    }
}
