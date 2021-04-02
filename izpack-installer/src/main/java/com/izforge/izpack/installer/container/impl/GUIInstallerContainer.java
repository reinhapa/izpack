package com.izforge.izpack.installer.container.impl;

import javax.swing.SwingUtilities;

import com.izforge.izpack.api.exception.ContainerException;
import com.izforge.izpack.api.exception.IzPackException;
import com.izforge.izpack.core.container.CdiInitializationContext;
import com.izforge.izpack.gui.GUIPrompt;
import com.izforge.izpack.gui.log.Log;
import com.izforge.izpack.installer.container.provider.GUIInstallDataProvider;
import com.izforge.izpack.installer.container.provider.IconsProvider;
import com.izforge.izpack.installer.container.provider.IzPanelsProvider;
import com.izforge.izpack.installer.gui.DefaultNavigator;
import com.izforge.izpack.installer.gui.InstallerController;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.izforge.izpack.installer.gui.SplashScreen;
import com.izforge.izpack.installer.language.LanguageDialog;
import com.izforge.izpack.installer.multiunpacker.MultiVolumeUnpackerHelper;
import com.izforge.izpack.installer.unpacker.GUIPackResources;
import com.izforge.izpack.installer.unpacker.IUnpacker;

/**
 * GUI Installer container.
 */
public class GUIInstallerContainer extends InstallerContainer
{

    /**
     * Constructs a <tt>GUIInstallerContainer</tt>.
     *
     * @throws ContainerException if initialisation fails
     */
    public GUIInstallerContainer()
    {
        initialise();
    }

    /**
     * Constructs a <tt>GUIInstallerContainer</tt>.
     * <p/>
     * This constructor is provided for testing purposes.
     *
     * @param container the underlying container
     * @throws ContainerException if initialisation fails
     */
    protected GUIInstallerContainer(CdiInitializationContext container)
    {
        initialise(container);
    }

    /**
     * Registers components with the container.
     */
    @Override
    protected void registerComponents()
    {
        super.registerComponents();
        addComponent(GUIInstallDataProvider.class);
        addComponent(IzPanelsProvider.class);
        addComponent(IconsProvider.class);
        addComponent(GUIPrompt.class);
        addComponent(InstallerController.class);
        addComponent(DefaultNavigator.class);
        addComponent(InstallerFrame.class);
        addComponent(Log.class);
        addComponent(GUIPackResources.class);
        addComponent(MultiVolumeUnpackerHelper.class);
        addComponent(SplashScreen.class);
        addComponent(LanguageDialog.class);
    }

    /**
     * Resolve components.
     */
    @Override
    protected void resolveComponents()
    {
        super.resolveComponents();
        try
        {
            SwingUtilities.invokeAndWait(new Runnable()
            {
                @Override
                public void run()
                {
                    InstallerFrame frame = getComponent(InstallerFrame.class);
                    IUnpacker unpacker = getComponent(IUnpacker.class);
                    frame.setUnpacker(unpacker);
                }
            });
        }
        catch (Exception exception)
        {
            throw new IzPackException(exception);
        }

    }
}
