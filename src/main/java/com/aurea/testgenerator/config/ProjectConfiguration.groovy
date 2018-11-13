package com.aurea.testgenerator.config

import com.aurea.testgenerator.source.FileNameConflictResolutionStrategyType
import groovy.transform.Canonical
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated

import javax.validation.constraints.NotNull
import java.nio.file.Path
import java.nio.file.Paths

@Configuration
@ConfigurationProperties(prefix = "project")
@EnableConfigurationProperties
@Canonical
@Validated
class ProjectConfiguration {
    boolean blank

    @NotNull
    String src

    List<String> resolvePaths = []
    List<String> resolveJars = []

    String testSrc
    String out
    String jacoco
    String methodPrefix
    String methodBody

    FileNameConflictResolutionStrategyType fileNameResolution = FileNameConflictResolutionStrategyType.OVERRIDE

    Path getSrcPath() {
        Paths.get(src)
    }

    Path getJacocoPath() {
        Paths.get(jacoco)
    }

    Path getTestSrcPath() {
        Paths.get(testSrc)
    }

    Path getOutPath() {
        Paths.get(out)
    }

    String getMethodPrefix() {
        Objects.toString(methodPrefix, "")
    }

    boolean isBlank() {
        return blank
    }
}
