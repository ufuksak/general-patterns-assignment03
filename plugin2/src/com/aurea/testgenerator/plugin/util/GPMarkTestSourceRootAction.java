package com.aurea.testgenerator.plugin.util;

import com.intellij.ide.projectView.actions.MarkTestSourceRootAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;

@SuppressWarnings("ComponentNotRegistered")
public class GPMarkTestSourceRootAction extends MarkTestSourceRootAction {

    public void modifyRoots(AnActionEvent e, Module module, VirtualFile file) {
        modifyRoots(e, module, new VirtualFile[]{file});
    }

}
