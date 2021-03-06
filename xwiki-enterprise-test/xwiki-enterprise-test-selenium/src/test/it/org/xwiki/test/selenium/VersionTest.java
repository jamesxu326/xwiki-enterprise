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
package org.xwiki.test.selenium;

import org.xwiki.test.selenium.framework.AbstractXWikiTestCase;
import org.xwiki.test.selenium.framework.ColibriSkinExecutor;
import org.xwiki.test.selenium.framework.XWikiTestSuite;

import junit.framework.Test;

/**
 * Verify versioning features of documents and attachments.
 *
 * @version $Id$
 */
public class VersionTest extends AbstractXWikiTestCase
{
    public static Test suite()
    {
        XWikiTestSuite suite =
            new XWikiTestSuite("Verify versioning features of documents and attachments");
        suite.addTestSuite(VersionTest.class, ColibriSkinExecutor.class);
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        loginAsAdmin();
    }

    /**
     * Verify we can rollback to the first version of a document that is bundled in the default
     * distribution.
     */ 
    public void testRollbackToFirstVersion() throws Exception
    {
        open("Main", "VersionTest", "edit", "editor=wiki");
        setFieldValue("content", "aaa");
        clickEditSaveAndView();
        open("Main", "VersionTest", "edit", "editor=wiki");
        setFieldValue("content", "bbb");
        clickEditSaveAndView();
        open("Main", "VersionTest", "rollback", "rev=1.1");
        clickLinkWithLocator("//input[@value='yes']");
        assertTextPresent("aaa");
    }
}
