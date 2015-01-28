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
package org.sonatype.nexus.yum.internal.createrepo

import org.junit.Test
import org.sonatype.sisu.litmus.testsupport.TestSupport

class YumPackageParserTest
extends TestSupport
{

  @Test
  void 'parse package'() {
    File rpm = util.resolveFile('src/test/ut-resources/rpms/ant/1.7.1-13/ant-1.7.1-13.el6.i686.rpm')
    YumPackage yumPackage = new YumPackageParser().parse(
        new FileInputStream(rpm),
        'Packages/ant-1.7.1-13.el6.i686.rpm',
        rpm.lastModified()
    )
    new YumRepositoryWriter(util.createTempDir('repodata')).withCloseable { YumRepositoryWriter writer ->
      writer.push(yumPackage)
    }

  }

}