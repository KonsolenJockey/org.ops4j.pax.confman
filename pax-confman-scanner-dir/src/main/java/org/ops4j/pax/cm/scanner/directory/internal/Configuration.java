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
package org.ops4j.pax.cm.scanner.directory.internal;

import java.io.File;

/**
 * Directory scanner system properties.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, February 16, 2008
 */
public interface Configuration
{

    /**
     * Returns an array of directories to be scanned.
     *
     * @return array of directories to be scanned
     */
    File[] getDirectories();

    /**
     * Returns the number of milliseconds between scanns.
     *
     * @return number of milliseconds or null if default should be used.
     */
    Long getInterval();

}