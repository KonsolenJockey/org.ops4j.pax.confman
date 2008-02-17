/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cm.scanner.commons.internal;

import org.ops4j.pax.cm.api.ConfigurationManager;

/**
 * ConfigurationManager setter. Implemented by classes that need a configurer service.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 12, 2008
 */
public interface ConfigurerSetter
{

    /**
     * Setter.
     *
     * @param configurationManager configurationManager; null if no configurationManager is available.
     */
    void setConfigurer( ConfigurationManager configurationManager );

}
