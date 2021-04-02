package com.izforge.izpack.core.container;

import com.izforge.izpack.api.exception.IzPackException;

public interface CdiInitializationContext {

  <T> void addComponent(Class<T> componentType);

  <T> void addComponent(Class<T> componentType, Object implementation);

  <T> T getComponent(Class<T> componentType);

  void addConfig(String name, Object value) throws IzPackException;

  <T> void removeComponent(Class<T> componnentType);

  void dispose();

  @Deprecated
  void removeChildContext(CdiInitializationContext context);

  @Deprecated
  CdiInitializationContext makeChildContainer();
}
