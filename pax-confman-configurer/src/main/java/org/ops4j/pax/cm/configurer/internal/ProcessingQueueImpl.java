package org.ops4j.pax.cm.configurer.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.cm.ConfigurationAdmin;
import org.ops4j.lang.NullArgumentException;

/**
 * Created by IntelliJ IDEA.
 * User: alindreghiciu
 * Date: Feb 12, 2008
 * Time: 1:21:07 PM
 * To change this template use File | Settings | File Templates.
 */
class ProcessingQueueImpl
    implements ProcessingQueue
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( ProcessingQueueImpl.class );
    /**
     * Queue of items to be processed.
     */
    private final List<Processable> m_processables;
    /**
     * In use configuration admin. Can be null when no configuration admin service is present.
     */
    private ConfigurationAdmin m_configurationAdmin;
    /**
     * Processable items processor thread.
     */
    private Runnable m_processor;
    /**
     * Lock for processing thread.
     */
    private final Lock m_lock;

    /**
     * Creates a new processing queue.
     */
    ProcessingQueueImpl()
    {
        m_processables = Collections.synchronizedList( new ArrayList<Processable>() );
        m_lock = new ReentrantLock();
    }

    /**
     * Setter.
     *
     * @param service configuration admin service to be used. Can be null, case when processing will stop.
     */
    public synchronized void setConfigurationAdmin( final ConfigurationAdmin service )
    {
        LOG.trace( "Configuration admin in use: " + service );
        m_configurationAdmin = service;
        if( m_configurationAdmin != null )
        {
            maybeStartProcessor();
        }
    }

    /**
     * Getter.
     *
     * @return current configuration admin
     */
    private synchronized ConfigurationAdmin getConfigurationAdmin()
    {
        return m_configurationAdmin;
    }

    /**
     * Adds a processing tem to the queue.
     *
     * @param processable to be added. Cannot be null.
     *
     * @throws NullArgumentException if processable item is null
     */
    public void add( final Processable processable )
    {
        LOG.trace( "Added processable item to the queue: " + processable );
        NullArgumentException.validateNotNull( processable, "Processable item" );

        m_processables.add( processable );
        maybeStartProcessor();
    }

    /**
     * Startes a processing thread if not already started.
     */
    private void maybeStartProcessor()
    {
        try
        {
            m_lock.lock();
            if( m_processor == null )
            {
                m_processor = new Runnable()
                {

                    public void run()
                    {
                        LOG.debug( "Start processing of configuration items" );
                        // processing loop
                        try
                        {
                            ConfigurationAdmin configurationAdmin;
                            Processable processable;
                            while( ( configurationAdmin = getConfigurationAdmin() ) != null
                                   && ( processable = m_processables.get( 0 ) ) != null )
                            {
                                LOG.debug( "Processing: " + processable );
                                try
                                {
                                    processable.process( configurationAdmin );
                                }
                                catch( Throwable ignore )
                                {
                                    LOG.error( "Ignored exception during processing", ignore );
                                }
                                m_processables.remove( processable );
                            }
                        }
                        catch( IndexOutOfBoundsException ignore )
                        {
                            // there are no items to be processed
                            LOG.trace( "There are no items to be processed" );
                        }
                        // reset processor
                        try
                        {
                            m_lock.lock();
                            m_processor = null;
                        }
                        finally
                        {
                            m_lock.unlock();
                        }
                        LOG.debug( "End of processing of configuration items" );
                    }

                };
                new Thread( m_processor ).start();
            }
        }
        finally
        {
            m_lock.unlock();
        }
    }

}
