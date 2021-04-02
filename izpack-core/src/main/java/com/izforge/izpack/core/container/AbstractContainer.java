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

import com.izforge.izpack.api.container.Container;
import com.izforge.izpack.api.exception.ContainerException;
import com.izforge.izpack.api.exception.IzPackClassNotFoundException;
import com.izforge.izpack.api.exception.IzPackException;


/**
 * Abstract implementation of the {@link Container} interface.
 *
 * @author Anthonin Bonnefoy
 * @author Tim Anderson
 */
public abstract class AbstractContainer implements Container
{
    private final Map<String, Object> panels;

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
    public <T> void addComponent(Class<T> componentType, Object implementation)
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
     * Creates a child container.
     * <p/>
     * A child container:
     * <ul>
     * <li>may have different objects keyed on the same identifiers as its parent.</li>
     * <li>will query its parent for dependencies if they aren't available</li>
     * <li>is disposed when its parent is disposed</li>
     * </ul>
     *
     * @return a new container
     * @throws ContainerException if creation fails
     */
    @Override
    public Container createChildContainer()
    {
        // TODO - dispose() won't be invoked on the Container, just the MutablePicoContainer.
        // not an issue for now
        return new ChildContainer(container);
    }

    /**
     * Removes a child container.
     *
     * @param child the container to remove
     * @return <tt>true</tt> if the container was removed
     */
    @Override
    public void removeChildContainer(Container child)
    {
        if (child instanceof AbstractContainer)
        {
            container.removeChildContext(((AbstractContainer) child).container);
        }
    }

    /**
     * Disposes of the container and all of its child containers.
     */
    @Override
    public void dispose()
    {
        container.dispose();
    }

    /**
     * Returns a class given its name.
     *
     * @param className the class name
     * @param superType the super type
     * @return the corresponding class
     * @throws ClassCastException           if <tt>className</tt> does not implement or extend <tt>superType</tt>
     * @throws IzPackClassNotFoundException if the class cannot be found
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(String className, Class<T> superType)
    {
        @SuppressWarnings("rawtypes")
		Class type;
        try
        {
            // Using the superclass class loader to load the child to avoid multiple copies of the superclass being
            // loaded in separate class loaders. This is typically an issue during testing where
            // the same classes may be loaded twice - once by maven, and once by the installer.
            ClassLoader classLoader = superType.getClassLoader();
            if (classLoader == null)
            {
                // may be null for bootstrap class loader
                classLoader = getClass().getClassLoader();
            }
            type = classLoader.loadClass(className);
            if (!superType.isAssignableFrom(type))
            {
                throw new ClassCastException("Class '" + type.getName() + "' does not implement "
                                                     + superType.getName());
            }
        }
        catch (ClassNotFoundException exception)
        {
            throw new IzPackClassNotFoundException(className, exception);
        }
        return type;
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
     * @param container the container
     * @throws ContainerException if initialisation fails, or the container has already been initialised
     */
    protected void initialise(CdiInitializationContext container)
    {
        if (this.container != null)
        {
            throw new ContainerException("Container already initialised");
        }
        this.container = container;
        try
        {
            fillContainer(container);
        }
        catch (ContainerException exception)
        {
            throw exception;
        }
        catch (Exception exception)
        {
            throw new ContainerException(exception);
        }
    }

    /**
     * Invoked by {@link #initialise} to fill the container.
     * <p/>
     * This exposes the underlying <tt>PicoContainer</tt> to enable subclasses to perform complex initialisation.
     * <p/>
     * This implementation delegates to {@link #fillContainer()}.
     *
     * @param container the underlying container
     * @throws ContainerException if initialisation fails
     */
    protected final void fillContainer(CdiInitializationContext container)
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
    }

    /**
     * Returns the underlying container.
     *
     * @return the underlying container, or <tt>null</tt> if {@link #initialise} hasn't been invoked
     */
    protected CdiInitializationContext getContainer()
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

    /**
     * Concrete container used by {@link #createChildContainer()}.
     */
    private static class ChildContainer extends AbstractContainer
    {

        /**
         * Constructs a ChildContainer.
         *
         * @param parent the parent container
         */
        public ChildContainer(CdiInitializationContext parent)
        {
            super(parent.makeChildContainer());
        }
    }

}
