package com;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.util.ClassUtils;

import com.aurea.testgenerator.Main;
import com.aurea.testgenerator.Pipeline;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;

abstract class BaseGenerateAction extends AnAction {

    private static final Logger log = Logger.getInstance(TestAction.class);

    void doGenerationInBackground(Project project, VirtualFile sourceFile, VirtualFile testFile, String methodBody) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Tests", false) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                try {
                    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
                    ClassLoader defaultLoader = ctx.getClassLoader();
                    CompositeClassLoader classLoaderToUse = new CompositeClassLoader(defaultLoader, Main.class.getClassLoader());
                    ClassUtils.overrideThreadContextClassLoader(classLoaderToUse);
                    ctx.setClassLoader(classLoaderToUse);

                    ctx.getEnvironment().getPropertySources().addFirst(new SimpleCommandLinePropertySource("--project.src=" + sourceFile.getPath(),
                            "--project.out=" + testFile.getPath(),
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
        });
    }

    private void postRefresh(Project project, VirtualFile file) {
        VcsDirtyScopeManager dirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
        if (file.isDirectory()) {
            dirtyScopeManager.dirDirtyRecursively(file);
        } else {
            dirtyScopeManager.fileDirty(file);
        }
    }

}
