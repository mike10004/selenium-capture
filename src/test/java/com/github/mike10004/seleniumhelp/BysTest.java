package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BysTest {

    @ClassRule
    public static final XvfbRule xvfb = XvfbRule.builder().build();

    @ClassRule
    public static WebDriverManagerRule chromedriverSetupRule = WebDriverManagerRule.chromedriver();

    @BeforeClass
    public static void setUpClass() throws Exception {
        ChromeOptions options = UnitTests.createChromeOptions();
        ChromeDriverService driverService = new ChromeDriverService.Builder()
                .withEnvironment(xvfb.getController().newEnvironment())
                .build();
        driver = new ChromeDriver(driverService, options);
    }

    private static WebDriver driver;

    @AfterClass
    public static void killDriver() {
        if (driver != null) {
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
        URL url = getClass().getResource("/BysTest-1.html");
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