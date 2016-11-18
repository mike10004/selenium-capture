/*
 * (c) 2016 Novetta
 *
 * Created by mike
 */
package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbselenium.WebDriverSupport;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;

public class ChromeWebDriverFactory extends OptionalDisplayWebDriverFactory {

    public ChromeWebDriverFactory() {
    }

    public ChromeWebDriverFactory(String display) {
        super(display);
    }

    @Override
    public WebDriver createWebDriver(BrowserMobProxy proxy, CertificateAndKeySource certificateAndKeySource) throws IOException {
        org.openqa.selenium.Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
        ChromeDriver driver = WebDriverSupport.chromeInEnvironment(buildEnvironment()).create(capabilities);
        return driver;
    }
}
