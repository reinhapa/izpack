package com.izforge.izpack.compiler.container;

import com.izforge.izpack.api.data.InstallData;
import org.junit.runners.model.FrameworkMethod;

import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.Variables;
import com.izforge.izpack.api.resource.Locales;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.core.container.CdiInitializationContext;
import com.izforge.izpack.installer.container.impl.InstallerContainer;
import com.izforge.izpack.installer.container.provider.GUIInstallDataProvider;
import com.izforge.izpack.util.Platform;

/**
 * Container for integration testing
 *
 * @author Anthonin Bonnefoy
 */
public class TestGUIInstallationContainer extends AbstractTestInstallationContainer
{

    public TestGUIInstallationContainer(Class<?> klass, FrameworkMethod frameworkMethod)
    {
        super(klass, frameworkMethod);
        initialise();
    }

    @Override
    protected InstallerContainer fillInstallerContainer(CdiInitializationContext container)
    {
        return new TestGUIInstallerContainer(container);
    }

    @Override
    public InstallData get(Resources resources, Variables variables, Platform platform, Locales locales)
    {
        return GUIInstallDataProvider.provide(resources, variables, platform);
    }
}
