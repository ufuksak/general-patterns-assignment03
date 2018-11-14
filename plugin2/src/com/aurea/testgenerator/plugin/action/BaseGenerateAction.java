package com.aurea.testgenerator.plugin.action;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.util.ClassUtils;

import com.aurea.testgenerator.plugin.messages.PluginBundle;
import com.aurea.testgenerator.plugin.util.CompositeClassLoader;
import com.aurea.testgenerator.Main;
import com.aurea.testgenerator.Pipeline;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;

abstract class BaseGenerateAction extends AnAction {

    private static final Logger log = Logger.getInstance(ProjectStructureAction.class);

    void doGenerationInBackground(AnActionEvent event, String methodBody) {
        VirtualFile sourceFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
        assert sourceFile != null;

        Project project = event.getProject();
        Module module = ProjectFileIndex.SERVICE.getInstance(Objects.requireNonNull(project)).getModuleForFile(sourceFile);
        assert module != null;


        PsiManager psiManager = PsiManager.getInstance(project);
        List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.TEST_SOURCE);
        if (sourceRoots.isEmpty()) {
            return;
        }

        VirtualFile testFile = sourceRoots.iterator().next();
        final PsiDirectory dir = psiManager.findDirectory(testFile);
        if (dir == null) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, PluginBundle.message("key.generating.tests"), false) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                performGeneration(project, testFile, sourceFile, methodBody);
            }
        });
    }

    private void performGeneration(Project project, VirtualFile testFile, VirtualFile sourceFile, String methodBody) {
        try {
            AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
            ClassLoader defaultLoader = ctx.getClassLoader();
            CompositeClassLoader classLoaderToUse = new CompositeClassLoader(defaultLoader, Main.class.getClassLoader());
            ClassUtils.overrideThreadContextClassLoader(classLoaderToUse);
            ctx.setClassLoader(classLoaderToUse);

            ctx.getEnvironment().getPropertySources().addFirst(new SimpleCommandLinePropertySource("--project.src=" + sourceFile.getPath(),
                    "--project.out=" + testFile.getPath(),
                    "--project.fileNameResolution=MERGE",
                    "--spring.profiles.active=open-pojo,manual,delegates",
                    methodBody != null ? ("--project.methodBody=" + methodBody) : ""));
            ctx.scan("com.aurea.testgenerator");
            ctx.refresh();

            ctx.getBean(Pipeline.class).start();

            if (testFile.isDirectory()) {
                testFile.getChildren();
            }
            if (testFile instanceof NewVirtualFile) {
                ((NewVirtualFile) testFile).markClean();
                ((NewVirtualFile) testFile).markDirtyRecursively();
            }

            RefreshQueue.getInstance().refresh(true, true, () -> postRefresh(project, testFile), testFile);
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    private void postRefresh(Project project, VirtualFile file) {
        VcsDirtyScopeManager dirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
        if (file.isDirectory()) {
            dirtyScopeManager.dirDirtyRecursively(file);
        } else {
            dirtyScopeManager.fileDirty(file);
        }
    }

    @Override
    public void update(final AnActionEvent e) {
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
