/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
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

package com.izforge.izpack.panels.userinput.console.check;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.izforge.izpack.panels.userinput.console.AbstractConsoleFieldTest;
import com.izforge.izpack.panels.userinput.field.check.CheckField;
import com.izforge.izpack.panels.userinput.field.check.TestCheckFieldConfig;
import com.izforge.izpack.util.Platform;
import com.izforge.izpack.util.Platforms;


/**
 * Tests the {@link ConsoleCheckField}.
 *
 * @author Tim Anderson
 */
public class ConsoleCheckFieldTest extends AbstractConsoleFieldTest
{

    @Override
    protected Platform usedPlatform()
    {
        return Platforms.HP_UX;
    }

    /**
     * Tests selection of the default value.
     */
    @Test
    public void testSelectDefaultValue()
    {
        String variable = "check";

        TestCheckFieldConfig config = new TestCheckFieldConfig(variable, "selected", "unselected");
        config.setInitialValue("selected");

        CheckField model = new CheckField(config, installData);
        ConsoleCheckField field = new ConsoleCheckField(model, console, prompt);

        console.addScript("Select default", "\n");
        assertTrue(field.display());

        assertEquals("selected", installData.getVariable(variable));
    }

    /**
     * Tests setting the state from deselected to selected.
     */
    @Test
    public void testSelect()
    {
        String variable = "check";
        CheckField model = new CheckField(new TestCheckFieldConfig(variable, "selected", "unselected"), installData);
        ConsoleCheckField field = new ConsoleCheckField(model, console, prompt);


        console.addScript("Select", "1\n");
        assertTrue(field.display());

        assertEquals("selected", installData.getVariable(variable));
    }

    /**
     * Tests setting the state from selected to deselected.
     */
    @Test
    public void testDeselect()
    {
        String variable = "check";
        TestCheckFieldConfig config = new TestCheckFieldConfig(variable, "selected", "unselected");
        config.setInitialValue("true");
        CheckField model = new CheckField(config, installData);
        ConsoleCheckField field = new ConsoleCheckField(model, console, prompt);

        console.addScript("Deselect", "0\n");
        assertTrue(field.display());

        assertEquals("unselected", installData.getVariable(variable));
    }

        /**
     * Tests setting the state from selected to deselected.
     */
    @Test
    public void testReselect()
    {
        String variable = "check";
        TestCheckFieldConfig config = new TestCheckFieldConfig(variable, "selected", "unselected");
        config.setInitialValue("true");
        CheckField model = new CheckField(config, installData);
        ConsoleCheckField field = new ConsoleCheckField(model, console, prompt);

        console.addScript("Deselect", "0\n");
        console.addScript("Redo", "1\n");
        assertTrue(field.display());

        assertEquals("unselected", installData.getVariable(variable));
    }
}
