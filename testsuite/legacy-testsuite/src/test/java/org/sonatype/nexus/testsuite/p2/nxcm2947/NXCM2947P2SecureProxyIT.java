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
package org.sonatype.nexus.testsuite.p2.nxcm2947;

import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;
import org.sonatype.tests.http.server.api.ServerProvider;

import org.junit.Test;

public class NXCM2947P2SecureProxyIT
    extends AbstractNexusProxyP2IT
{

  public NXCM2947P2SecureProxyIT() {
    super("nxcm2947");
  }

  protected ServerProvider buildServerProvider() {
    final ServerProvider serverProvider = super.buildServerProvider();
    serverProvider.addAuthentication("/*", "BASIC");
    serverProvider.addUser("admin", "admin");
    return serverProvider;
  }

  @Test
  public void test() throws Exception {
    installAndVerifyP2Feature();
  }

}
