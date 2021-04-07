/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
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

package com.izforge.izpack.core.rules;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.izforge.izpack.api.exception.ContainerException;
import com.izforge.izpack.api.rules.Condition;
import com.izforge.izpack.api.rules.RulesEngine;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;


/**
 * Condition container.
 *
 * @author Anthonin Bonnefoy
 */
@ApplicationScoped
public class ConditionContainer
{
  private final Map<Condition, Instance<Condition>> instances;

  /**
   * Constructs a <tt>ConditionContainer</tt>.
   *
   * @param container the parent container
   */
  public ConditionContainer()
  {
      instances = new ConcurrentHashMap<>();
  }

  /**
   * Retrieve a condition by its component type.
   * <p/>
   * If the condition type is registered but an instance does not exist, then it will be created.
   *
   * @param rules the rules engine used
   * @param conditionType the type of the condition
   * @return the corresponding object instance, or <tt>null</tt> if it does not exist
   * @throws ContainerException if condition creation fails
   */
  public <T extends Condition> T getCondition(RulesEngine rules, Class<T> conditionType)
  {
      try
      {
        return conditionType.getConstructor(RulesEngine.class).newInstance(rules);
      }
      catch (ReflectiveOperationException e1)
      {
          try
          {
          return conditionType.getConstructor().newInstance();
          }
          catch (ReflectiveOperationException e2)
          {
              throw new ContainerException("Failed to initialize condition type " + conditionType, e2);
          }
      } 
  }

  @PreDestroy
  public void dispose() {
      instances.forEach(ConditionContainer::disposeCondition);
  }
  
  private static <T extends Condition> void disposeCondition(T condition, Instance<T> conditionInstance)  {
      conditionInstance.destroy(condition);
  }
}
