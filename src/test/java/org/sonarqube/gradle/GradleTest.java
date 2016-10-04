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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assume.assumeTrue;

public class GradleTest extends AbstractGradleIT {

  @Test
  public void testSimpleJavaProject() throws Exception {
    Properties props = runGradlewSonarQubeSimulationMode("/java-gradle-simple");

    Path baseDir = Paths.get(props.getProperty("sonar.projectBaseDir"));

    assertThat(props).contains(entry("sonar.projectKey", "org.codehaus.sonar:example-java-gradle"));
    assertThat(Paths.get(props.getProperty("sonar.sources"))).isEqualTo(baseDir.resolve("src/main/java"));
    assertThat(Paths.get(props.getProperty("sonar.tests"))).isEqualTo(baseDir.resolve("src/test/java"));
    assertThat(Paths.get(props.getProperty("sonar.java.binaries"))).isEqualTo(baseDir.resolve("build/classes/main"));
    assertThat(Paths.get(props.getProperty("sonar.java.test.binaries"))).isEqualTo(baseDir.resolve("build/classes/test"));
    assertThat(props.getProperty("sonar.java.libraries")).contains("commons-io-2.5.jar");
    assertThat(props.getProperty("sonar.java.libraries")).doesNotContain("junit-4.10.jar");
    assertThat(props.getProperty("sonar.java.test.libraries")).contains("junit-4.10.jar");
    assertThat(props.getProperty("sonar.java.test.libraries")).contains("commons-io-2.5.jar");
  }

  @Test
  public void testSkip() throws Exception {
    Map<String, String> env = new HashMap<>();
    env.put("SONARQUBE_SCANNER_PARAMS", "{\"sonar.scanner.skip\" : \"true\" }");
    BuildResult result = runGradlewSonarQubeWithEnv("/java-gradle-simple", env);

    assertThat(result.task(":sonarqube").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.getOutput()).contains("SonarQube Scanner analysis skipped");
  }

  @Test
  public void testCompileOnly() throws Exception {
    // compileOnly introduced with Gradle 2.12
    String gradleVersion = getGradleVersion();
    assumeTrue(
      gradleVersion.startsWith("2.12") || gradleVersion.startsWith("2.13") || gradleVersion.startsWith("2.14") || gradleVersion.startsWith("3.") || gradleVersion.startsWith("4."));

    Properties props = runGradlewSonarQubeSimulationMode("/java-compile-only");

    assertThat(props.getProperty("sonar.java.libraries")).contains("commons-io-2.5.jar");
    assertThat(props.getProperty("sonar.java.libraries")).contains("commons-lang-2.6.jar");
    assertThat(props.getProperty("sonar.java.libraries")).doesNotContain("junit-4.10.jar");
    assertThat(props.getProperty("sonar.java.test.libraries")).contains("junit-4.10.jar");
    assertThat(props.getProperty("sonar.java.test.libraries")).contains("commons-io-2.5.jar");
    // Seems like compileOnly are not included into test classpath
    assertThat(props.getProperty("sonar.java.test.libraries")).doesNotContain("commons-lang-2.6.jar");
  }

  @Test
  public void mixJavaGroovyProject() throws Exception {
    Properties props = runGradlewSonarQubeSimulationMode("/java-groovy-tests-gradle");

    Path baseDir = Paths.get(props.getProperty("sonar.projectBaseDir"));

    assertThat(props).contains(entry("sonar.projectKey", "groovy-java-gradle-mixed-tests"));
    assertThat(Paths.get(props.getProperty("sonar.sources"))).isEqualTo(baseDir.resolve("src/main/groovy"));
    assertThat(Paths.get(props.getProperty("sonar.tests"))).isEqualTo(baseDir.resolve("src/test/groovy"));

    assertThat(Paths.get(props.getProperty("sonar.junit.reportsPath"))).isEqualTo(baseDir.resolve("build/test-results"));
    assertThat(Paths.get(props.getProperty("sonar.groovy.jacoco.reportPath"))).isEqualTo(baseDir.resolve("build/jacoco/test.exec"));
    assertThat(Paths.get(props.getProperty("sonar.jacoco.reportPath"))).isEqualTo(baseDir.resolve("build/jacoco/test.exec"));
  }

  @Test
  public void loadSonarScannerPropertiesEnv() throws Exception {
    Map<String, String> env = new HashMap<>();
    env.put("SONARQUBE_SCANNER_PARAMS", "{\"sonar.host.url\" : \"myhost\" }");
    Properties props = runGradlewSonarQubeSimulationModeWithEnv("/java-gradle-simple", env);

    assertThat(props).contains(entry("sonar.host.url", "myhost"));
  }

  @Test
  public void module_inclusion_duplicate_key() throws Exception {
    Properties props = runGradlewSonarQubeSimulationMode("/module-inclusion");

    assertThat(props).contains(entry("sonar.projectKey", "com.mygroup:root_project"));
    assertThat(props.getProperty("sonar.modules").split(",")).containsOnly(":toplevel1", ":toplevel2");

    assertThat(props).contains(entry(":toplevel1.sonar.moduleKey", "com.mygroup:root_project:toplevel1"));
    assertThat(props).contains(entry(":toplevel2.sonar.moduleKey", "com.mygroup:root_project:toplevel2"));

    assertThat(props).contains(entry(":toplevel1.sonar.modules", ":toplevel1:plugins"));
    assertThat(props).contains(entry(":toplevel2.sonar.modules", ":toplevel2:plugins"));

    assertThat(props).contains(entry(":toplevel1.:toplevel1:plugins.sonar.moduleKey", "com.mygroup:root_project:toplevel1:plugins"));
    assertThat(props).contains(entry(":toplevel2.:toplevel2:plugins.sonar.moduleKey", "com.mygroup:root_project:toplevel2:plugins"));
  }

}
