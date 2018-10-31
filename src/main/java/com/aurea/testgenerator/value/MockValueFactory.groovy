package com.aurea.testgenerator.value

import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedType
import org.springframework.stereotype.Component

@Component
class MockValueFactory {

    String getExpression(ResolvedType type) {
        if (type.isPrimitive()) {
            def primitive = type.asPrimitive();
            if (primitive == ResolvedPrimitiveType.BOOLEAN) {
                return "anyBoolean()"
            } else if (primitive == ResolvedPrimitiveType.CHAR) {
                return "anyChar()"
            } else if (primitive == ResolvedPrimitiveType.BYTE) {
                return "anyByte()"
            } else if (primitive == ResolvedPrimitiveType.SHORT) {
                return "anyShort()"
            } else if (primitive == ResolvedPrimitiveType.INT) {
                return "anyInt()"
            } else if (primitive == ResolvedPrimitiveType.LONG) {
                return "anyLong()"
            } else if (primitive == ResolvedPrimitiveType.FLOAT) {
                return "anyFloat()"
            } else if (primitive == ResolvedPrimitiveType.DOUBLE) {
                return "anyDouble()"
            }
            throw new UnsupportedOperationException("Unknown primitive type: $type")
        }
        return "any()";
    }

}
