package io.github.mike10004.seleniumcapture.chrome;

import com.browserup.harreader.model.HarContent;
import com.browserup.harreader.model.HarResponse;
import io.github.mike10004.seleniumcapture.testbases.CollectionTestBase;
import com.github.mike10004.seleniumhelp.WebDriverFactory;
import org.junit.Assume;
import org.junit.Test;

import java.net.URL;

public class ChromeHttpCollectionTest extends CollectionTestBase {

    public ChromeHttpCollectionTest() {
        super(new ChromeTestParameter(true), "http");
    }

    @Test
    public void http() throws Exception {
        testhttp(false);
    }

    @Override
    protected boolean isHeadlessTestDisabled() {
        return ChromeUnitTests.isHeadlessChromeTestsDisabled();
    }

    @Test
    public void http_headless() throws Exception {
        testhttp(true);
    }

    private void testhttp(boolean headless) throws Exception {
        Assume.assumeFalse("headless tests disabled", headless && isHeadlessTestDisabled());
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .configure(ChromeUnitTests.createChromeOptions())
                .configure(o -> o.setHeadless(headless))
                .environment(createEnvironmentSupplierForDisplay(headless))
                .build();
        testTrafficCollectorOnExampleDotCom(webDriverFactory);
    }

    @Test
    public void http_headless_brotli() throws Exception {
        Assume.assumeFalse("headless tests disabled", isHeadlessTestDisabled());
        WebDriverFactory webDriverFactory = ChromeWebDriverFactory.builder()
                .configure(ChromeUnitTests.createChromeOptions())
                .configure(o -> o.setHeadless(true))
                .build();
        HarResponse response = testTrafficCollector(webDriverFactory, new URL("http://httpbin.org/brotli"));
        HarContent content = response.getContent();
        System.out.println(content.getText());
    }
}
