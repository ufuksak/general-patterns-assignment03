package com;

import java.util.List;
import java.util.Objects;

import org.jetbrains.jps.model.java.JavaSourceRootType;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;

public class EditorPopupAction extends BaseGenerateAction {

    //todo: handle missing test source folder
    @Override
    public void actionPerformed(final AnActionEvent e) {
        PsiElement element = e.getData(LangDataKeys.PSI_ELEMENT);
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


        VirtualFile sourceFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        assert sourceFile != null;
        Module module = ProjectFileIndex.SERVICE.getInstance(Objects.requireNonNull(e.getProject())).getModuleForFile(sourceFile);
        assert module != null;


        PsiManager psiManager = PsiManager.getInstance(e.getProject());
        List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.TEST_SOURCE);
        if (sourceRoots.isEmpty()) {
            return;
        }

        VirtualFile testFile = sourceRoots.iterator().next();
        final PsiDirectory dir = psiManager.findDirectory(testFile);
        if (dir == null) {
            return;
        }

        doGenerationInBackground(e.getProject(), sourceFile, testFile, method);
    }

    @Override
    public void update(AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        final VirtualFile sourceRoot = ProjectFileIndex.SERVICE.getInstance(Objects.requireNonNull(e.getProject())).getSourceRootForFile(file);
        Module module = ProjectFileIndex.SERVICE.getInstance(Objects.requireNonNull(e.getProject())).getModuleForFile(file);
        if (module == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        List<VirtualFile> sources = ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.SOURCE);
        boolean visible = "java".equalsIgnoreCase(file.getExtension()) && !sources.isEmpty() && sources.get(0).equals(sourceRoot);
        e.getPresentation().setEnabledAndVisible(visible);
    }
    
}
