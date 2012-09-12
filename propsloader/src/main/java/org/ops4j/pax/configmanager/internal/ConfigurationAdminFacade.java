/*
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
package org.ops4j.pax.configmanager.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.configmanager.IConfigurationFileHandler;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * {@code ConfigurationAdminFacade} has most of the code from the old {@code Activator}.
 * 
 * @author Edward Yakop
 * @author Makas Tzavellas
 * @author Lars Heppler
 */
final class ConfigurationAdminFacade
{

    private static final Log LOGGER = LogFactory.getLog(ConfigurationAdminFacade.class);

    public static final String DIRECTORY_NAME_FACTORIES = "factories";
    public static final String DIRECTORY_NAME_SERVICES = "services";
    public static final String DEFAULT_CONFIGURATION_LOCATION = "configurations";

    /**
     * System property to set where the ConfigurationAdminFacade should load the configuration files from.
     */
    public static final String BUNDLES_CONFIGURATION_LOCATION = "bundles.configuration.location";
    public static final String OSGI_CONFIGURATION_AREA = "osgi.configuration.area";
    private final List<IConfigurationFileHandler> m_handlers;
    private ConfigurationAdmin m_configAdminService;
    private final ManagedFactoryPropertiesProcessor m_processor = new ManagedFactoryPropertiesProcessor();
    /**
     * Property resolver used to resolve properties.
     */
    private final PropertyResolver m_propertyResolver;

    public ConfigurationAdminFacade( PropertyResolver propertyResolver )
    {
        m_propertyResolver = propertyResolver;
        m_handlers = new ArrayList<IConfigurationFileHandler>();
    }


    /**
     * Add the specified {@code handler} to this {@code ConfigurationAdminFacade}. The handler will be used to handle
     * configuration file during {@code registerConfigurations}.
     * 
     * @param handler The file handler. This argument must not be {@code null}.
     * 
     * @throws IllegalArgumentException Thrown if the specified {@code handler} is {@code null}.
     * @since 1.0.0
     */
    final void addFileHandler( IConfigurationFileHandler handler )
    throws IllegalArgumentException
    {
        NullArgumentException.validateNotNull( handler, "handler" );

        synchronized( m_handlers )
        {
            m_handlers.add( 0, handler );

            // Reload all configurations just in case if this is added later
            // Only do this though if the config admin service is available. If
            // the config admin service is not currently available, the registerConfigurations
            // call is delayed until the config admin service is available
            if (m_configAdminService != null) {
                try
                {
                    registerConfigurations(null, false);
                } catch( IOException e )
                {
                    String msg = "IOException by either getting the configuration admin or loading the configuration file.";
                    LOGGER.error( msg, e );
                } catch( InvalidSyntaxException e )
                {
                    LOGGER.error( "Invalid syntax. This should not happened.", e );
                }
            }
        }
    }


    /**
     * Registers configuration for OSGi Managed services.
     * 
     * @param configuration if null then all configuration found will be registered.
     * @param overwrite     A {@code boolean} indicator to overwrite the configuration
     * 
     * @throws IOException            Thrown if there is an IO problem during loading of {@code configuration}.
     * @throws InvalidSyntaxException Thrown if there is an invalid exception during retrieval of configurations.
     * @throws IllegalStateException  Thrown if the configuration admin service is not available.
     */
    final void registerConfigurations( String configuration, boolean overwrite )
    throws IOException, InvalidSyntaxException, IllegalStateException
    {
        if( m_configAdminService == null )
        {
            throw new IllegalStateException(
                "Configuration admin service is not available. Please start configuration admin bundle."
            );
        }

        File configDir = getConfigDir();
        if( configDir == null )
        {

            return;
        }

        Configuration[] existingConfigurations;
        synchronized( this )
        {
            existingConfigurations = m_configAdminService.listConfigurations( null );
        }

        Set<String> configCache = new HashSet<String>();
        if( existingConfigurations != null && !overwrite )
        {
            for( Configuration existingConfig : existingConfigurations )
            {
                configCache.add(existingConfig.getPid());
            }
        }

        // Create configuration for ManagedServiceFactory
        createConfiguration(configuration, configDir, configCache, true);
        // Create configuration for ManagedService
        createConfiguration(configuration, configDir, configCache, false);
    }

    private void createConfiguration( String configuration, File configDir, Set<String> configCache, boolean isFactory )
    throws IOException
    {
        File dir;
        if( isFactory )
        {
            dir = new File( configDir, DIRECTORY_NAME_FACTORIES );
        }
        else
        {
            dir = new File( configDir, DIRECTORY_NAME_SERVICES );
        }

        if( !dir.exists() )
        {
            LOGGER.info( "Directory [" + dir + "] does not exist." );
            return;
        }

        String[] files = dir.list();
        for( String configFileName : files )
        {
            createConfigurationForFile( configuration, configFileName, configCache, dir, isFactory );
        }
    }


    private void createConfigurationForFile( String configuration, String configFileName,
        Set<String> configCache, File dir, boolean isFactory )
    throws IOException
    {

        File f = new File( dir, configFileName );
        if( !f.isDirectory() )
        {
            List<IConfigurationFileHandler> handlers;

            synchronized( m_handlers )
            {
                handlers = new ArrayList<IConfigurationFileHandler>( m_handlers );
            }

            // since the configFileName might end with a file type suffix, we have to check
            // all file handlers
            for( IConfigurationFileHandler handler : handlers )
            {
                // check if we have the correct file handler for the file
                if( handler.canHandle( f ) )
                {
                    // get the service PID
                    String servicePid = handler.getServicePID( configFileName );

                    // check if the service is already configured
                    if( configCache.contains( servicePid ) )
                    {
                        return;
                    }

                    // check if the service is the one that should be configured
                    if( (configuration != null) && !servicePid.equals( configuration ) )
                    {
                        return;
                    }

                    // configure the service
                    handle( handler, configFileName, f, isFactory );
                }
            }
        }
    }


    /**
     * Handle the extraction and registration of the configuration into the config service.
     * If a property service.pid exists in the configuration, then that will be used to locate the service instance.
     * To register the service with a service.pid, do something like
     * <pre>
     * Properties filterProp = new Properties();
     * filterProp.put(Constants.SERVICE_PID, "my.test.service.Interface");
     * bundleContext.registerService(ManagedService.class.getName(), myServiceInstance, filterProp);
     * </pre>
     * in your client code that registeres the managed service.
     */
    private void handle( IConfigurationFileHandler handler, String configFile, File file, boolean isFactory )
    throws IOException
    {
        String servicePid = handler.getServicePID( configFile );
        Properties prop = handler.handle(file);

        // Find out if a service.pid property is included, use it if it does
        String str = (String) prop.get(Constants.SERVICE_PID);
        if( str != null )
        {
            servicePid = str;
        }

        synchronized( this )
        {
            if( isFactory )
            {
                m_processor.process( m_configAdminService, servicePid, prop );
            }
            else
            {
                Configuration conf = m_configAdminService.getConfiguration( servicePid, null );
                conf.update((Dictionary)prop);
            }
        }

        LOGGER.info( "Register configuration [" + servicePid + "]" );
    }

    private File getConfigDir()
    {
		String configArea = m_propertyResolver.getProperty( OSGI_CONFIGURATION_AREA );

        // Only run the configuration changes if the configArea is set.
		File dir = null;
		if ( configArea != null )
		{
			try
			{
				dir = FileUtils.toFile( new URL( configArea ) );
			}
			catch ( MalformedURLException e )
			{
				LOGGER.error("Configuration area [" + configArea + "] is not a valid URL.", e );
			}
		} 
		else
		{
			LOGGER.info( "System property [" + OSGI_CONFIGURATION_AREA + "] is not defined. Using fallback." );
			// fallback to previous behavior
			configArea = m_propertyResolver.getProperty( BUNDLES_CONFIGURATION_LOCATION );
        if( configArea == null )
        {
            LOGGER.info( "System property [" + BUNDLES_CONFIGURATION_LOCATION + "] is not defined." );
            LOGGER.info( "Using default configurations location [" + DEFAULT_CONFIGURATION_LOCATION + "]." );
            configArea = DEFAULT_CONFIGURATION_LOCATION;
        }
			dir = new File( configArea );
		}
        if( !dir.exists() )
        {
            String absolutePath = dir.getAbsolutePath();
            LOGGER.error( "Configuration area [" + absolutePath + "] does not exist. Unable to load properties." );
            return null;
        }
		LOGGER.info( "Using configuration from [" + dir.getAbsolutePath() + "]" );
        return dir;
    }


    /**
     * Dispose this {@code ConfigurationAdminFacade} instance. Once this object instance is disposed, it is not meant to
     * be used again.
     */
    void dispose()
    {
        m_configAdminService = null;
        m_handlers.clear();
    }

    final void printConfigFileList( PrintWriter writer, String fileName )
    {
        File configDir = getConfigDir();

        if( configDir == null )
        {
            writer.println("Configuration dir is not setup.");
            return;
        }

        if( fileName != null )
        {
            printConfiguration(writer, fileName, configDir);
            return;
        }

        String configAbsolutePath = configDir.getAbsolutePath();
        writer.println("config dir: [" + configAbsolutePath + "] contains the following config files:");
        String[] files = configDir.list();
        for( String file : files )
        {
            writer.println(file);
        }
    }

    private void printConfiguration( PrintWriter writer, String fileName, File configDir )
    {
        File configFile = new File(configDir, fileName);
        String absolutePath = configFile.getAbsolutePath();
        if( !configFile.canRead() || !configFile.exists() )
        {
            writer.println("Can't read configfile [" + absolutePath + "]");
            return;
        }

        Properties props = new Properties();
        try
        {
            InputStream in = new FileInputStream(configFile);
            props.load(in);
        }
        catch( Exception e )
        {
            String message = "Can't read configfile [" + absolutePath + "] - not a correct config file";
            writer.println(message);
            return;
        }

        writer.println("Config file: [" + absolutePath + "]");
        for( Object keyObject : props.keySet() )
        {
            String key = (String) keyObject;
            String value = props.getProperty(key);
            writer.println(key + " = " + value);
        }
    }


    /**
     * Remove the specified {@code handler} from this {@code ConfigurationAdminFacade}.
     * 
     * @param handler The handler to be removed. This argument must not be {@code null}.
     * 
     * @throws IllegalArgumentException Thrown if the specified {@code handler} is {@code null}.
     */
    final void removeFileHandler( IConfigurationFileHandler handler )
    throws IllegalArgumentException
    {
        NullArgumentException.validateNotNull( handler, "handler" );

        synchronized( m_handlers )
        {
            m_handlers.remove( handler );
        }
    }


    /**
     * Set the configuration admin service. Sets to {@code null} if the configuration admin service is not available.
     * 
     * @param configurationAdminService The configuration admin.
     */
    final void setConfigurationAdminService( ConfigurationAdmin configurationAdminService )
    {
        synchronized( this )
        {
            m_configAdminService = configurationAdminService;
        }
    }

    /**
     * Resolves properties without coupling the facade to specific properties sources as System.getproperty or
     * BundleContext.getproperty.
     */
    static interface PropertyResolver
    {

        /**
         * Returns the value of the specified property.
         * 
         * @param key the name of the requested property
         * 
         * @return the value of the requested property, or null if not available
         */
        String getProperty(String key);

    }
}
