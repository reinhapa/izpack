/*
 * IzPack - Copyright 2001-2013 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2020 Patrick Reinhart
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

package com.izforge.izpack.panels.userinput.gui.text;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

import com.izforge.izpack.api.handler.Prompt;
import com.izforge.izpack.panels.userinput.field.Field;
import com.izforge.izpack.panels.userinput.field.ValidationStatus;
import com.izforge.izpack.panels.userinput.field.text.TextArea;
import com.izforge.izpack.panels.userinput.gui.GUIField;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;


/**
 * Text area view.
 *
 * @author Patrick Reinhart
 */
public class GUITextArea extends GUIField implements FocusListener, DocumentListener
{

    /**
     * The component.
     */
    private final JTextArea textArea;
    private final JScrollPane scrollPane;

    private transient boolean changed = false;


    /**
     * Constructs a {@code GUITextField}.
     *
     * @param field the field
     */
    public GUITextArea(TextArea field)
    {
        super(field);

        textArea = new JTextArea(field.getInitialValue(), field.getHeight(), field.getSize());
        textArea.setName(field.getVariable());
        textArea.setCaretPosition(0);
        textArea.getDocument().addDocumentListener(this);
        textArea.addFocusListener(this);
        textArea.setMargin(new Insets(2, 2, 2, 2));
        textArea.setEditable(true);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        scrollPane = new JScrollPane(textArea, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
        addField(scrollPane);
        addTooltip();
    }

    /**
     * Updates the field from the view.
     *
     * @param prompt the prompt to display messages
     * @param skipValidation set to true when wanting to save field data without validating
     * @return {@code true} if the field was updated, {@code false} if the view is invalid
     */
    @Override
    public boolean updateField(Prompt prompt, boolean skipValidation)
    {
        boolean result = false;
        String text = this.textArea.getText();
        Field field = getField();
        ValidationStatus status = field.validate(text);
        if (skipValidation || status.isValid())
        {
            field.setValue(text);
            result = true;
        }
        else
        {
            String message = status.getMessage();
            if (message == null)
            {
                message = "Text entered did not pass validation.";
            }
            warning(message, prompt);
        }
        return result;
    }

    /**
     * Updates the view from the field.
     *
     * @return {@code true} if the view was updated
     */
    @Override
    public boolean updateView()
    {
        boolean result = super.updateView();
        Field f = getField();
        String value = f.getInitialValue();

        if (value != null)
        {
            replaceValue(value);
            result = true;
        }
        else
        {
            // Set default value here for getting current variable values replaced
            String defaultValue = f.getDefaultValue();
            if (defaultValue != null)
            {
                replaceValue(defaultValue);
            }
        }

        return result;
    }

    private void replaceValue(String value)
    {
        boolean changed = false;
        String oldValue = textArea.getText();
        if (!(oldValue == null ? value == null : oldValue.equals(value)))
        {
            textArea.getDocument().removeDocumentListener(this);
            textArea.setText(replaceVariables(value));
            textArea.getDocument().addDocumentListener(this);
            changed = true;
        }
        setChanged(changed);
    }

    public synchronized void setChanged(boolean changed)
    {
        this.changed = changed;
    }

    private synchronized boolean isChanged()
    {
        return changed;
    }


    // FocusListener interface

    @Override
    public void focusGained(FocusEvent event)
    {
        textArea.selectAll();
    }
    @Override
    public void focusLost(FocusEvent event)
    {
        if (isChanged())
        {
            notifyUpdateListener();
            setChanged(false);
        }
        textArea.select(0, 0);
    }


    // DocumentListener interface

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        setChanged(true);
    }
    @Override
    public void removeUpdate(DocumentEvent e)
    {
        setChanged(true);
    }
    @Override
    public void changedUpdate(DocumentEvent e)
    {
        setChanged(true);
    }

    @Override
    public JComponent getFirstFocusableComponent()
    {
        return textArea;
    }
}