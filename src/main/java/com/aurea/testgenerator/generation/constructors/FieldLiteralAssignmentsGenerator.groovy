package com.aurea.testgenerator.generation.constructors

import com.aurea.testgenerator.ast.FieldAssignments
import com.aurea.testgenerator.ast.InvocationBuilder
import com.aurea.testgenerator.generation.PatternToTest
import com.aurea.testgenerator.generation.TestNodeExpression
import com.aurea.testgenerator.generation.TestUnit
import com.aurea.testgenerator.generation.source.AssertionBuilder
import com.aurea.testgenerator.generation.source.Imports
import com.aurea.testgenerator.pattern.PatternMatch
import com.aurea.testgenerator.pattern.general.constructors.ConstructorPatterns
import com.aurea.testgenerator.value.ValueFactory
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import groovy.util.logging.Log4j2
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Log4j2
class FieldLiteralAssignmentsGenerator implements PatternToTest {

    FieldAssignments fieldAssignments
    JavaParserFacade solver
    ValueFactory valueFactory

    @Autowired
    FieldLiteralAssignmentsGenerator(FieldAssignments fieldAssignments, JavaParserFacade solver, ValueFactory valueFactory) {
        this.fieldAssignments = fieldAssignments
        this.solver = solver
        this.valueFactory = valueFactory
    }

    @Override
    void accept(PatternMatch patternMatch, TestUnit testUnit) {
        if (patternMatch.pattern != ConstructorPatterns.HAS_FIELD_LITERAL_ASSIGNMENTS) {
            return
        }

        ConstructorDeclaration cd = patternMatch.match.asConstructorDeclaration()
        String instanceName = cd.nameAsString.uncapitalize()
        Expression scope = new NameExpr(instanceName)

        List<AssignExpr> assignExprs = cd.body.findAll(AssignExpr)
        Collection<AssignExpr> onlyLastAssignExprs = fieldAssignments.findLastAssignExpressionsByField(assignExprs)
        Collection<AssignExpr> onlyLiteralAssignExprs = onlyLastAssignExprs.findAll { it.value.literalExpr }
        AssertionBuilder builder = AssertionBuilder.buildFor(testUnit).softly(onlyLiteralAssignExprs.size() > 1)
        for (AssignExpr assignExpr : onlyLiteralAssignExprs) {
            Expression expected = assignExpr.value
            FieldAccessExpr fieldAccessExpr = assignExpr.target.asFieldAccessExpr()
            Optional<ResolvedFieldDeclaration> maybeField = fieldAccessExpr.findField(solver)
            Optional<Expression> maybeFieldAccessExpression = fieldAssignments.buildFieldAccessExpression(assignExpr, scope)
            maybeFieldAccessExpression.ifPresent { fieldAccessExpression ->
                builder.with(maybeField.get().getType(), fieldAccessExpression, expected)
            }
        }
        List<Statement> assertions = builder.build()
        if (!assertions.empty) {
            Optional<TestNodeExpression> constructorCall = new InvocationBuilder(valueFactory).build(cd)
            constructorCall.ifPresent { constructCallExpr ->
                String assignsConstantsCode = """
            @Test
            public void test_${cd.nameAsString}_AssignsConstantsToFields() throws Exception {
                ${cd.nameAsString} $instanceName = ${constructCallExpr.expr};
                
                ${assertions.join(System.lineSeparator())}
            }
            """
                MethodDeclaration assignsConstants = JavaParser.parseBodyDeclaration(assignsConstantsCode)
                                                               .asMethodDeclaration()
                testUnit.addImport Imports.JUNIT_TEST
                testUnit.addTest assignsConstants
            }
        }
    }
}