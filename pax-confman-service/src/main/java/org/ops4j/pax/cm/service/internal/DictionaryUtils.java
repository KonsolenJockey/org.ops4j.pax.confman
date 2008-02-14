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
package org.ops4j.pax.cm.service.internal;

import java.util.Dictionary;
import java.util.Enumeration;

/**
 * Dictionary related utils.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, February 14, 2008
 */
class DictionaryUtils
{

    /**
     * Utility class.
     */
    private DictionaryUtils()
    {
        // utility class
    }

    /**
     * Copy all entries with keys matching regex from source to target.
     *
     * @param regex  matching expression; if null it will match everything
     * @param source source dictionary
     * @param target target dictionary
     */
    @SuppressWarnings( "unchecked" )
    static void copyDictionary( final String regex,
                                final Dictionary source,
                                final Dictionary target )
    {
        if( source != null && target != null && !source.isEmpty() )
        {
            final Enumeration keys = source.keys();
            while( keys.hasMoreElements() )
            {
                Object key = keys.nextElement();
                if( regex == null || key.toString().matches( regex ) )
                {
                    target.put( key, source.get( key ) );
                }
            }
        }
    }

    /**
     * Copy all entries from source to target.
     *
     * @param source source dictionary
     * @param target target dictionary
     */
    static void copyDictionary( final Dictionary source,
                                final Dictionary target )
    {
        copyDictionary( null, source, target );
    }

}
