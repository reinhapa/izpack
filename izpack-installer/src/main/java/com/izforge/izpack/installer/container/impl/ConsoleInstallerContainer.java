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

package com.izforge.izpack.installer.container.impl;


import com.izforge.izpack.api.exception.ContainerException;
import com.izforge.izpack.core.container.CdiInitializationContext;
import com.izforge.izpack.core.handler.ConsolePrompt;
import com.izforge.izpack.installer.console.ConsoleInstaller;
import com.izforge.izpack.installer.console.ConsolePanelAutomationHelper;
import com.izforge.izpack.installer.container.provider.ConsoleInstallDataProvider;
import com.izforge.izpack.installer.container.provider.ConsolePanelsProvider;
import com.izforge.izpack.installer.container.provider.ConsolePrefsProvider;
import com.izforge.izpack.installer.container.provider.MessagesProvider;
import com.izforge.izpack.installer.language.LanguageConsoleDialog;
import com.izforge.izpack.installer.multiunpacker.MultiVolumeUnpackerAutomationHelper;
import com.izforge.izpack.installer.unpacker.ConsolePackResources;
import com.izforge.izpack.util.Console;

/**
 * Installer container for console installation mode.
 *
 * @author Tim Anderson
 */
public class ConsoleInstallerContainer extends InstallerContainer
{

    /**
     * Constructs a <tt>ConsoleInstallerContainer</tt>.
     *
     * @throws ContainerException if initialisation fails
     */
    public ConsoleInstallerContainer()
    {
        initialise();
    }

    /**
     * Constructs a <tt>ConsoleInstallerContainer</tt>.
     * <p/>
     * This constructor is provided for testing purposes.
     *
     * @param container the underlying container
     * @throws ContainerException if initialisation fails
     */
    protected ConsoleInstallerContainer(CdiInitializationContext container)
    {
        initialise(container);
    }

    /**
     * Registers components with the container.
     */
    @Override
    protected void registerComponents()
    {
        super.registerComponents();
        addComponent(ConsoleInstallDataProvider.class);
        addComponent(ConsolePanelsProvider.class);
        addComponent(MessagesProvider.class); // required by ConsolePrompt and Console
        addComponent(ConsolePrefsProvider.class); // required by Console
        addComponent(Console.class);
        addComponent(ConsolePrompt.class);
        addComponent(ConsoleInstaller.class);
        addComponent(ConsolePanelAutomationHelper.class);
        addComponent(ConsolePackResources.class);
        addComponent(MultiVolumeUnpackerAutomationHelper.class);
        addComponent(LanguageConsoleDialog.class);
    }
}
