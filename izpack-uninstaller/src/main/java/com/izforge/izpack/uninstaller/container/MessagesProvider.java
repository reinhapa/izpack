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

package com.izforge.izpack.uninstaller.container;

import java.io.IOException;

import com.izforge.izpack.api.exception.ResourceNotFoundException;
import com.izforge.izpack.api.resource.Locales;
import com.izforge.izpack.api.resource.Messages;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;


/**
 * A provider of a {@link Messages} instances.
 *
 * @author Tim Anderson
 */
@ApplicationScoped
public class MessagesProvider
{

    /**
     * Provides the  messages.
     *
     * @param locales the supported locales
     * @return the locale database
     * @throws IOException               for any I/O error
     * @throws ResourceNotFoundException if <em>langpack.xml</em> cannot be found
     */
    @Produces
    public Messages provide(Locales locales) throws IOException
    {
        return locales.getMessages("langpack.xml");
    }
}
