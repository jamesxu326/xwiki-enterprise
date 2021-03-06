/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.test.cluster;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.runner.RunWith;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.XWikiExecutorSuite;

import ch.qos.logback.classic.jmx.JMXConfiguratorMBean;

/**
 * Runs all functional tests found in the classpath and start/stop XWiki before/after the tests (only once).
 *
 * @version $Id$
 */
@RunWith(XWikiExecutorSuite.class)
@XWikiExecutorSuite.Executors(2)
public class AllTests
{
    private static final String WEBINF_PATH = "/observation/remote/jgroups";

    @XWikiExecutorSuite.PreStart
    public void preInitialize(List<XWikiExecutor> executors) throws Exception
    {
        initChannel(executors.get(0), "tcp1");
        initChannel(executors.get(1), "tcp2");
    }

    @XWikiExecutorSuite.PostStart
    public void postInitialize(List<XWikiExecutor> executors) throws Exception
    {
        // Now that the server is started, connect to it remotely using JMX/RMI to set the log levels for some
        // cluster-related XWiki classes in order to get more information in the logs for easier debugging.
        for (XWikiExecutor executor : executors) {
            JMXServiceURL url = new JMXServiceURL(
                "service:jmx:rmi:///jndi/rmi://:" + executor.getRMIPort() + "/jmxrmi");
            JMXConnector jmxc = JMXConnectorFactory.connect(url);
            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
            ObjectName oname = new ObjectName("logback:type=xwiki");
            JMXConfiguratorMBean proxy = JMX.newMBeanProxy(mbsc, oname, JMXConfiguratorMBean.class);
            proxy.setLoggerLevel("org.xwiki.observation.remote", "debug");
            proxy.setLoggerLevel("com.xpn.xwiki.internal", "debug");
        }
    }

    private void initChannel(XWikiExecutor executor, String channelName) throws Exception
    {
        Properties properties = executor.loadXWikiProperties();
        properties.setProperty("observation.remote.enabled", "true");
        properties.setProperty("observation.remote.channels", channelName);
        executor.saveXWikiProperties(properties);

        String filename = channelName + ".xml";

        InputStream is = getClass().getResourceAsStream("/" + filename);
        try {
            FileOutputStream fos = new FileOutputStream(executor.getWebInfDirectory() + WEBINF_PATH + "/" + filename);

            byte[] buffer = new byte[1024];

            for (int nb; (nb = is.read(buffer)) > 0;) {
                fos.write(buffer, 0, nb);
            }
        } finally {
            is.close();
        }
    }
}
