package com.aurea.testgenerator.generation.patterns.delegate

import com.aurea.testgenerator.ast.Callability
import com.aurea.testgenerator.config.ProjectConfiguration
import com.aurea.testgenerator.generation.MethodLevelTestGeneratorWithClassContext
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
import com.github.javaparser.ParseProblemException
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component



@Component
@Profile("delegates")
@Log4j2
class DelegateTestGenerator extends MethodLevelTestGeneratorWithClassContext<MethodDeclaration> {

    ValueFactory factory
    MockValueFactory mockFactory
    def methodCounter = [:]
    private static final List<ImportDeclaration> ALL_IMPORTS = [
            Imports.JUNIT_TEST,
            Imports.JUNIT_EQUALS,
            Imports.REFLECT_FIELD,
            Imports.MOCKITO_MATCHERS_ANY_BOOLEAN,
            Imports.MOCKITO_MATCHERS_ANY,
            Imports.MOCKITO_MATCHERS_ANY_FLOAT,
            Imports.MOCKITO_MATCHERS_ANY_DOUBLE,
            Imports.MOCKITO_MATCHERS_ANY_INT,
            Imports.MOCKITO_MATCHERS_ANY_LONG,
            Imports.MOCKITO_MATCHERS_ANY_STRING,
            Imports.MOCKITO_ATLEAST,
            Imports.MOCKITO_DO_NOTHING,
            Imports.MOCKITO_MOCK,
            Imports.MOCKITO_SPY,
            Imports.MOCKITO_DO_RETURN,
            Imports.MOCKITO_VERIFY
    ]

    @Autowired
    DelegateTestGenerator(JavaParserFacade solver, TestGeneratorResultReporter reporter,
                          CoverageReporter visitReporter, NomenclatureFactory nomenclatures,
                          ValueFactory factory, MockValueFactory mockFactory,
                          ProjectConfiguration projectConfiguration) {
        super(solver, reporter, visitReporter, nomenclatures, projectConfiguration)
        this.factory = factory
        this.mockFactory = mockFactory
    }

    @Override
    protected TestGeneratorResult generate(MethodDeclaration method, Unit unit) {
        def result = new TestGeneratorResult()

        if (!method.body.present) {
            return result
        }

        def visitor = new DelegateMethodCallVisitor(unit, method)
        if (!visitor.isMethodDelegationFound()) {
            return result
        }
        visitor.visit(method.body.get())

        def testCase = new DelegateTestCaseGenerator(this, method, visitor)

        if (visitor.calls.isEmpty()) {
            return result
        }

        def arrangeBlock = testCase.generateArrange(unit)
        def verifyBlock = testCase.generateVerify()

        def assertBlock = testCase.generateAssert(visitor)

        return addTest(result, method , """@Test
            public void test${this.generateMethodName(method, unit)}() throws Exception {
                // arrange
                ${arrangeBlock}   

                // act                    
                ${testCase.generateAct()}

                // assert
                ${assertBlock}    
                ${verifyBlock}
            }""")
    }

    @Override
    protected TestType getType() {
        return DelegateTypes.METHOD
    }

    protected TestGeneratorResult addTest(TestGeneratorResult result, MethodDeclaration methodUnderTest, String testCode) {
        DependableNode<MethodDeclaration> testMethod = new DependableNode<>()

        def configuration = JavaParser.getStaticConfiguration()
        JavaParser.setStaticConfiguration(new ParserConfiguration())
        StringBuilder importBuilder = new StringBuilder()
        try {
            testMethod.node = JavaParser.parseBodyDeclaration(testCode).asMethodDeclaration()
            testMethod.dependency.imports.addAll(ALL_IMPORTS)
            if(methodUnderTest.name.toString() == "read") {
                System.out.print("sdds")
            }
            methodUnderTest.parameters.each {
                def parameterName = it.nameAsString
                Optional<DependableNode<VariableDeclarationExpr>> variableDeclaration = factory.getVariable(parameterName, it.type)
                if (variableDeclaration.isPresent()) {
                    importBuilder.append(variableDeclaration.get().node)
                    importBuilder.append(";")
                    importBuilder.append(System.lineSeparator())
                    testMethod.dependency.imports.addAll(variableDeclaration.get().dependency.imports)
                } else {
                    importBuilder.append("${it.type.asString()} ${parameterName} = null;")
                }

            }

            result.tests << testMethod
        } catch (ParseProblemException e) {
            log.trace("class is not parsable:{}",e)
        }
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

    @Override
    protected void visitClass(ClassOrInterfaceDeclaration classDeclaration, List<TestGeneratorResult> results) {

    }

    @Override
    boolean shouldBeVisited(Unit unit, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        !classOrInterfaceDeclaration.interface &&
                !classOrInterfaceDeclaration.abstract &&
                !classOrInterfaceDeclaration.localClassDeclaration &&
                !classOrInterfaceDeclaration.nestedType &&
                Callability.isInstantiable(classOrInterfaceDeclaration)
    }

    @Override
    protected boolean shouldBeVisited(Unit unit, MethodDeclaration method) {
        !method.private &&
                !method.protected &&
                !method.static &&
                !method.abstract &&
                Callability.isCallableFromTests(method)
    }

}
