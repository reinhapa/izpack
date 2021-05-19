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
package com.izforge.izpack.panels.test;

import org.mockito.Mockito;

import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.Variables;
import com.izforge.izpack.api.exception.ContainerException;
import com.izforge.izpack.api.resource.Locales;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.gui.log.Log;
import com.izforge.izpack.installer.container.provider.GUIInstallDataProvider;
import com.izforge.izpack.util.Platform;


/**
 * Container for GUI panel testing.
 *
 * @author Tim Anderson
 */
public class TestGUIPanelContainer extends AbstractTestPanelContainer
{

    /**
     * Constructs a {@code TestGUIPanelContainer}.
     *
     * @throws ContainerException if initialisation fails
     */
    public TestGUIPanelContainer()
    {
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
        addComponent(Log.class, Mockito.mock(Log.class));
//        addComponent(InstallDataConfiguratorWithRules.class);
//        addComponent(GUIPrompt.class);
//        addComponent(GUIInstallDataMockProvider.class);
//        addComponent(IconsProvider.class);
    }

    @Override
    public AutomatedInstallData get(Resources resources, Variables variables, Platform platform, Locales locales)
    {
        return GUIInstallDataProvider.provide(resources, variables, platform);
    }
}
