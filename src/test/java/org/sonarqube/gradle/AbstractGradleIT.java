/**
 * SonarQube Gradle Plugin
 * Copyright (C) 2015-2016 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarqube.gradle;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractGradleIT {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  protected String getGradleVersion() {
    return System.getProperty("gradle.version", "3.1");
  }

  protected Properties runGradlewSonarQubeSimulationMode(String project) throws Exception {
    return runGradlewSonarQubeSimulationModeWithEnv(project, Collections.emptyMap());
  }

  protected Properties runGradlewSonarQubeSimulationModeWithEnv(String project, Map<String, String> env) throws Exception {
    File projectBaseDir = new File(this.getClass().getResource(project).toURI());
    File tempProjectDir = temp.newFolder(projectBaseDir.getName());
    FileUtils.copyDirectory(projectBaseDir, tempProjectDir);
    File out = temp.newFile();

    BuildResult result = GradleRunner.create()
            .withProjectDir(tempProjectDir)
            .withArguments("--stacktrace", "sonarqube", "-DsonarRunner.dumpToFile=" + out.getAbsolutePath())
            .withPluginClasspath()
            .build();
    assertThat(result.task(":sonarqube").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

    Properties props = new Properties();
    try (FileReader fr = new FileReader(out)) {
      props.load(fr);
    }
    return props;
  }

  protected BuildResult runGradlewSonarQubeWithEnv(String project, Map<String, String> env) throws Exception {
    File projectBaseDir = new File(this.getClass().getResource(project).toURI());
    File tempProjectDir = temp.newFolder(projectBaseDir.getName());
    File outputFile = temp.newFile();
    FileUtils.copyDirectory(projectBaseDir, tempProjectDir);

    return GradleRunner.create()
            .withProjectDir(tempProjectDir)
            .withArguments("--stacktrace", "sonarqube")
            .withPluginClasspath()
            .build();
  }

}
