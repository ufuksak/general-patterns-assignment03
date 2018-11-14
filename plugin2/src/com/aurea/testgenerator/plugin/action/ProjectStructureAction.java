package com.aurea.testgenerator.plugin.action;

import com.aurea.testgenerator.plugin.messages.PluginBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class ProjectStructureAction extends BaseGenerateAction {

    public ProjectStructureAction() {
        getTemplatePresentation().setText(PluginBundle.message("key.generate.tests"));
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        doGenerationInBackground(event, null);
    }

}

