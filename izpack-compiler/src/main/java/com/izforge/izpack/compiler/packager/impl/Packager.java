/*
 * $Id$
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
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

package com.izforge.izpack.compiler.packager.impl;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.adaptator.impl.XMLElementImpl;
import com.izforge.izpack.api.data.Pack;
import com.izforge.izpack.api.data.PackCompression;
import com.izforge.izpack.api.data.PackFile;
import com.izforge.izpack.api.data.PackInfo;
import com.izforge.izpack.api.rules.RulesEngine;
import com.izforge.izpack.compiler.data.CompilerData;
import com.izforge.izpack.compiler.listener.PackagerListener;
import com.izforge.izpack.compiler.merge.CompilerPathResolver;
import com.izforge.izpack.merge.MergeManager;
import com.izforge.izpack.merge.resolve.MergeableResolver;
import com.izforge.izpack.util.NoCloseOutputStream;
import com.izforge.izpack.util.StreamSupport;
import org.apache.commons.io.output.CountingOutputStream;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

/**
 * The packager class. The packager is used by the compiler to put files into an
 * installer, and create the actual installer files.
 *
 * @author Julien Ponge
 * @author Chadwick McHenry
 */
public class Packager extends PackagerBase
{
    private static final Logger logger = Logger.getLogger(Packager.class.getName());

    private final CompilerData compilerData;

    /**
     * Constructs a <tt>Packager</tt>.
     *
     * @param properties        the properties
     * @param listener          the packager listener
     * @param mergeManager      the merge manager
     * @param pathResolver      the path resolver
     * @param mergeableResolver the mergeable resolver
     * @param compilerData      the compiler data
     */
    public Packager(Properties properties, PackagerListener listener, MergeManager mergeManager,
                    CompilerPathResolver pathResolver, MergeableResolver mergeableResolver, CompilerData compilerData,
                    RulesEngine rulesEngine)
    {
        super(properties, listener, mergeManager, pathResolver, mergeableResolver,
                compilerData, rulesEngine);
        this.compilerData = compilerData;
    }

    private JarOutputStream getJarOutputStream(Path jarFile) throws IOException
    {
        Files.deleteIfExists(jarFile);
        return getJarOutputStream(jarFile, compilerData);
    }

    /**
     * Write packs to the installer jar, or each to a separate jar.
     *
     * @throws IOException for any I/O error
     */
    @Override
    protected void writePacks(JarOutputStream installerJar) throws IOException
    {
        List<PackInfo> packs = getPacksList();
        final int num = packs.size();
        sendMsg("Writing " + num + " Pack" + (num > 1 ? "s" : "") + " into installer");

        // Map to remember pack number and bytes offsets of back references
        Map<Path, PackFile> storedFiles = new HashMap<>();

        // Force UTF-8 encoding in order to have proper ZipEntry names.
        int packNumber = 0;
        final IXMLElement root = new XMLElementImpl("packs");

        for (PackInfo packInfo : packs)
        {
            final Pack pack = packInfo.getPack();
            pack.setFileSize(0);

            sendMsg("Writing Pack " + packNumber + ": " + pack.getName(), PackagerListener.MSG_VERBOSE);

            final ZipEntry entry;
            final String streamResourceName = "packs/pack-" + pack.getName();
            final JarOutputStream packJar;
            if (packSeparateJars())
            {
                // TODO REFACTOR : Use a mergeManager for each packages that will be added to the main merger
                Path jarFile = Paths.get(getInfo().getInstallerBase() + ".pack-" + pack.getName() + ".jar");
                packJar = getJarOutputStream(jarFile);
                entry = new ZipEntry(streamResourceName);
            }
            else
            {
                packJar = installerJar;
                entry = new ZipEntry(RESOURCES_PATH + streamResourceName);
            }

            packJar.putNextEntry(entry);
            packJar.flush(); // flush before we start counting

            try (CountingOutputStream packOutputStream = new CountingOutputStream(new NoCloseOutputStream(packJar)))
            {
                for (PackFile packFile : packInfo.getPackFiles())
                {
                    boolean addFile = !pack.isLoose();
                    Path file = packInfo.getFile(packFile).toPath();

                    // use a back reference if file was in previous pack, and in
                    // same jar
                    PackFile linkedPackFile = storedFiles.get(file);

                    if (linkedPackFile != null && !packSeparateJars())
                    {
                        // Save backreference link
                        logger.fine("File " + packFile.getTargetPath() + " is a backreference, linked to " + linkedPackFile.getTargetPath());
                        packFile.setLinkedPackFile(linkedPackFile);
                        addFile = false;
                    }

                    if (addFile && !packFile.isDirectory())
                    {
                        packFile.setStreamResourceName(streamResourceName);
                        packFile.setStreamOffset(packOutputStream.getByteCount()); // get the position

                        PackCompression comprFormat = getInfo().getCompressionFormat();
                        CountingOutputStream proxyOutputStream = new CountingOutputStream(new NoCloseOutputStream(packOutputStream));
                        try (OutputStream finalStream = StreamSupport.compressedOutput(comprFormat, proxyOutputStream))
                        {
                            long bytesWritten = Files.copy(file, finalStream);
                            if (bytesWritten != packFile.length())
                            {
                                throw new IOException("File size mismatch when reading " + file);
                            }
                        }
                        packFile.setSize(proxyOutputStream.getByteCount());
                        logger.fine("File " + packFile.getTargetPath() + " added compressed as " + comprFormat.toName()
                                + " (" + packFile.length() + " -> " + packFile.size() + " bytes)");

                        storedFiles.put(file, packFile);
                    }

                    // even if not written, it counts towards pack size
                    pack.addFileSize(packFile.length());
                }

                if (pack.getFileSize() > pack.getSize())
                {
                    pack.setSize(pack.getFileSize());
                }

                // Cleanup
                packOutputStream.flush();
                packOutputStream.close();
                packJar.closeEntry();
            }
            finally
            {
                packJar.flush();
                // close pack specific jar if required
                if (packSeparateJars())
                {
                    packJar.close();
                }
            }

            IXMLElement child = new XMLElementImpl("pack", root);
            child.setAttribute("name", pack.getName());
            child.setAttribute("size", Long.toString(pack.getSize()));
            child.setAttribute("fileSize", Long.toString(pack.getFileSize()));
            if (pack.getLangPackId() != null)
            {
                child.setAttribute("id", pack.getLangPackId());
            }
            root.addChild(child);

            packNumber++;
        }

        // Now that we know sizes, write pack metadata to primary jar.
        installerJar.putNextEntry(new ZipEntry(PACKSINFO_RESOURCE_PATH));
        try (ObjectOutputStream out = new ObjectOutputStream(new NoCloseOutputStream(installerJar)))
        {
            out.writeObject(packs);
        }
        installerJar.closeEntry();
    }

    @Override
    public void addConfigurationInformation(IXMLElement data)
    {
    }
}
