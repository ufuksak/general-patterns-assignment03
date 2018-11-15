package com.aurea.testgenerator.plugin.action;

import java.io.File;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.util.ClassUtils;

import com.aurea.testgenerator.Main;
import com.aurea.testgenerator.Pipeline;
import com.aurea.testgenerator.plugin.messages.PluginBundle;
import com.aurea.testgenerator.plugin.util.CompositeClassLoader;
import com.aurea.testgenerator.plugin.util.GPMarkTestSourceRootAction;
import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.xml.GenericDomValue;

abstract class BaseGenerateAction extends AnAction {

    private static final Logger log = Logger.getInstance(ProjectStructureAction.class);

    void doGenerationInBackground(AnActionEvent event, String methodBody) {
        VirtualFile sourceFile = event.getData(CommonDataKeys.VIRTUAL_FILE);
        assert sourceFile != null;

        Project project = event.getProject();
        Module module = ProjectFileIndex.SERVICE.getInstance(Objects.requireNonNull(project)).getModuleForFile(sourceFile);
        assert module != null;

        VirtualFile testRoot = getTestSourceRoot(event, project, module);
        if (testRoot == null) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, PluginBundle.message("key.generating.tests"), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                performGeneration(project, testRoot, sourceFile, methodBody);
            }
        });
    }

    private VirtualFile getTestSourceRoot(AnActionEvent event, Project project, Module module) {
        List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.TEST_SOURCE);
        if (!sourceRoots.isEmpty()) {
            return sourceRoots.iterator().next();
        }

        MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
        if (!projectsManager.isMavenizedModule(module)) {
            return null;
        }
        MavenProject mavenProject = projectsManager.findProject(module);
        assert mavenProject != null;

        VirtualFile pom = mavenProject.getFile();
        MavenDomProjectModel mavenDomProjectModel = MavenDomUtil.getMavenDomProjectModel(project, pom);
        assert mavenDomProjectModel != null;

        GenericDomValue<PathReference> testSourceDirectory = mavenDomProjectModel.getBuild().getTestSourceDirectory();
        String testFolderPath;
        if (testSourceDirectory.exists() && testSourceDirectory.getStringValue() != null) {
            testFolderPath = testSourceDirectory.getStringValue();
        } else{
            testFolderPath = project.getBaseDir().getPath() + "/src/test/java";
        }

        File testSourceDir = new File(testFolderPath);
        if (!testSourceDir.exists()) {
            testSourceDir.mkdirs();
        }

        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(testSourceDir);
        if (virtualFile == null) {
            return null;
        }

        new GPMarkTestSourceRootAction().modifyRoots(event, module, virtualFile);
        return virtualFile;
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

            refreshFileTree(testFile, project);
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    private void refreshFileTree(VirtualFile testFile, Project project) {
        if (testFile.isDirectory()) {
            testFile.getChildren();
        }
        if (testFile instanceof NewVirtualFile) {
            ((NewVirtualFile) testFile).markClean();
            ((NewVirtualFile) testFile).markDirtyRecursively();
        }

        RefreshQueue.getInstance().refresh(true, true, () -> {
            VcsDirtyScopeManager dirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
            if (testFile.isDirectory()) {
                dirtyScopeManager.dirDirtyRecursively(testFile);
            } else {
                dirtyScopeManager.fileDirty(testFile);
            }

            DumbService.getInstance(project).smartInvokeLater(() -> optimizeImports(testFile, project));
        }, testFile);
    }

    private void optimizeImports(VirtualFile file, Project project) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor<VirtualFile>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile child) {
                boolean directory = child.isDirectory();
                if (!directory) {
                    try {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(child);
                        if (psiFile != null) {
                            new OptimizeImportsProcessor(project, psiFile).run();
                        }
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
                return directory;
            }
        });
    }

    @Override
    public void update(AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        VirtualFile sourceRoot = ProjectFileIndex.SERVICE.getInstance(Objects.requireNonNull(e.getProject())).getSourceRootForFile(file);
        Module module = ProjectFileIndex.SERVICE.getInstance(Objects.requireNonNull(e.getProject())).getModuleForFile(file);
        if (module == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        List<VirtualFile> sources = ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.SOURCE);
        boolean fileCheck = file.isDirectory() || "java".equalsIgnoreCase(file.getExtension());
        boolean visible = fileCheck && !sources.isEmpty() && sources.get(0).equals(sourceRoot);
        e.getPresentation().setEnabledAndVisible(visible);
    }
}
