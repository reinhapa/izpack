/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2012 Tim Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.core.container;

import com.izforge.izpack.api.container.Container;
import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.Variables;
import com.izforge.izpack.api.exception.ContainerException;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.util.Platform;

import jakarta.enterprise.inject.Vetoed;

/**
 * Default implementation of the {@link Container} interface.
 *
 * @author Tim Anderson
 */
@Vetoed
public class DefaultContainer extends AbstractContainer
{
    private final Class<?> classUnderTest;

    /**
     * Constructs a <tt>DefaultContainer</tt>.
     *
     * @throws ContainerException if initialization fails
     */
    public DefaultContainer()
    {
        this(null);
    }

    public DefaultContainer(Class<?> classUnderTest)
    {
        this.classUnderTest = classUnderTest;
        initialise();
    }

    @Override
    protected void fillContainer()
    {
        super.fillContainer();
        if (classUnderTest != null) {
            addComponent(classUnderTest);
        }
    }

    @Override
    public AutomatedInstallData get(Resources resources, Variables variables, Platform platform)
    {
        return new AutomatedInstallData(variables, platform);
    }
}
