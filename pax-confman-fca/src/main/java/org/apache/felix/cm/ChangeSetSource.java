package org.apache.felix.cm;

/**
 * Created by IntelliJ IDEA.
 * User: alindreghiciu
 * Date: Feb 21, 2008
 * Time: 3:37:07 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ChangeSetSource
{

    void addListener( ChangeSetListener listener );

    void removeListener( ChangeSetListener listener );

}
