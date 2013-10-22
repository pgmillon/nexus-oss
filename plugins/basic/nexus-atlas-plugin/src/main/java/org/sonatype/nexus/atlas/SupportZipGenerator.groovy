/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

package org.sonatype.nexus.atlas

import groovy.transform.ToString
import org.sonatype.nexus.atlas.SupportZipGenerator.Request
import org.sonatype.nexus.atlas.SupportZipGenerator.Result

/**
 * Generates a support ZIP file.
 *
 * @since 2.7
 */
interface SupportZipGenerator
{
  @ToString(includePackage=false, includeNames=true)
  static class Request
  {
    boolean systemInformation

    boolean threadDump

    boolean metrics

    boolean configuration

    boolean security

    boolean log

    boolean limitFileSizes

    boolean limitZipSize
  }

  @ToString(includePackage=false, includeNames=true)
  static class Result
  {
    boolean truncated

    File file
  }

  /**
   * The directory where support ZIP files are generated.
   */
  File getDirectory()

  /**
   * Generate a support ZIP for the given request.
   */
  Result generate(Request request)
}