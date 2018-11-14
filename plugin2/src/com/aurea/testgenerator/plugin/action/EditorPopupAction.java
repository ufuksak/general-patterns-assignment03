package com.aurea.testgenerator.plugin.action;

import com.aurea.testgenerator.plugin.messages.PluginBundle;
import com.aurea.testgenerator.plugin.util.MethodSignatureUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

public class EditorPopupAction extends BaseGenerateAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        PsiElement element = event.getData(LangDataKeys.PSI_ELEMENT);
        String method = null;
        if (element instanceof PsiMethod) {
            method = MethodSignatureUtil.getSignature((PsiMethod) element);
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
