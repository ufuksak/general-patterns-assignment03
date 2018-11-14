package com.aurea.testgenerator.plugin.action;

import com.aurea.testgenerator.plugin.messages.PluginBundle;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

public class EditorPopupAction extends BaseGenerateAction {

    //todo: handle missing test source folder
    @Override
    public void actionPerformed(AnActionEvent event) {
        PsiElement element = event.getData(LangDataKeys.PSI_ELEMENT);
        String method = null;
        if (element instanceof PsiMethod) {
            method = ((PsiMethod) element).getName() + "(";
            JvmParameter[] parameters = ((PsiMethod) element).getParameters();
            for (int i = 0; i < parameters.length; i++) {
                method += ((PsiType) parameters[i].getType()).getPresentableText() + " " + parameters[i].getName();
                if (i < parameters.length - 1) {
                    method += ", ";
                }
            }
            method += ")";
        }

        doGenerationInBackground(event, method);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);

        PsiElement element = e.getData(LangDataKeys.PSI_ELEMENT);
        e.getPresentation().setText(element instanceof PsiMethod ?
                PluginBundle.message("key.generate.tests.for.method", ((PsiMethod) element).getName()) :
                PluginBundle.message("key.generate.tests.for.class"));
    }

}
