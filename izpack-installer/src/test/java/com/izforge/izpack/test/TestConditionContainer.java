package com.izforge.izpack.test;

import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.Variables;
import com.izforge.izpack.api.exception.ContainerException;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.core.container.AbstractContainer;
import com.izforge.izpack.installer.container.provider.GUIInstallDataProvider;
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
    public AutomatedInstallData get(Resources resources, Variables variables, Platform platform)
    {
        return GUIInstallDataProvider.provide(resources, variables, platform);
    }
}
