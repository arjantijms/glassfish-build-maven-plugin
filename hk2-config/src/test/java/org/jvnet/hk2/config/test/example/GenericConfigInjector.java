/*
 * Copyright (c) 2015, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.jvnet.hk2.config.test.example;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.InjectionTarget;
import org.jvnet.hk2.config.NoopConfigInjector;

@Service(
    name = "generic-config",
    metadata = "target=org.jvnet.hk2.config.test.example.GenericConfig,"
        + "@name=optional,"
        + "@name=datatype:java.lang.String,"
        + "@name=leaf,"
        + "key=@name,"
        + "keyed-as=org.jvnet.hk2.config.test.example.GenericConfig,"
        + "<generic-config>=org.jvnet.hk2.config.test.example.GenericConfig"
)
@InjectionTarget(GenericConfig.class)
public class GenericConfigInjector extends NoopConfigInjector {

}
