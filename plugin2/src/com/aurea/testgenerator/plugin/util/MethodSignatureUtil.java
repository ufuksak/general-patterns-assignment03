package com.aurea.testgenerator.plugin.util;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

public class MethodSignatureUtil {

    public static String getSignature(PsiMethod method) {
        StringBuilder result = new StringBuilder(method.getName() + "(");
        JvmParameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            result.append(((PsiType) parameters[i].getType()).getPresentableText()).append(" ").append(parameters[i].getName());
            if (i < parameters.length - 1) {
                result.append(", ");
            }
        }
        result.append(")");
        return result.toString();
    }
}
