package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import io.github.mike10004.seleniumcapture.testbases.WebDriverTestParameter;
import io.github.mike10004.seleniumcapture.testing.UnitTests;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.net.URL;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

public class BysTest {

    @ClassRule
    public static XvfbRule xvfb = UnitTests.xvfbRuleBuilder()
            .build();

    @BeforeClass
    public static void setUpClass() throws Exception {
        WebDriverTestParameter ffp = new FirefoxTestParameter();
        ffp.doDriverManagerSetup();
        webdrivingSession = ffp.createWebDriverFactory(xvfb).startWebdriving(WebdrivingConfig.nonCapturing());
        System.out.println("setUpClass: " +webdrivingSession);
    }

    private static WebdrivingSession webdrivingSession;

    @AfterClass
    public static void killDriver() {
        if (webdrivingSession != null) {
            WebDriver driver = webdrivingSession.getWebDriver();
            driver.quit();
        }
    }

    @Test
    public void conjoin_tagNameOnly() throws Exception {
        By b1 = By.tagName("h1");
        testConjoin(ImmutableList.of(b1), 3);
    }

    @Test
    public void conjoin_tagNameAndCss() throws Exception {
        By b1 = By.tagName("h1"), b2 = By.cssSelector(".foo");
        testConjoin(ImmutableList.of(b1, b2), 1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void conjoin_zeroConditions() throws Exception {
        Bys.conjoin(ImmutableList.of());
    }

    @Test
    public void conjoin_starCssAndName() throws Exception {
        By b1 = By.cssSelector("*"), b2 = By.name("mars");
        testConjoin(ImmutableList.of(b1, b2), 1);
    }

    private void testConjoin(Iterable<By> bys, int numExpectedElements) throws WebDriverException {
        testFindElements(Bys.conjoin(bys), numExpectedElements);
    }

    private void testFindElements(By by, int numExpectedElements) {
        URL url = requireNonNull(getClass().getResource("/BysTest-1.html"));
        WebDriver driver = webdrivingSession.getWebDriver();
        driver.get(url.toString());
        List<WebElement> found = by.findElements(driver);
        assertEquals("num elements found", numExpectedElements, found.size());
    }

    @Test
    public void attribute() throws Exception {
        By by = Bys.attribute(By.tagName("a"), "href", Predicates.containsPattern("www\\.nasa\\.gov"));
        testFindElements(by, 1);
    }

    @Test
    public void textFuzzy() throws Exception {
        By by = Bys.elementWithTextFuzzy(By.cssSelector("p.venus"), "radiusofmiles");
        testFindElements(by, 1);
    }

    @Test
    public void textEquals() throws Exception {
        By by = Bys.elementWithText(By.cssSelector("h1"), "Venus");
        testFindElements(by, 1);
    }
}