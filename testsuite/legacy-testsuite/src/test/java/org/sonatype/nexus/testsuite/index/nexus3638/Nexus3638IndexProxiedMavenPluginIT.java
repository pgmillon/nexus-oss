/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.testsuite.index.nexus3638;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.test.utils.TestProperties;
import org.sonatype.tests.http.runner.junit.ServerResource;
import org.sonatype.tests.http.server.api.ServerProvider;
import org.sonatype.tests.http.server.fluent.Server;

import org.apache.commons.io.FileUtils;
import org.apache.maven.index.artifact.Gav;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.*;

public class Nexus3638IndexProxiedMavenPluginIT
    extends AbstractNexusIntegrationTest
{
  @Rule
  public ServerResource serverResource = new ServerResource(buildServerProvider());

  protected ServerProvider buildServerProvider() {
    return Server.withPort(TestProperties.getInteger("webproxy-server-port"))
        .serve("/nexus3638/*").fromDirectory(getTestFile("repo"))
        .getServerProvider();
  }

  @Test
  public void downloadSnapshotPlugin()
      throws Throwable
  {
    final Gav gav = GavUtil.newGav("org.apache.maven.plugins", "maven-invoker-plugin", "1.6-SNAPSHOT");

    downloadFile(
        new URL(
            "http://localhost:"
                + TestProperties.getInteger("webproxy-server-port")
                +
                "/nexus3638/org/apache/maven/plugins/maven-invoker-plugin/1.6-SNAPSHOT/maven-invoker-plugin-1.6-20100922.124315-3.jar"),
        "target/downloads/nexus3638");

    Thread[] threads = new Thread[10];
    final Throwable[] errors = new Throwable[threads.length];
    for (int i = 0; i < threads.length; i++) {
      Thread thread = new Downloader(this, gav, i, errors);

      threads[i] = thread;
    }

    // launch them all
    for (Thread thread : threads) {
      thread.start();
    }

    // w8 all to finish
    for (Thread thread : threads) {
      thread.join();
    }

    // check for errors
    for (Throwable throwable : errors) {
      if (throwable != null) {
        throw throwable;
      }
    }

    // make sure it does have enough time to index the artifact!
    TaskScheduleUtil.waitForAllTasksToStop();

    List<NexusArtifact> items =
        getSearchMessageUtil().searchForGav(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), "nexus3638");
    Assert.assertFalse(items.isEmpty());
    Assert.assertEquals(items.get(0).getPackaging(), "maven-plugin");

    final File nexusLog = getNexusLogFile();

    if (nexusLog != null) {
      String logContent = FileUtils.readFileToString(nexusLog);
      // NEXUS-3707
      MatcherAssert.assertThat(logContent,
          not(containsString("Rename operation failed after -1 retries in -1 ms intervals")));
      MatcherAssert.assertThat(logContent, not(containsString("java.util.zip.ZipException")));
    }
  }
}
