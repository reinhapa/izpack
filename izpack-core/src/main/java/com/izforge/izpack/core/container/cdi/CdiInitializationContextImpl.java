/*
 * IzPack - Copyright 2001-20212 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/ http://izpack.codehaus.org/
 *
 * Copyright 2010 Anthonin Bonnefoy Copyright 2012 Tim Anderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.izforge.izpack.core.container.cdi;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.izforge.izpack.api.exception.IzPackException;
import com.izforge.izpack.core.container.CdiInitializationContext;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

/**
 * Collects all bean definitions an use those for later creation of a bean
 * 
 * @author Patrick Reinhart
 */
public final class CdiInitializationContextImpl implements CdiInitializationContext {
  private final AtomicReference<State> init = new AtomicReference<>(State.NEW);

  private final HashMap<Class<?>, BeanImplementation> components;
  private final HashMap<String, Object> configurations;

  private SeContainer seContainer;

  public CdiInitializationContextImpl() {
    components = new HashMap<>();
    configurations = new HashMap<>();
  }

  private void checkNew() {
    if (!State.NEW.equals(init.get())) {
      throw new IllegalStateException("Unable to modify components if not in new state");
    }
  }


  @Override
  public <T> void addComponent(Class<T> type) {
    addComponent(type, null);
  }

  @Override
  public <T, I extends T> void addComponent(Class<T> type, I implementation) {
    checkNew();
    BeanImplementation existing =
        components.putIfAbsent(type, new BeanImplementation(implementation));
    if (existing != null) {
      throw new IllegalStateException("Entry for given type [" + type + "] allready registered");
    }
  }

  @Override
  public <T> void removeComponent(Class<T> componnentType) {
    checkNew();
    components.remove(componnentType);
  }

  @Override
  public void addConfig(String name, Object value) throws IzPackException {
    checkNew();
    configurations.put(name, value);
  }

  @Override
  public <T> T getComponent(Class<T> componentType) {
    if (!State.RUNNING.equals(init.get())) {
      throw new IllegalStateException("Unable to get component when not yet running");
    }
    return seContainer.select(componentType).get();
  }

  @Override
  public void start() {
    if (init.compareAndSet(State.NEW, State.INITIALIZING)) {
      SeContainerInitializer initializer = SeContainerInitializer.newInstance();
      ManualBeanDefinitions beanDefinitions = new ManualBeanDefinitions();
      initializer.addProperty("javax.enterprise.inject.scan.implicit", Boolean.TRUE); // scan also .jar files without beans.xml
      initializer.addExtensions(beanDefinitions);
      components.forEach((type, implementation) -> implementation.register(initializer, type, beanDefinitions));
      seContainer = initializer.initialize();
      init.set(State.RUNNING);
    }
  }

  @Override
  public void close() {
    if (init.compareAndSet(State.RUNNING, State.STOPPING)) {
      seContainer.close();
      seContainer = null;
      init.set(State.STOPPED);
    }
  }

  private enum State {
    NEW, INITIALIZING, RUNNING, STOPPING, STOPPED;
  }

  private static final class BeanImplementation {
    private final Object implementation;

    BeanImplementation(Object implementation) {
      this.implementation = implementation;
    }

    void register(SeContainerInitializer initializer, Class<?> beanType, Consumer<DynamicBean<?>> implementationConsumer) {
      if (implementation==null)
      {
        initializer.addBeanClasses(beanType);
      }
      else
      {
        implementationConsumer.accept(dynamicBean(beanType, implementation));
      }
    }

    static <T> DynamicBean<T> dynamicBean(Class<T> type, Object instance) {
      return new DynamicBean<>(type, () -> type.cast(instance));
    }
  }
}
