package com.izforge.izpack.test;

import java.util.Properties;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.exception.ContainerException;
import com.izforge.izpack.api.rules.RulesEngine;
import com.izforge.izpack.api.substitutor.VariableSubstitutor;
import com.izforge.izpack.core.container.AbstractContainer;
import com.izforge.izpack.core.data.DefaultVariables;
import com.izforge.izpack.core.rules.ConditionContainer;
import com.izforge.izpack.core.rules.RulesEngineImpl;
import com.izforge.izpack.core.substitutor.VariableSubstitutorImpl;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.merge.resolve.MergeableResolver;
import com.izforge.izpack.util.Platform;
import com.izforge.izpack.util.Platforms;

/**
 * Container for condition tests.
 *
 * @author Anthonin Bonnefoy
 */
public class TestConditionContainer extends AbstractContainer
{

    /**
     * Constructs a <tt>TestConditionContainer</tt>.
     *
     * @throws ContainerException if initialisation fails
     */
    public TestConditionContainer()
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
        addComponent(InstallData.class, GUIInstallData.class);
        addComponent(RulesEngine.class, RulesEngineImpl.class);
        addComponent(VariableSubstitutor.class, VariableSubstitutorImpl.class);
        addComponent(MergeableResolver.class);
        addComponent(Properties.class);
        addComponent(DefaultVariables.class);
        addComponent(ConditionContainer.class);
        addComponent(AbstractContainer.class, this);
        addComponent(Platform.class, Platforms.HP_UX);
    }
}
