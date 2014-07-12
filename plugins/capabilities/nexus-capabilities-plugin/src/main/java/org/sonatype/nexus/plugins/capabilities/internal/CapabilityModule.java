/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2014 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.plugins.capabilities.internal;

import javax.inject.Named;

import org.sonatype.nexus.plugins.capabilities.internal.storage.CapabilityStorage;
import org.sonatype.nexus.plugins.capabilities.internal.storage.OrientCapabilityStorage;
import org.sonatype.nexus.plugins.capabilities.validator.ValidatorFactory;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * Capabilities guice module.
 *
 * @since 3.0
 */
@Named
public class CapabilityModule
    extends AbstractModule
{

  @Override
  protected void configure() {
    bind(CapabilityStorage.class).to(OrientCapabilityStorage.class);

    install(new FactoryModuleBuilder().build(ActivationConditionHandlerFactory.class));
    install(new FactoryModuleBuilder().build(ValidityConditionHandlerFactory.class));
    install(new FactoryModuleBuilder().build(ValidatorFactory.class));
  }
}