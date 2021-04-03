package com.izforge.izpack.compiler.container;

import org.junit.runners.model.FrameworkMethod;

import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.Variables;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.core.container.CdiInitializationContext;
import com.izforge.izpack.installer.container.impl.InstallerContainer;
import com.izforge.izpack.installer.container.provider.ConsoleInstallDataProvider;
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
    public AutomatedInstallData get(Resources resources, Variables variables, Platform platform)
    {
        return ConsoleInstallDataProvider.provide(resources, variables, platform);
    }

}
