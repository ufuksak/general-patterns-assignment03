package com.aurea.testgenerator.plugin.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.utils.actions.MavenActionUtil;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;

public class AddDependenciesAction extends AnAction {

    private static final Logger log = Logger.getInstance(AddDependenciesAction.class);
    private final static List<MavenId> TEST_DEPENDENCIES = new ArrayList<>();

    static {
        TEST_DEPENDENCIES.add(new MavenId("com.openpojo", "openpojo", "0.8.7"));
        TEST_DEPENDENCIES.add(new MavenId("org.ow2.asm", "asm", "5.2"));
        TEST_DEPENDENCIES.add(new MavenId("org.assertj", "assertj-core", "2.9.0"));
        TEST_DEPENDENCIES.add(new MavenId("nl.jqno.equalsverifier", "equalsverifier", "2.4.3"));
        TEST_DEPENDENCIES.add(new MavenId("com.aurea", "tests-commons", "1.8.1"));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        assert file != null;
        assert e.getProject() != null;

        try {
            MavenDomProjectModel mavenDomProjectModel = MavenDomUtil.getMavenDomProjectModel(e.getProject(), file);
            assert mavenDomProjectModel != null;

            WriteCommandAction.runWriteCommandAction(e.getProject(), () -> {
                TEST_DEPENDENCIES.forEach(newDependency -> {

                    Optional result = mavenDomProjectModel.getDependencies().getDependencies().stream().filter(d ->
                            StringUtils.equals(d.getGroupId().getValue(), newDependency.getGroupId()) &&
                                    StringUtils.equals(d.getArtifactId().getValue(), newDependency.getArtifactId())
                    ).findAny();
                    if (!result.isPresent()) {
                        MavenDomUtil.createDomDependency(mavenDomProjectModel, null, newDependency);
                    }
                });
            });
        } catch (Exception err) {
            log.error(err);
            err.printStackTrace();
        }
    }

    @Override
    public void update(AnActionEvent e) {
        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        boolean available = MavenActionUtil.hasProject(e.getDataContext()) && MavenActionUtil.isMavenProjectFile(file);
        e.getPresentation().setEnabledAndVisible(available);
    }
}


