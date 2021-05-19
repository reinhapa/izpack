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

package com.izforge.izpack.test;

import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.Variables;
import com.izforge.izpack.api.exception.ContainerException;
import com.izforge.izpack.api.resource.Locales;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.core.container.AbstractContainer;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.util.Platform;

/**
 * Container for condition tests.
 *
 * @author Anthonin Bonnefoy
 */
public class TestConditionContainer extends AbstractContainer
{
    private Class<?> classUnderTest;

    /**
     * Constructs a <tt>TestMergeContainer</tt>.
     *
     * @throws ContainerException if initialisation fails
     */
    public TestConditionContainer(Class<?> classUnderTest)
    {
        this.classUnderTest = classUnderTest;
        initialise();
    }

    /**
     * Invoked by {@link #initialise} to fill the container.
     *
     * @throws ContainerException if initialisation fails
     */
    @Override
    protected void fillContainer()
    {
        super.fillContainer();
        addComponent(classUnderTest);
        addComponent(TestAutomatedInstallDataProvider.class);

//        addComponent(Platform.class, Platforms.HP_UX);

//        addComponent(GUIInstallData.class);
//        addComponent(RulesEngineImpl.class);
//        addComponent(VariableSubstitutorImpl.class);
//        addComponent(MergeableResolver.class);
//        addComponent(Properties.class);
//        addComponent(DefaultVariables.class);
//        addComponent(ConditionContainer.class);
    }

    @Override
    public AutomatedInstallData get(Resources resources, Variables variables, Platform platform, Locales locales)
    {
        return new GUIInstallData(variables, platform);
    }
}
