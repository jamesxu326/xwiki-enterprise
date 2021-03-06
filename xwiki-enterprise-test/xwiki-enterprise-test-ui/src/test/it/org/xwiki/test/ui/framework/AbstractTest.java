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
package org.xwiki.test.ui.framework;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.openqa.selenium.WebDriver;
import org.xwiki.test.ui.framework.elements.BaseElement;

/**
 * To be extended by all Test Classes. Allows to start/stop the Web Driver and get access to it.
 * 
 * @version $Id$
 * @since 2.3M1
 */
public class AbstractTest
{
    protected static PersistentTestContext context;

    /** Used so that AllTests can set the persistent test context. */
    public static void setContext(PersistentTestContext context)
    {
        AbstractTest.context = context;
        BaseElement.setContext(context);
        TestUtils.setContext(context);
    }

    @BeforeClass
    public static void init() throws Exception
    {
        // This will not be null if we are in the middle of allTests
        if (context == null) {
            setContext(new PersistentTestContext());
        }
    }

    @AfterClass
    public static void shutdown() throws Exception
    {
        context.shutdown();
    }

    protected WebDriver getDriver()
    {
        return context.getDriver();
    }

    /**
     * @return Utility class with functions not specific to any test or element.
     */
    protected TestUtils getUtil()
    {
        return context.getUtil();
    }
}
