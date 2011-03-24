package org.ops4j.pax.exam.rbc.client.intern;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.rbc.client.RemoteBundleContextClient;

/**
 *
 */
public class RetryRemoteBundleContextClient implements RemoteBundleContextClient {

    private static final Logger LOG = LoggerFactory.getLogger( RetryRemoteBundleContextClient.class );

    final private RemoteBundleContextClient m_proxy;

    final private int m_maxRetry;

    public RetryRemoteBundleContextClient( final RemoteBundleContextClient client, int maxRetries )
    {
        m_maxRetry = maxRetries;

        m_proxy = (RemoteBundleContextClient) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{ RemoteBundleContextClient.class },
            new InvocationHandler() {

                public Object invoke( Object o, Method method, Object[] objects )
                    throws Throwable
                {
                    Object ret = null;
                    Exception lastError = null;
                    // invoke x times or fail.
                    boolean retry = false;
                    int triedTimes = 0;
                    do {
                        try {
                            LOG.info( "Call RBC." + method.getName() + " (retries: " + triedTimes + ")" );
                            triedTimes++;
                            if( retry ) { Thread.sleep( 300 ); }
                            ret = method.invoke( client, objects );
                            retry = false;
                        } catch( Exception ex ) {
                            lastError = ex;

                            if( ex instanceof NoSuchObjectException ) {
                                LOG.info( "Catched NoSuchObjectException in RBC." + method.getName() );

                                retry = true;
                            }
                            else {
                                LOG.warn( "Exception that does not cause Retry: " + ex.getClass().getName() + " in RBC." + method.getName(), ex );
                                // just escape
                                throw new Exception( lastError );
                            }
                        }
                    } while( retry && m_maxRetry > triedTimes );
                    // check if we need to throw an exception:

                    if( ( retry ) && ( lastError != null ) ) {
                        throw new Exception( lastError );
                    }
                    LOG.info( "Return RBC." + method.getName() + " with: " + ret );

                    return ret;
                }
            }
        );
    }

    public long install( InputStream stream )
    {
        return m_proxy.install( stream );
    }

    public void cleanup()
    {
        m_proxy.cleanup();
    }

    public void setBundleStartLevel( long bundleId, int startLevel )
    {
        m_proxy.setBundleStartLevel( bundleId, startLevel );
    }

    public void start()
    {
        m_proxy.start();
    }

    public void stop()
    {
        m_proxy.stop();
    }

    public void waitForState( long bundleId, int state, long timeoutInMillis )
        throws BundleException, RemoteException
    {
        m_proxy.waitForState( bundleId, state, timeoutInMillis );
    }

    public void call( TestAddress address )
        throws InvocationTargetException, ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        m_proxy.call( address );
    }
}