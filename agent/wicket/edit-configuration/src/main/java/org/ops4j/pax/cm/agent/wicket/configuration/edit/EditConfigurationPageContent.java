/*
 * Copyright 2006 Edward Yakop.
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
package org.ops4j.pax.cm.agent.wicket.configuration.edit;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.cm.agent.ConfigurationConstant;
import org.ops4j.pax.wicket.service.AbstractPageContent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import wicket.Page;
import wicket.PageParameters;

/**
 * @author Edward Yakop
 * @since 0.1.0
 */
public final class EditConfigurationPageContent extends AbstractPageContent
{
    private static final Log m_logger = LogFactory.getLog( EditConfigurationPageContent.class );
    private final BundleContext m_bundleContext;

    public EditConfigurationPageContent(
        BundleContext bundleContext, String applicationName, String pageName )
    {
        super( bundleContext, EditConfigurationPage.PAGE_ID, applicationName, pageName );

        m_bundleContext = bundleContext;
    }

    /**
     * Returns the {@code EditConfigurationPage} class.
     *
     * @since 0.1.0
     */
    public Class<EditConfigurationPage> getPageClass()
    {
        return EditConfigurationPage.class;
    }

    /**
     * Create an instance of {@code EditConfigurationPage} with the specified {@code params}. The specified
     * {@code params} must have values for {@code "PARAM_KEY_PID"} key. The {@code "PARAM_KEY_LOCATION"} key value is
     * optional. Returns {@code null} if the specified params does not follow the stated requirement or retrieval of
     * configuration from configuration admin service fails.
     *
     * @param params The edit configuration page parameters. This argument must not be {@code null}.
     *
     * @return An instance of {@code EditConfigurationPage} initialized with the specified {@code params}.
     *
     * @throws IllegalArgumentException Thrown if the specified {@code params} is {@code null}.
     * @see org.ops4j.pax.cm.agent.ConfigurationConstant#PARAM_KEY_PID
     * @see org.ops4j.pax.cm.agent.ConfigurationConstant#PARAM_KEY_LOCATION
     * @since 0.1.0
     */
    public Page createPage( PageParameters params )
        throws IllegalArgumentException
    {
        NullArgumentException.validateNotNull( params, "params" );

        String pid = params.getString( ConfigurationConstant.PARAM_KEY_PID );
        String location = params.getString( ConfigurationConstant.PARAM_KEY_LOCATION );

        if( pid == null || pid.length() == 0 )
        {
            return null;
        }

        ServiceReference configAdmin = m_bundleContext.getServiceReference( ConfigurationAdmin.class.getName() );
        ConfigurationAdmin configAdminService = (ConfigurationAdmin) m_bundleContext.getService( configAdmin );
        EditConfigurationPage editConfigurationPage = null;
        try
        {
            Configuration configuration = configAdminService.getConfiguration( pid, location );
            editConfigurationPage = new EditConfigurationPage( pid, configuration );
        } catch( IOException e )
        {
            m_logger.error( "Configuration [" + pid + "] with location [" + location + "] can not be accessed.", e );
        }
        m_bundleContext.ungetService( configAdmin );

        return editConfigurationPage;
    }
}
