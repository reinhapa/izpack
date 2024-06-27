package com.izforge.izpack.compiler.container;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.installer.data.ConsoleInstallData;
import org.junit.runners.model.FrameworkMethod;

import com.izforge.izpack.api.data.Variables;
import com.izforge.izpack.api.resource.Locales;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.core.container.CdiInitializationContext;
import com.izforge.izpack.installer.container.impl.InstallerContainer;
import com.izforge.izpack.util.Platform;


/**
 * Container for integration testing
 *
 * @author Anthonin Bonnefoy
 */
public class TestConsoleInstallationContainer extends AbstractTestInstallationContainer
{
    public TestConsoleInstallationContainer(Class<?> klass, FrameworkMethod frameworkMethod)
    {
        super(klass, frameworkMethod);
        initialise();
    }


    @Override
    protected InstallerContainer fillInstallerContainer(CdiInitializationContext container)
    {
        return new TestConsoleInstallerContainer(container);
    }

    @Override
    public InstallData create(Resources resources, Variables variables, Platform platform, Locales locales)
    {
        return new ConsoleInstallData(variables, platform, resources);
    }

}
