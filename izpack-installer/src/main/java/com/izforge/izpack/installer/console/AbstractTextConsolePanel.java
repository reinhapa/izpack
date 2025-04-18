/*
 * Copyright 2016 Julien Ponge, René Krell and the IzPack team.
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

package com.izforge.izpack.installer.console;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.rules.RulesEngine;
import com.izforge.izpack.installer.panel.PanelView;
import com.izforge.izpack.installer.util.PanelHelper;
import com.izforge.izpack.util.Console;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Abstract console panel for displaying paginated text.
 *
 * @author Tim Anderson
 */
public abstract class AbstractTextConsolePanel extends AbstractConsolePanel
{

    /**
     * The logger.
     */
    private static final Logger LOGGER = Logger.getLogger(AbstractTextConsolePanel.class.getName());

    /**
     * Constructs an {@code AbstractTextConsolePanel}.
     *
     * @param panel the parent panel/view. May be {@code null}
     */
    protected AbstractTextConsolePanel(PanelView<ConsolePanel> panel)
    {
        super(panel);
    }

    /**
     * Runs the panel using the supplied properties.
     *
     * @param installData the installation data
     * @param properties  the properties
     * @return <tt>true</tt>
     */
    @Override
    public boolean run(InstallData installData, Properties properties)
    {
        return true;
    }

    /**
     * Runs the panel using the specified console.
     * <p/>
     * If there is no text to display, the panel will return <tt>false</tt>.
     *
     * @param installData the installation data
     * @param console     the console
     * @return <tt>true</tt> if the panel ran successfully, otherwise <tt>false</tt>
     */
    @Override
    public boolean run(InstallData installData, Console console)
    {
        printHeadLine(installData, console);

        String panelLabel = getPanelLabel(installData);
        if (panelLabel != null)
        {
            console.println(panelLabel);
        }

        String text = getText();
        if (substituteVariables()) {
            text = installData.getVariables().replace(text);
        }
        if (text != null)
        {
            Panel panel = getPanel();
            RulesEngine rules = installData.getRules();
            boolean paging = Boolean.parseBoolean(panel.getConfigurationOptionValue("console-text-paging", rules));
            boolean wordwrap = Boolean.parseBoolean(panel.getConfigurationOptionValue("console-text-wordwrap", rules));

            try
            {
                console.printMultiLine(text, wordwrap, paging);
            }
            catch (IOException e)
            {
                LOGGER.warning("Displaying multiline text failed: " + e.getMessage());
            }
        }
        else
        {
            LOGGER.warning("No text to display");
        }
        return promptEndPanel(installData, console);
    }

    /**
     * Returns the panel label to display.
     *
     * @param installData the installation data
     * @return the panel label. A <tt>null</tt> indicates no panel label to display
     */
    protected String getPanelLabel(InstallData installData) {
        String titleMessageKey = PanelHelper.getPanelTitleMessageKey(getPanel(), "info", installData);
        return installData.getMessages().get(titleMessageKey);
    }

    /**
     * Returns the text to display.
     *
     * @return the text. A <tt>null</tt> indicates failure
     */
    protected abstract String getText();

    /**
     * Returns true if variables are to be substituted in the text or else false.
     *
     * @return true, the default implementation
     */
    protected boolean substituteVariables() {
        return true;
    }

    /**
     * Helper to strip HTML from text.
     * From code originally developed by Jan Blok.
     *
     * @param html the text. May be {@code null}
     * @return the text with HTML removed
     */
    protected String removeHTML(String html)
    {
        if (html != null)
        {
            Document doc = Jsoup.parse(html);
            Element titleElement = doc.selectFirst("title");
            String titleText = (titleElement != null) ? titleElement.text()+ "\n\n" : "";
            String bodyText = doc.body().text();
            return  titleText + bodyText.replaceAll("\n+", "\n");
        }
        return "";
    }
}
