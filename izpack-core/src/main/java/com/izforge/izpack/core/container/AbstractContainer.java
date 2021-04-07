/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2010 Anthonin Bonnefoy
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

package com.izforge.izpack.core.container;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.izforge.izpack.api.container.Container;
import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.AutomatedInstallDataSupplier;
import com.izforge.izpack.api.data.Variables;
import com.izforge.izpack.api.exception.ContainerException;
import com.izforge.izpack.api.exception.IzPackException;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.core.container.cdi.CdiInitializationContextImpl;
import com.izforge.izpack.util.Platform;


/**
 * Abstract implementation of the {@link Container} interface.
 *
 * @author Anthonin Bonnefoy
 * @author Tim Anderson
 */
public abstract class AbstractContainer implements Container
{
    private final Map<String, Object> panels;
    private final AtomicReference<State> init = new AtomicReference<>(State.NEW);

    /**
     * The underlying container.
     */
    private CdiInitializationContext container;


    /**
     * Constructs an <tt>AbstractContainer</tt>.
     * <p/>
     * The container must be initialised via {@link #initialise()} before use.
     */
    public AbstractContainer()
    {
        this(null);
    }

    /**
     * Constructs an <tt>AbstractContainer</tt>.
     * <p/>
     * If a container is provided, {@link #initialise(CdiInitializationContext)} will be invoked. Subclasses should only
     * provide a container if they don't require their constructor to complete before <tt>initialise</tt> is called.
     *
     * @param container the underlying container. May be <tt>null</tt>
     * @throws ContainerException if initialisation fails
     */
    public AbstractContainer(CdiInitializationContext container)
    {
        panels = new HashMap<>();
        if (container != null)
        {
            initialise(container);
        }
    }

    /**
     * Register a component type.
     *
     * @param componentType the component type
     * @throws ContainerException if registration fails
     */
    @Override
    public <T> void addComponent(Class<T> componentType)
    {
        container.addComponent(componentType);
    }

    /**
     * Register a component.
     *
     * @param componentType the component type
     * @param implementation the component implementation
     * @throws ContainerException if registration fails
     */
    @Override
    public <T, I extends T> void addComponent(Class<T> componentType, I implementation)
    {
        container.addComponent(componentType, implementation);
    }

    /**
     * Retrieve a component by its component type.
     * <p/>
     * If the component type is registered but an instance does not exist, then it will be created.
     *
     * @param componentType the type of the component
     * @return the corresponding object instance, or <tt>null</tt> if it does not exist
     * @throws ContainerException if component creation fails
     */
    @Override
    public <T> T getComponent(Class<T> componentType)
    {
        return container.getComponent(componentType);
    }

    /**
     * Register a config item.
     *
     * @param name  the name of the config item
     * @param value the value of the config item
     * @throws ContainerException if registration fails
     */
    public void addConfig(String name, Object value)
    {
        try
        {
            container.addConfig(name, value);
        }
        catch (IzPackException exception)
        {
            throw new ContainerException(exception);
        }
    }

    /**
     * Disposes of the container and all of its child containers.
     */
    @Override
    public final void dispose()
    {
        container.close();
    }

    @Override
    public final void addPanel(String id, Object panel) {
        Object existingPanel = panels.putIfAbsent(id, panel);
        if (existingPanel != null)
        {
            throw new IllegalArgumentException("Panel with id [" + id + " already exist: " + existingPanel);
        }
    }

    @Override
    public final Object getPanel(String id) {
        return panels.get(id);
    }

    @Override
    public AutomatedInstallData get(Resources resources, Variables variables, Platform platform)
    {
        return new AutomatedInstallData(variables, platform);
    }

    protected final <T> void removeComponent(Class<T> componnentType) {
        container.removeComponent(componnentType);
    }

    /**
     * Initialises the container.
     * <p/>
     * This must only be invoked once.
     *
     * @throws ContainerException if initialisation fails, or the container has already been initialised
     */
    protected final void initialise()
    {
        initialise(createContainer());
    }

    /**
     * Initialises the container.
     * <p/>
     * This must only be invoked once.
     *
     * @param context the CDI initialization context
     * @throws ContainerException if initialisation fails, or the container has already been initialised
     */
    protected final void initialise(CdiInitializationContext context)
    {
        Objects.requireNonNull(context, "context must not be null");
        if (!init.compareAndSet(State.NEW, State.INITIALIZING))
        {
            throw new ContainerException("Container already initialised");
        }
        this.container = context;
        try
        {
            fillContainer(context);
            context.start();
        }
        catch (ContainerException exception)
        {
            throw exception;
        }
        catch (Exception exception)
        {
            throw new ContainerException(exception);
        }
        init.set(State.INITIALIZED);
    }

    /**
     * Invoked by {@link #initialise} to fill the container.
     * <p/>
     * This exposes the underlying <tt>PicoContainer</tt> to enable subclasses to perform complex initialisation.
     * <p/>
     * This implementation delegates to {@link #fillContainer()}.
     *
     * @param context the CDI initialization context
     * @throws ContainerException if initialisation fails
     */
    protected final void fillContainer(CdiInitializationContext context)
    {
        fillContainer();
    }

    /**
     * Invoked by {@link #initialise} to fill the container.
     * <p/>
     * This implementation is a no-op.
     * <p/>
     *
     * @throws ContainerException if initialisation fails
     */
    protected void fillContainer()
    {
        addComponent(Container.class, this);
        addComponent(AutomatedInstallDataSupplier.class, this);
    }

    /**
     * Returns the underlying container.
     *
     * @return the underlying container, or <tt>null</tt> if {@link #initialise} hasn't been invoked
     */
    protected final CdiInitializationContext getContext()
    {
        return container;
    }

    /**
     * Creates a new container.
     *
     * @return a new container
     */
    protected final CdiInitializationContext createContainer()
    {
        return new CdiInitializationContextImpl();
    }

    private enum State
    {
        NEW, INITIALIZING, INITIALIZED;
    }
}
