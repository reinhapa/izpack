/*
 * Copyright 2016 Julien Ponge, René Krell and the IzPack team.
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

package com.izforge.izpack.installer.container.provider;

import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.ConsolePrefs;
import com.izforge.izpack.api.data.Variables;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.installer.data.ConsoleInstallData;
import com.izforge.izpack.util.Platform;

public final class ConsoleInstallDataProvider
{

  public static AutomatedInstallData provide(Resources resources, Variables variables, Platform platform)
  {
        final ConsoleInstallData installData = new ConsoleInstallData(variables, platform);
        loadConsoleInstallData(installData, resources);
        return installData;
    }

    /**
     * Load GUI preference information.
     *
     * @param installData the console installation data
     * @throws Exception
     */
    private static void loadConsoleInstallData(ConsoleInstallData installData, Resources resources)
    {
        installData.consolePrefs = (ConsolePrefs) resources.getObject("ConsolePrefs");
    }

}
