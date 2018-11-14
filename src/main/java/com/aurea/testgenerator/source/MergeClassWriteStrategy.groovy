package com.aurea.testgenerator.source

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component

import java.nio.charset.Charset

@Component
class MergeClassWriteStrategy implements ExistingTestClassWriteStrategy {

    @Override
    void write(final File existingTest, final Unit testUnit) {
        CompilationUnit existingCU = JavaParser.parse(existingTest, Charset.defaultCharset())
        existingCU.imports.addAll(testUnit.cu.imports)
        existingCU.setImports(new NodeList<>(new HashSet<>(existingCU.imports)))

        testUnit.cu.types.forEach {
            newType -> mergeType(existingCU, newType)
        }

        existingCU.module.ifPresent { module -> println module.toString() }

        existingTest.delete()
        existingTest.write(existingCU.toString())
    }

    def static mergeType(CompilationUnit existingCU, TypeDeclaration newType) {
        def existingType = existingCU.types.find { StringUtils.equals(it.name.identifier, newType.name.identifier) }
        if (existingType != null) {
            newType.members.forEach {
                member -> mergeMember(existingType, member)
            }
        } else {
            existingCU.types.add(newType)
        }
    }

    def static mergeMember(final TypeDeclaration existingType, final BodyDeclaration newMethod) {
        def existingMethod = existingType.members.find { StringUtils.equals(it.name.identifier, newMethod.name.identifier) }
        if (existingMethod == null) {
            existingType.members.add(newMethod)
        }
    }

    @Override
    FileNameConflictResolutionStrategyType getType() {
        FileNameConflictResolutionStrategyType.MERGE
    }
}
