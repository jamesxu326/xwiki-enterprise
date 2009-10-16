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
package com.xpn.xwiki.it.selenium.framework;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.codehaus.plexus.util.StringInputStream;

import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.Wait;

/**
 * All XWiki Selenium tests must extend this class.
 * 
 * @version $Id$
 */
public abstract class AbstractXWikiTestCase extends TestCase implements SkinExecutor
{
    public static final String BASEDIR = System.getProperty("basedir");

    public static final String DOC = "selenium.browserbot.getCurrentWindow().document.";

    private static final int WAIT_TIME = 30000;

    private SkinExecutor skinExecutor;

    private Selenium selenium;

    public void setSkinExecutor(SkinExecutor skinExecutor)
    {
        this.skinExecutor = skinExecutor;
    }

    public SkinExecutor getSkinExecutor()
    {
        if (this.skinExecutor == null) {
            throw new RuntimeException("Skin executor hasn't been initialized. Make sure to wrap " + "your test in a "
                + XWikiTestSuite.class.getName() + " class and call "
                + " addTestSuite(Class testClass, SkinExecutor skinExecutor).");
        }
        return this.skinExecutor;
    }

    public void setSelenium(Selenium selenium)
    {
        this.selenium = selenium;
    }

    public Selenium getSelenium()
    {
        return this.selenium;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        // Print test name for easier parsing of Selenium logs
        System.out.println("Test: " + getName());
    }

    /**
     * Capture test failures in order to output the HTML for easier debugging + take screenshot.
     */
    @Override
    public void runBare() throws Throwable
    {
        Throwable exception= null;
        setUp();
        try {
            runTest();
        } catch (Throwable running) {
            exception = running;
            // Take screenshot before the tear down to ensure we take a picture of the real problem.
            takeScreenShot();
        } finally {
            try {
                tearDown();
            } catch (Throwable tearingDown) {
                if (exception == null) exception= tearingDown;
            }
        }
        if (exception != null) throw exception;
    }

    private void takeScreenShot() throws Throwable
    {
        try {
            // Selenium method execution results are logged automatically by Selenium so just calling getHtmlSource
            // is enough to have it in the logs.
            getSelenium().getHtmlSource();

            // Create directory where to store screenshots
            String screenshotDir = BASEDIR;
            if (!screenshotDir.endsWith(System.getProperty("file.separator"))) {
                screenshotDir = screenshotDir + System.getProperty("file.separator");
            }
            screenshotDir = screenshotDir + "target" + System.getProperty("file.separator")
                + "selenium-screenshots" + System.getProperty("file.separator");
            new File(screenshotDir).mkdirs();

            // Capture screenshot
            getSelenium().captureEntirePageScreenshot(screenshotDir + this.getClass().getName() + "." + getName()
                + ".png", "background=#FFFFFF");
        } catch (Throwable error2) {
            // Don't throw any exception generated by the debugging steps
            error2.printStackTrace();
        }
    }

    // Convenience methods wrapping Selenium

    public void open(String url)
    {
        getSelenium().open(url);
    }

    public void open(String space, String page)
    {
        open(getUrl(space, page));
    }

    public void open(String space, String page, String action)
    {
        open(getUrl(space, page, action));
    }

    public void open(String space, String page, String action, String queryString)
    {
        open(getUrl(space, page, action, queryString));
    }

    public String getTitle()
    {
        return getSelenium().getTitle();
    }

    public void assertPage(String space, String page)
    {
        assertTrue(getTitle().matches(".*\\(" + space + "." + page + "\\) - XWiki"));
    }

    public boolean isExistingPage(String space, String page)
    {
        String saveUrl = getSelenium().getLocation();

        open(getUrl(space, page));
        boolean exists = !getSelenium().isTextPresent("The requested document could not be found.");

        // Restore original URL
        open(saveUrl);

        return exists;
    }

    public void assertTitle(String title)
    {
        assertEquals(title, getTitle());
    }

    public boolean isElementPresent(String locator)
    {
        return getSelenium().isElementPresent(locator);
    }

    public boolean isLinkPresent(String text)
    {
        return isElementPresent("link=" + text);
    }

    public void clickLinkWithText(String text)
    {
        clickLinkWithText(text, true);
    }

    public void assertTextPresent(String text)
    {
        assertTrue("[" + text + "] isn't present.", getSelenium().isTextPresent(text));
    }

    public void assertTextNotPresent(String text)
    {
        assertFalse("[" + text + "] is present.", getSelenium().isTextPresent(text));
    }

    public void assertElementPresent(String elementLocator)
    {
        assertTrue("[" + elementLocator + "] isn't present.", isElementPresent(elementLocator));
    }

    public void assertElementNotPresent(String elementLocator)
    {
        assertFalse("[" + elementLocator + "] is present.", isElementPresent(elementLocator));
    }

    public void waitPage()
    {
        waitPage(WAIT_TIME);
    }

    /**
     * @deprecated use {@link #waitPage()} instead
     */
    @Deprecated
    public void waitPage(int nbMillisecond)
    {
        getSelenium().waitForPageToLoad(String.valueOf(nbMillisecond));
    }

    public void createPage(String space, String page, String content)
    {
        createPage(space, page, content, null);
    }

    public void createPage(String space, String page, String content, String syntax)
    {
        // If the page already exists, delete it first
        deletePage(space, page);
        if (syntax == null) {
            editInWikiEditor(space, page);
        } else {
            editInWikiEditor(space, page, syntax);
        }
        setFieldValue("content", content);
        clickEditSaveAndView();
    }

    public void deletePage(String space, String page)
    {
        open(space, page, "delete", "confirm=1");
    }

    public void restorePage(String space, String page)
    {
        open(space, page, "view");
        if (getSelenium().isTextPresent("Restore")) {
            clickLinkWithText("Restore", true);
        }
    }

    public void clickLinkWithLocator(String locator)
    {
        clickLinkWithLocator(locator, true);
    }

    public void clickLinkWithLocator(String locator, boolean wait)
    {
        assertElementPresent(locator);
        getSelenium().click(locator);
        if (wait) {
            waitPage();
        }
    }

    public void clickLinkWithText(String text, boolean wait)
    {
        clickLinkWithLocator("link=" + text, wait);
    }

    public boolean isChecked(String locator)
    {
        return getSelenium().isChecked(locator);
    }

    public String getFieldValue(String fieldName)
    {
        // Note: We could use getSelenium().getvalue() here. However getValue() is stripping spaces
        // and some of our tests verify that there are leading spaces/empty lines.
        return getSelenium().getEval(
            "selenium.browserbot.getCurrentWindow().document.getElementById(\"" + fieldName + "\").value");
    }

    public void setFieldValue(String fieldName, String value)
    {
        getSelenium().type(fieldName, value);
    }

    public void checkField(String locator)
    {
        getSelenium().check(locator);
    }

    public void submit()
    {
        clickLinkWithXPath("//input[@type='submit']");
    }

    public void submit(String locator)
    {
        clickLinkWithLocator(locator);
    }

    public void submit(String locator, boolean wait)
    {
        clickLinkWithLocator(locator, wait);
    }

    public void clickLinkWithXPath(String xpath)
    {
        clickLinkWithXPath(xpath, true);
    }

    public void clickLinkWithXPath(String xpath, boolean wait)
    {
        clickLinkWithLocator("xpath=" + xpath, wait);
    }

    public void waitForCondition(String condition)
    {
        getSelenium().waitForCondition(condition, "" + WAIT_TIME);
    }

    public void waitForTextPresent(final String elementLocator, final String expectedValue)
    {
        new Wait()
        {
            public boolean until()
            {
                return getSelenium().getText(elementLocator).equals(expectedValue);
            }
        }.wait("element [" + elementLocator + "] not found or doesn't have the value [" + expectedValue + "]");
    }

    public void waitForTextContains(final String elementLocator, final String containsValue)
    {
        new Wait()
        {
            public boolean until()
            {
                return getSelenium().getText(elementLocator).indexOf(containsValue) > -1;
            }
        }.wait("element [" + elementLocator + "] not found or doesn't contain the value [" + containsValue + "]");
    }

    public void waitForBodyContains(final String containsValue)
    {
        new Wait()
        {
            public boolean until()
            {
                return getSelenium().getBodyText().indexOf(containsValue) > -1;
            }
        }.wait("Body text doesn't contain the value [" + containsValue + "]");
    }

    public void waitForElement(final String elementLocator)
    {
        new Wait()
        {
            public boolean until()
            {
                return getSelenium().isElementPresent(elementLocator);
            }
        }.wait("element [" + elementLocator + "] not found");
    }

    /**
     * {@inheritDoc}
     * 
     * @see SkinExecutor#clickEditPage()
     */
    public void clickEditPage()
    {
        getSkinExecutor().clickEditPage();
    }

    public void clickDeletePage()
    {
        getSkinExecutor().clickDeletePage();
    }

    public void clickCopyPage()
    {
        getSkinExecutor().clickCopyPage();
    }

    public void clickShowComments()
    {
        getSkinExecutor().clickShowComments();
    }

    public void clickShowAttachments()
    {
        getSkinExecutor().clickShowAttachments();
    }

    public void clickShowHistory()
    {
        getSkinExecutor().clickShowHistory();
    }

    public void clickShowInformation()
    {
        getSkinExecutor().clickShowInformation();
    }

    public void clickEditPreview()
    {
        getSkinExecutor().clickEditPreview();
    }

    public void clickEditSaveAndContinue()
    {
        getSkinExecutor().clickEditSaveAndContinue();
    }

    public void clickEditCancelEdition()
    {
        getSkinExecutor().clickEditCancelEdition();
    }

    public void clickEditSaveAndView()
    {
        getSkinExecutor().clickEditSaveAndView();
    }

    public boolean isAuthenticated()
    {
        return getSkinExecutor().isAuthenticated();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAuthenticated(String username)
    {
        return getSkinExecutor().isAuthenticated(username);
    }
    
    public boolean isAuthenticationMenuPresent()
    {
        return getSkinExecutor().isAuthenticationMenuPresent();
    }

    public void logout()
    {
        getSkinExecutor().logout();
    }

    public void login(String username, String password, boolean rememberme)
    {
        getSkinExecutor().login(username, password, rememberme);
    }

    public void loginAsAdmin()
    {
        getSkinExecutor().loginAsAdmin();
    }

    /**
     * If the user is not logged in already and if the specified user page exists, it is logged in. Otherwise the user
     * is registered first and then the login is executed.
     * 
     * @param username the user name to login as. If the user is to be created, this will also be used as the user first
     *            name while the user last name will be left blank
     * @param password the password of the user
     * @param rememberMe whether the login should be remembered or not
     */
    public void loginAndRegisterUser(String username, String password, boolean rememberMe)
    {   
        if (!isAuthenticationMenuPresent()) {
            // navigate to the main page
            open("Main", "WebHome");
        }
        
        // if user is already authenticated, don't login
        if (isAuthenticated(username)) {
            return;
        }        

        // try to go to the user page 
        open("XWiki", username);
        // if user page doesn't exist, register the user first
        boolean exists = !getSelenium().isTextPresent("The requested document could not be found.");
        if (!exists) {
            if (isAuthenticated()) {
                logout();
            }
            clickRegister();
            fillRegisterForm(username, "", username, password, "");
            submit();
            // assume registration was done successfully, otherwise the register test should fail too
        }

        login(username, password, rememberMe);
    }

    public void fillRegisterForm(String firstName, String lastName, String username, String password, String email)
    {
        setFieldValue("register_first_name", firstName);
        setFieldValue("register_last_name", lastName);
        setFieldValue("xwikiname", username);
        setFieldValue("register_password", password);
        setFieldValue("register2_password", password);
        setFieldValue("register_email", email);
    }

    public void clickLogin()
    {
        getSkinExecutor().clickLogin();
    }

    public void clickRegister()
    {
        getSkinExecutor().clickRegister();
    }

    public String getEditorSyntax()
    {
        return getSkinExecutor().getEditorSyntax();
    }

    public void setEditorSyntax(String syntax)
    {
        getSkinExecutor().setEditorSyntax(syntax);
    }

    public void editInWikiEditor(String space, String page)
    {
        getSkinExecutor().editInWikiEditor(space, page);
    }

    public void editInWikiEditor(String space, String page, String syntax)
    {
        getSkinExecutor().editInWikiEditor(space, page, syntax);
    }

    public void editInWysiwyg(String space, String page)
    {
        getSkinExecutor().editInWysiwyg(space, page);
    }

    public void editInWysiwyg(String space, String page, String syntax)
    {
        getSkinExecutor().editInWysiwyg(space, page, syntax);
    }

    public void clearWysiwygContent()
    {
        getSkinExecutor().clearWysiwygContent();
    }

    public void keyPressAndWait(String element, String keycode) throws InterruptedException
    {
        getSelenium().keyPress(element, keycode);
        waitPage();
    }

    public void typeInWysiwyg(String text)
    {
        getSkinExecutor().typeInWysiwyg(text);
    }

    public void typeInWiki(String text)
    {
        getSkinExecutor().typeInWiki(text);
    }

    public void typeEnterInWysiwyg()
    {
        getSkinExecutor().typeEnterInWysiwyg();
    }

    public void typeShiftEnterInWysiwyg()
    {
        getSkinExecutor().typeShiftEnterInWysiwyg();
    }

    public void clickWysiwygUnorderedListButton()
    {
        getSkinExecutor().clickWysiwygUnorderedListButton();
    }

    public void clickWysiwygOrderedListButton()
    {
        getSkinExecutor().clickWysiwygOrderedListButton();
    }

    public void clickWysiwygIndentButton()
    {
        getSkinExecutor().clickWysiwygIndentButton();
    }

    public void clickWysiwygOutdentButton()
    {
        getSkinExecutor().clickWysiwygOutdentButton();
    }

    public void clickWikiBoldButton()
    {
        getSkinExecutor().clickWikiBoldButton();
    }

    public void clickWikiItalicsButton()
    {
        getSkinExecutor().clickWikiItalicsButton();
    }

    public void clickWikiUnderlineButton()
    {
        getSkinExecutor().clickWikiUnderlineButton();
    }

    public void clickWikiLinkButton()
    {
        getSkinExecutor().clickWikiLinkButton();
    }

    public void clickWikiHRButton()
    {
        getSkinExecutor().clickWikiHRButton();
    }

    public void clickWikiImageButton()
    {
        getSkinExecutor().clickWikiImageButton();
    }

    public void clickWikiSignatureButton()
    {
        getSkinExecutor().clickWikiSignatureButton();
    }

    public void assertWikiTextGeneratedByWysiwyg(String text)
    {
        getSkinExecutor().assertWikiTextGeneratedByWysiwyg(text);
    }

    public void assertHTMLGeneratedByWysiwyg(String xpath) throws Exception
    {
        getSkinExecutor().assertHTMLGeneratedByWysiwyg(xpath);
    }

    public void assertGeneratedHTML(String xpath) throws Exception
    {
        getSkinExecutor().assertGeneratedHTML(xpath);
    }

    public void openAdministrationPage()
    {
        getSkinExecutor().openAdministrationPage();
    }

    public void openAdministrationSection(String section)
    {
        getSkinExecutor().openAdministrationSection(section);
    }

    public String getUrl(String space, String doc)
    {
        return getUrl(space, doc, "view");
    }

    public String getUrl(String space, String doc, String action)
    {
        return "/xwiki/bin/" + action + "/" + space + "/" + doc;
    }

    public String getUrl(String space, String doc, String action, String queryString)
    {
        return getUrl(space, doc, action) + "?" + queryString;
    }

    public void pressKeyboardShortcut(String shortcut, boolean withCtrlModifier, boolean withAltModifier,
        boolean withShiftModifier) throws InterruptedException
    {
        getSkinExecutor().pressKeyboardShortcut(shortcut, withCtrlModifier, withAltModifier, withShiftModifier);
    }

    /**
     * Set global xwiki configuration options (as if the xwiki.cfg file had been modified). This is useful for testing
     * configuration options.
     * 
     * @param configuration the configuration in {@link Properties} format. For example "param1=value2\nparam2=value2"
     * @throws IOException if an error occurs while parsing the configuration
     */
    public void setXWikiConfiguration(String configuration) throws IOException
    {
        Properties properties = new Properties();
        properties.load(new StringInputStream(configuration));
        StringBuffer sb = new StringBuffer();

        // Since we don't have access to the XWiki object from Selenium tests and since we don't want to restart XWiki
        // with a different xwiki.cfg file for each test that requires a configuration change, we use the following
        // trick: We create a document and we access the XWiki object with a Velocity script inside that document.
        for (Entry<Object, Object> param : properties.entrySet()) {
            sb.append("$xwiki.xWiki.config.setProperty('").append(param.getKey()).append("', '").append(
                param.getValue()).append("')").append('\n');
        }
        editInWikiEditor("Test", "XWikiConfigurationPageForTest", "xwiki/1.0");
        setFieldValue("content", sb.toString());
        // We can execute the script in preview mode. Thus we don't need to save the document.
        clickEditPreview();
    }
}
