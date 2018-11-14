package com.aurea.testgenerator.generation.names

import com.aurea.common.JavaClass
import com.aurea.testgenerator.config.ProjectConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.util.concurrent.ConcurrentHashMap


@Component
class NomenclatureFactory {

    Map<JavaClass, TestMethodNomenclature> testMethodNomenclatures = new ConcurrentHashMap<>()
    Map<JavaClass, TestClassNomenclature> testClassNomenclatures = new ConcurrentHashMap<>()
    Map<JavaClass, Map<String, TestVariableNomenclature>> testVariablesNomenclatures = new ConcurrentHashMap<>()

    final ProjectConfiguration projectConfiguration

    @Autowired
    NomenclatureFactory(ProjectConfiguration projectConfiguration) {
        this.projectConfiguration = projectConfiguration
    }

    TestMethodNomenclature getTestMethodNomenclature(JavaClass javaClass) {
        testMethodNomenclatures.computeIfAbsent(javaClass, { new TestMethodNomenclature(projectConfiguration) })
    }

    TestVariableNomenclature getVariableNomenclature(JavaClass javaClass, String methodName) {
        Map<String, TestVariableNomenclature> variablesPerMethod = testVariablesNomenclatures.computeIfAbsent(javaClass, {
            new HashMap<>()
        })
        variablesPerMethod.computeIfAbsent(methodName, { new StandardVariableNomenclature() })
    }

}
