package com.izforge.izpack.core.container;

import com.izforge.izpack.api.exception.IzPackException;

public class CdiInitializationContextImpl implements CdiInitializationContext {

  @Override
  public <T> void addComponent(Class<T> componentType) {
  }

  @Override
  public <T> void addComponent(Class<T> componentType, Object implementation) {
  }

  @Override
  public <T> void removeComponent(Class<T> componnentType) {
  }

  @Override
  public void addConfig(String name, Object value) throws IzPackException {
  }

  @Override
  public <T> T getComponent(Class<T> componentType) {
    return null;
  }

  @Override
  public void dispose() {}

  @Override
  public void removeChildContext(CdiInitializationContext container) {
  }

  @Override
  public CdiInitializationContext makeChildContainer() {
    return null;
  }

}
