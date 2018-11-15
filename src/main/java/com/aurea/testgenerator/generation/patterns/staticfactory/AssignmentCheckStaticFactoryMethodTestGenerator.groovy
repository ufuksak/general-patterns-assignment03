package com.aurea.testgenerator.generation.patterns.staticfactory

import com.aurea.common.CognitiveComplexityCalculator
import com.aurea.testgenerator.ast.Callability
import com.aurea.testgenerator.ast.InvocationBuilder
import com.aurea.testgenerator.config.ProjectConfiguration
import com.aurea.testgenerator.generation.AbstractMethodTestGenerator
import com.aurea.testgenerator.generation.TestGeneratorError
import com.aurea.testgenerator.generation.TestGeneratorResult
import com.aurea.testgenerator.generation.TestType
import com.aurea.testgenerator.generation.assertions.SoftAssertions
import com.aurea.testgenerator.generation.assertions.StateChangeAssertionBuilder
import com.aurea.testgenerator.generation.ast.DependableNode
import com.aurea.testgenerator.generation.merge.TestNodeMerger
import com.aurea.testgenerator.generation.names.NomenclatureFactory
import com.aurea.testgenerator.generation.names.TestMethodNomenclature
import com.aurea.testgenerator.generation.patterns.pojos.Pojos
import com.aurea.testgenerator.generation.source.Imports
import com.aurea.testgenerator.reporting.CoverageReporter
import com.aurea.testgenerator.reporting.TestGeneratorResultReporter
import com.aurea.testgenerator.source.Unit
import com.aurea.testgenerator.value.ValueFactory
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.Type
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import groovy.util.logging.Log4j2
import one.util.streamex.StreamEx
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("manual")
@Log4j2
class AssignmentCheckStaticFactoryMethodTestGenerator extends AbstractMethodTestGenerator {

    ValueFactory valueFactory
    SoftAssertions softAssertions

    @Autowired
    AssignmentCheckStaticFactoryMethodTestGenerator(JavaParserFacade solver,
                                                    TestGeneratorResultReporter reporter,
                                                    CoverageReporter visitReporter,
                                                    NomenclatureFactory nomenclatures,
                                                    ValueFactory valueFactory,
                                                    SoftAssertions softAssertions,
                                                    ProjectConfiguration projectConfiguration) {
        super(solver, reporter, visitReporter, nomenclatures, projectConfiguration)
        this.valueFactory = valueFactory
        this.softAssertions = softAssertions
    }

    @Override
    protected TestGeneratorResult generate(MethodDeclaration method, Unit unitUnderTest) {
        TestGeneratorResult result = new TestGeneratorResult()

        try {
            Expression returnExpr = method.findAll(ReturnStmt).first().expression.get()
            NameExpr testInstanceName = new NameExpr("resultingInstance")
            StateChangeAssertionBuilder assertionBuilder = new StateChangeAssertionBuilder(
                    returnExpr,
                    testInstanceName,
                    method,
                    solver)

            TestMethodNomenclature testMethodNomenclature = nomenclatures.getTestMethodNomenclature(unitUnderTest.javaClass)
            String testName = testMethodNomenclature.requestTestMethodName(getType(), method)

            List<DependableNode<Statement>> assertionStatements = softAssertions.softly(unitUnderTest.javaClass, testName, assertionBuilder)
            if (assertionStatements) {
                DependableNode<MethodDeclaration> testMethod = new DependableNode<>()
                TestNodeMerger.appendDependencies(testMethod, assertionStatements)

                List<DependableNode<VariableDeclarationExpr>> variables = StreamEx.of(method.parameters).map { p ->
                    valueFactory.getVariable(p.nameAsString, p.type).orElseThrow {
                        new TestGeneratorError("Failed to build variable for parameter $p of $method")
                    }
                }.toList()
                List<Statement> variableStatements = variables.collect { new ExpressionStmt(it.node) }
                TestNodeMerger.appendDependencies(testMethod, variables)

                Map<String, DependableNode<Expression>> variableExpressionsByNames = StreamEx.of(variables)
                                                                                                 .toMap(
                        { it.node.getVariable(0).nameAsString },
                        { DependableNode.from(new NameExpr(it.node.getVariable(0).name)) })

                Optional<DependableNode<MethodCallExpr>> methodCall = new InvocationBuilder(valueFactory)
                        .usingForParameters(variableExpressionsByNames)
                        .buildMethodInvocation(method)

                methodCall.ifPresent { methodCallExpr ->
                    TestNodeMerger.appendDependencies(testMethod, methodCallExpr)
                    String testCode = """
                    @Test
                    public void ${testName}() throws Exception {
                        ${variableStatements.join(System.lineSeparator())}
        
                        ${method.type} $testInstanceName = ${methodCallExpr.node};
                        
                        ${assertionStatements.collect { it.node }.join(System.lineSeparator())}
                    }
                    """

                    testMethod.node = JavaParser.parseBodyDeclaration(testCode)
                                                .asMethodDeclaration()
                    testMethod.dependency.imports << Imports.JUNIT_TEST
                    result.tests = [testMethod]
                }

            }
        } catch (TestGeneratorError tge) {
            result.errors << tge
        }
        result
    }

    @Override
    protected TestType getType() {
        return StaticFactoryMethodTypes.ASSIGNMENT_CHECK
    }

    @Override
    protected boolean shouldBeVisited(Unit unit, MethodDeclaration method) {
        super.shouldBeVisited(unit, method) &&
                Callability.isCallableFromTests(method) &&
                method.static &&
                returnsCreatedClassOrInterface(method) &&
                isC0(method) &&
                hasOnlySetterCalls(method) &&
                hasOnlyOneReturn(method)
    }

    private static boolean returnsCreatedClassOrInterface(MethodDeclaration method) {
        Type returnType = method.type
        returnType?.classOrInterfaceType && method.findAll(ObjectCreationExpr) { it.type == returnType }
    }

    private static boolean isC0(MethodDeclaration method) {
        return !method.abstract &&
                method.body.present &&
                CognitiveComplexityCalculator.calculate(method) == 0
    }

    private static boolean hasOnlySetterCalls(MethodDeclaration method) {
        boolean hasNonSetterCalls = method.findAll(MethodCallExpr).any { it -> !Pojos.isSetterCall(it) }
        !hasNonSetterCalls
    }

    private static hasOnlyOneReturn(MethodDeclaration method) {
        method.findAll(ReturnStmt).findAll { it.expression.present }.size() == 1
    }
}
