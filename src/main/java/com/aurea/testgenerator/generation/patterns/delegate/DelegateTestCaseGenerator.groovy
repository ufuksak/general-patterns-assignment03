package com.aurea.testgenerator.generation.patterns.delegate;

class DelegateTestCaseGenerator {
    def counter = 0

    String generateAssert() {
        "verify(${callingObj}, atLeast(1)).${call.name.toString()}(${this.getMethodVerifyParams(call, params)});\n"
    }
}
