package com.github.mike10004.seleniumhelp;

import com.github.mike10004.chromecookieimplant.ChromeCookieImplanter;
import com.github.mike10004.chromecookieimplant.CookieImplantOutput;
import com.github.mike10004.chromecookieimplant.CookieProcessingStatus;
import com.github.mike10004.seleniumhelp.ChromeWebDriverFactory.CookiePreparer;
import com.github.mike10004.xvfbmanager.XvfbController;
import com.google.common.io.Resources;
import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@org.junit.Ignore("Chrome cookie support is indefinitely disabled as of 0.57")
public class ChromeCookieUsageTest extends CookieUsageTestBase {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @BeforeClass
    public static void setupChromeDriver() {
        ChromeUnitTests.setupRecommendedChromeDriver();
    }

    @Override
    protected WebDriverFactory createCookielessWebDriverFactory(XvfbController xvfbController) {
        return ChromeWebDriverFactory.builder()
                .configure(ChromeUnitTests.createChromeOptions())
                .environment(xvfbController::newEnvironment)
                .acceptInsecureCerts()
                .build();
    }

    @Override
    protected WebDriverFactory createCookiefulWebDriverFactory(XvfbController xvfbController, List<DeserializableCookie> cookiesSetByServer) {
        Duration cookieImplantTimeout = UnitTests.Settings.timeouts().get("chromeCookieImplant", Duration.ofSeconds(10));
        ChromeCookieImplanter implanter = new CustomCookieImplanter(Ints.checkedCast(cookieImplantTimeout.getSeconds()), new Gson());
        CookiePreparer cookiePreparer = new ChromeCookiePreparer(tmp.getRoot().toPath(), () -> cookiesSetByServer, implanter);
        return ChromeWebDriverFactory.builder()
                .configure(ChromeUnitTests.createChromeOptions())
                .environment(xvfbController::newEnvironment)
                .cookiePreparer(cookiePreparer)
                .acceptInsecureCerts()
                .build();
    }

    @Test
    public void testCookieUsage() throws Exception {
        exerciseCookieCapabilities();
    }

    private static class CustomCookieImplanter extends ChromeCookieImplanter {

        private Gson gson;

        public CustomCookieImplanter(int outputTimeoutSeconds, Gson gson) {
            super(Resources.asByteSource(getCrxResourceOrDie()), outputTimeoutSeconds, gson);
            this.gson = gson;
        }

        static final String EXTENSION_RESOURCE_PATH = "/chrome-cookie-implant.crx";

        private static URL getCrxResourceOrDie() throws IllegalStateException {
            URL url = ChromeCookieImplanter.class.getResource(EXTENSION_RESOURCE_PATH);
            if (url == null) {
                throw new IllegalStateException("resource does not exist: classpath:" + EXTENSION_RESOURCE_PATH);
            }
            return url;
        }

        @Override
        protected CookieImplantOutput waitForCookieImplantOutput(WebDriver driver, int timeOutInSeconds) {
            By locator;
            try {
                locator = byOutputStatus(CookieProcessingStatus.all_implants_processed);
            } catch (org.openqa.selenium.TimeoutException e) {
                UnitTests.dumpState(driver, System.err);
                throw e;
            }
            Function<? super WebDriver, WebElement> fn = ExpectedConditions.presenceOfElementLocated(locator);
            WebElement outputElement = new WebDriverWait(driver, timeOutInSeconds)
                    .until(fn);
            String outputJson = outputElement.getText();
            CookieImplantOutput output = gson.fromJson(outputJson, CookieImplantOutput.class);
            return output;
        }

        protected By byOutputStatus(CookieProcessingStatus requiredStatus) {
            return byOutputStatus(new Predicate<CookieProcessingStatus>() {
                @Override
                public boolean test(CookieProcessingStatus cookieProcessingStatus) {
                    return requiredStatus == cookieProcessingStatus;
                }
                @Override
                public String toString() {
                    return String.format("EqualsPredicate{%s}", requiredStatus);
                }
            });
        }

        private static class ImplantOutputPredicate implements Predicate<CookieImplantOutput> {

            private final Predicate<CookieProcessingStatus> statusPredicate;

            public ImplantOutputPredicate(Predicate<CookieProcessingStatus> statusPredicate) {
                this.statusPredicate = statusPredicate;
            }


            @Override
            public boolean test(CookieImplantOutput cookieImplantOutput) {
                return statusPredicate.test(cookieImplantOutput.status);
            }

            @Override
            public String toString() {
                return String.format("CookieImplantOutputPredicate{status:%s}", statusPredicate);
            }
        }

        @Override
        protected By byOutputStatus(Predicate<CookieProcessingStatus> statusPredicate) {
            return elementTextRepresentsObject(By.cssSelector("#output"), CookieImplantOutput.class, new ImplantOutputPredicate(statusPredicate));
        }

        @Override
        protected <T> By elementTextRepresentsObject(By elementLocator, Class<T> deserializedType, Predicate<? super T> predicate) {
            return new By() {
                @Override
                public List<WebElement> findElements(SearchContext context) {
                    List<WebElement> parents = elementLocator.findElements(context);
                    List<WebElement> filteredElements = new ArrayList<>(parents.size());
                    for (WebElement parent : parents) {
                        String json = parent.getText();
                        T item = gson.fromJson(json, deserializedType);
                        if (predicate.test(item)) {
                            filteredElements.add(parent);
                        }
                    }
                    return filteredElements;
                }

                @Override
                public String toString() {
                    return String.format("ElementTextRepresentsObject{superset=%s,type=%s,predicate=%s}", elementLocator, deserializedType, predicate);
                }
            };
        }

    }

}
