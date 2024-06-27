/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2002 Marcus Stursberg
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

package com.izforge.izpack.core.provider;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.izforge.izpack.api.adaptator.impl.XMLElementImpl;
import com.izforge.izpack.api.data.InstallDataHandler;
import com.izforge.izpack.api.data.DynamicInstallerRequirementValidator;
import com.izforge.izpack.api.data.DynamicVariable;
import com.izforge.izpack.api.data.Info;
import com.izforge.izpack.api.data.Info.TempDir;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.InstallerRequirement;
import com.izforge.izpack.api.data.Pack;
import com.izforge.izpack.api.data.PackInfo;
import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.data.ScriptParserConstant;
import com.izforge.izpack.api.data.Value;
import com.izforge.izpack.api.data.Variables;
import com.izforge.izpack.api.exception.ResourceException;
import com.izforge.izpack.api.exception.ResourceNotFoundException;
import com.izforge.izpack.api.resource.Locales;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.util.Housekeeper;
import com.izforge.izpack.util.IoHelper;
import com.izforge.izpack.util.OsVersion;
import com.izforge.izpack.util.PlatformModelMatcher;
import com.izforge.izpack.util.TemporaryDirectory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Abstract base class for providers of {@link InstallData}.
 */
@ApplicationScoped
public class InstallDataProvider
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(InstallDataProvider.class.getName());


    @Produces
    public InstallData provide(
            InstallDataHandler installDataHandler, Resources resources, Locales locales,
            Variables variables, Housekeeper housekeeper, PlatformModelMatcher matcher)
        throws IOException, ClassNotFoundException
    {
        InstallData installData = installDataHandler.create(resources, variables, matcher.getCurrentPlatform(), locales);
        loadInstallData(installData, resources, matcher, housekeeper);
        loadDynamicVariables(variables, installData, resources);
        loadDynamicConditions(installData, resources);
        loadDefaultLocale(installData, locales);
        loadInstallerRequirements(installData, resources);
        addCustomLangpack(installData, locales);
        addUserInputLangpack(installData, locales);
        return installData;
    }

    /**
     * Loads the installation data. Also sets environment variables to <code>installdata</code>.
     * All system properties are available as $SYSTEM_<variable> where <variable> is the actual
     * name _BUT_ with all separators replaced by '_'. Properties with null values are never stored.
     * Example: $SYSTEM_java_version or $SYSTEM_os_name
     *
     * @param installData the installation data to populate
     * @param resources   the resources
     * @param matcher     the platform-model matcher
     * @param housekeeper the housekeeper for cleaning up temporary files
     * @throws IOException            for any I/O error
     * @throws ClassNotFoundException if a serialized object's class cannot be found
     * @throws ResourceException      for any resource error
     */
    @SuppressWarnings("unchecked")
    private void loadInstallData(InstallData installData, Resources resources,
        PlatformModelMatcher matcher, Housekeeper housekeeper)
        throws IOException, ClassNotFoundException
    {
        // We load the Info data
        Info info = (Info) resources.getObject("info");

        // We put the Info data as variables
        installData.setVariable(ScriptParserConstant.APP_NAME, info.getAppName());
        if (info.getAppURL() != null)
        {
            installData.setVariable(ScriptParserConstant.APP_URL, info.getAppURL());
        }
        installData.setVariable(ScriptParserConstant.APP_VER, info.getAppVersion());
        if (info.getUninstallerCondition() != null)
        {
            installData.setVariable("UNINSTALLER_CONDITION", info.getUninstallerCondition());
        }

        installData.setInfo(info);
        // Set the installation path in a default manner
        String dir = getDir(resources);
        String installPath = dir + info.getAppName();
        if (info.getInstallationSubPath() != null)
        { // A sub-path was defined, use it.
            installPath = IoHelper.translatePath(dir + info.getInstallationSubPath(), installData.getVariables());
        }

        installData.setDefaultInstallPath(installPath);
        // Pre-set install path from a system property,
        // for instance in unattended installations
        installPath = System.getProperty(InstallData.INSTALL_PATH);
        if (installPath != null)
        {
            installData.setInstallPath(installPath);
        }

        // We read the panels order data
        List<Panel> panelsOrder = (List<Panel>) resources.getObject("panelsOrder");
        List<Panel> installDataPanelsOrder = installData.getPanelsOrder();
        installDataPanelsOrder.clear();
        installDataPanelsOrder.addAll(panelsOrder);

        // We read the packs data
        List<PackInfo> packInfos = (List<PackInfo>)resources.getObject("packs.info");
        List<Pack> allPacks = installData.getAllPacks();
        // initialize all packs first
        allPacks.clear();
        packInfos.forEach(packInfo -> allPacks.add(packInfo.getPack()));
        // update available packs
        installData.updateAvailablePacks(pack -> matcher.matchesCurrentPlatform(pack.getOsConstraints()));
        // update selected based on the available packs
        installData.updateSelectedPacks(Pack::isPreselected);

        setStandardVariables(installData, dir);

        // We load the user variables
        Properties properties = (Properties) resources.getObject("vars");
        if (properties != null)
        {
            Set<String> vars = properties.stringPropertyNames();
            for (String varName : vars)
            {
                installData.setVariable(varName, properties.getProperty(varName));
            }
        }

        // Create any temp directories
        Set<TempDir> tempDirs = info.getTempDirs();
        if (null != tempDirs && tempDirs.size() > 0)
        {
            for (TempDir tempDir : tempDirs)
            {
                TemporaryDirectory directory = new TemporaryDirectory(tempDir, installData, housekeeper);
                directory.create();
                directory.deleteOnExit();
            }
        }
    }

    public static void setStandardVariables(InstallData installData, String dir)
    {
        // Determine the hostname and IP address
        String hostname;
        String canonicalHostname;
        String IPAddress;

        try
        {
            InetAddress localHost = InetAddress.getLocalHost();
            IPAddress = localHost.getHostAddress();
            hostname = localHost.getHostName();
            canonicalHostname = localHost.getCanonicalHostName();
        }
        catch (Exception exception)
        {
            logger.log(Level.WARNING, "Failed to determine hostname and IP address", exception);
            hostname = "";
            canonicalHostname = "";
            IPAddress = "";
        }

        installData.setVariable("APPLICATIONS_DEFAULT_ROOT", dir);
        installData.setVariable(ScriptParserConstant.JAVA_HOME, System.getProperty("java.home"));
        installData.setVariable(ScriptParserConstant.CLASS_PATH, System.getProperty("java.class.path"));
        installData.setVariable(ScriptParserConstant.USER_HOME, System.getProperty("user.home"));
        installData.setVariable(ScriptParserConstant.USER_NAME, System.getProperty("user.name"));
        installData.setVariable(ScriptParserConstant.IP_ADDRESS, IPAddress);
        installData.setVariable(ScriptParserConstant.HOST_NAME, hostname);
        installData.setVariable(ScriptParserConstant.CANONICAL_HOST_NAME, canonicalHostname);
        installData.setVariable(ScriptParserConstant.FILE_SEPARATOR, File.separator);
    }

    /**
     * Add the contents of a custom langpack to the default langpack, if it exists.
     *
     * @param installData the install data to be used
     */
    public static void addCustomLangpack(InstallData installData, Locales locales)
    {
        addLangpack(Resources.CUSTOM_TRANSLATIONS_RESOURCE_NAME, "custom", installData, locales);
    }

    /**
     * Add the contents of a custom langpack to the default langpack, if it exists.
     *
     * @param installData the install data to be used
     */
    public static void addUserInputLangpack(InstallData installData, Locales locales)
    {
        addLangpack(Resources.USER_INPUT_TRANSLATIONS_RESOURCE_NAME, "user input", installData, locales);
    }

    private static void addLangpack(String resName, String langPackName, InstallData installData, Locales locales)
    {
        // We try to load and add langpack.
        try
        {
            installData.getMessages().add(locales.getMessages(resName));
            logger.fine("Found " + langPackName + " langpack for " + installData.getLocaleISO3());
        }
        catch (ResourceNotFoundException exception)
        {
            logger.fine("No " + langPackName + " langpack for " + installData.getLocaleISO3() + " available");
        }
    }

    private String getDir(Resources resources)
    {
        // We determine the operating system and the initial installation path
        String dir;
        if (OsVersion.IS_WINDOWS)
        {
            dir = buildWindowsDefaultPath(resources);
        }
        else if (OsVersion.IS_OSX)
        {
            dir = "/Applications/";
        }
        else
        {
            if (new File("/usr/local/").canWrite())
            {
                dir = "/usr/local/";
            }
            else
            {
                dir = System.getProperty("user.home") + File.separatorChar;
            }
        }
        return dir;
    }

    /**
     * Get the default path for Windows (i.e Program Files/...).
     * Windows has a Setting for this in the environment and in the registry.
     * Just try to use the setting in the environment. If it fails for whatever reason, we take the former solution (buildWindowsDefaultPathFromProps).
     *
     * @param resources the resources
     * @return The Windows default installation path for applications.
     */
    private String buildWindowsDefaultPath(Resources resources)
    {
        try
        {
            //get value from environment...
            String prgFilesPath = IoHelper.getenv("ProgramFiles");
            if (prgFilesPath != null && prgFilesPath.length() > 0)
            {
                return prgFilesPath + File.separatorChar;
            }
            else
            {
                return buildWindowsDefaultPathFromProps(resources);
            }
        }
        catch (Exception exception)
        {
            logger.log(Level.WARNING, exception.getMessage(), exception);
            return buildWindowsDefaultPathFromProps(resources);
        }
    }

    /**
     * just plain wrong in case the programfiles are not stored where the developer expects them.
     * E.g. in custom installations of large companies or if used internationalized version of windows with a language pack.
     *
     * @return the program files path
     */
    private String buildWindowsDefaultPathFromProps(Resources resources)
    {
        StringBuilder result = new StringBuilder("");
        try
        {
            // We load the properties
            Properties props = new Properties();
            props.load(resources.getInputStream("/com/izforge/izpack/installer/win32-defaultpaths.properties"));

            // We look for the drive mapping
            String drive = System.getProperty("user.home");
            if (drive.length() > 3)
            {
                drive = drive.substring(0, 3);
            }

            // Now we have it :-)
            result.append(drive);

            // Ensure that we have a trailing backslash (in case drive was
            // something
            // like "C:")
            if (drive.length() == 2)
            {
                result.append("\\");
            }

            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();
            String language_country = language + "_" + country;

            // Try the most specific combination first
            if (null != props.getProperty(language_country))
            {
                result.append(props.getProperty(language_country));
            }
            else if (null != props.getProperty(language))
            {
                result.append(props.getProperty(language));
            }
            else
            {
                result.append(props.getProperty(Locale.ENGLISH.getLanguage()));
            }
        }
        catch (Exception err)
        {
            result = new StringBuilder("C:\\Program Files");
        }

        return result.toString();
    }

    /**
     * Loads Dynamic Variables.
     *
     * @param variables   the collection to added variables to
     * @param installData the installation data
     */
    @SuppressWarnings("unchecked")
    private void loadDynamicVariables(Variables variables, InstallData installData, Resources resources)
    {
        try
        {
            List<DynamicVariable> dynamicVariables = (List<DynamicVariable>) resources.getObject("dynvariables");
            for (DynamicVariable dynamic : dynamicVariables)
            {
                Value value = dynamic.getValue();
                value.setInstallData(installData);
                variables.add(dynamic);
            }
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Cannot find optional dynamic variables", e);
        }
    }

    /**
     * Loads dynamic conditions.
     *
     * @param installData the installation data
     * @param resources   the resources
     */
    @SuppressWarnings("unchecked")
    private void loadDynamicConditions(InstallData installData, Resources resources)
    {
        try
        {
            List<DynamicInstallerRequirementValidator> conditions
                    = (List<DynamicInstallerRequirementValidator>) resources.getObject("dynconditions");
            installData.setDynamicInstallerRequirements(conditions);
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Cannot find optional dynamic conditions", e);
        }
    }

    /**
     * Load installer conditions.
     *
     * @param installData the installation data
     * @throws IOException               for any I/O error
     * @throws ClassNotFoundException    if a serialized object's class cannot be found
     * @throws ResourceNotFoundException if the resource cannot be found
     */
    @SuppressWarnings("unchecked")
    private void loadInstallerRequirements(InstallData installData, Resources resources)
    {
        List<InstallerRequirement> requirements =
                (List<InstallerRequirement>) resources.getObject("installerrequirements");
        installData.setInstallerRequirements(requirements);
    }

    /**
     * Load a default locale in the installData
     *
     * @param installData the installation data
     * @param locales     the supported locales
     * @throws IOException for any I/O error
     */
    public static void loadDefaultLocale(InstallData installData, Locales locales)
    {
        Locale locale = locales.getLocale();
        if (locale != null)
        {
            installData.setInstallationRecord(new XMLElementImpl("AutomatedInstallation"));
            installData.setLocale(locale, locales.getISOCode());
            installData.setMessages(locales.getMessages());
        }
    }

}
