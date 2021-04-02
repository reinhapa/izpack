package com.izforge.izpack.compiler.container;

import org.junit.runners.model.FrameworkMethod;

import com.izforge.izpack.core.container.CdiInitializationContext;
import com.izforge.izpack.installer.container.impl.InstallerContainer;


/**
 * Container for integration testing
 */
public class TestAutomatedInstallationContainer extends AbstractTestInstallationContainer
{
    public TestAutomatedInstallationContainer(Class<?> klass, FrameworkMethod frameworkMethod)
    {
        super(klass, frameworkMethod);
        initialise();
    }


    @Override
    protected InstallerContainer fillInstallerContainer(CdiInitializationContext container)
    {
        return new TestAutomatedInstallerContainer(container);
    }

}
