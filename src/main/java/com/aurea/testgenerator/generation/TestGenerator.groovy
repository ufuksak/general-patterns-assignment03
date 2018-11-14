package com.aurea.testgenerator.generation

import com.aurea.testgenerator.source.Unit


interface TestGenerator {
    Collection<TestGeneratorResult> generate(Unit unit)

    String requestTestClassName(Unit unit)
}