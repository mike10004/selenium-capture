package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbselenium.WebDriverSupport;
import com.google.common.collect.ImmutableMap;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public class ChromeWebDriverFactory extends EnvironmentWebDriverFactory {

    private final ChromeOptions chromeOptions;
    private final Capabilities capabilitiesOverrides;

    public ChromeWebDriverFactory(Map<String, String> environment, ChromeOptions chromeOptions, Capabilities capabilitiesOverrides) {
        super(environment);
        this.chromeOptions = checkNotNull(chromeOptions);
        this.capabilitiesOverrides = checkNotNull(capabilitiesOverrides);
    }

    public ChromeWebDriverFactory() {
        this(ImmutableMap.of(), new ChromeOptions(), new DesiredCapabilities());
    }

    public ChromeWebDriverFactory(Supplier<Map<String, String>> environmentSupplier, ChromeOptions chromeOptions, Capabilities capabilitiesOverrides) {
        super(environmentSupplier);
        this.chromeOptions = checkNotNull(chromeOptions);
        this.capabilitiesOverrides = checkNotNull(capabilitiesOverrides);
    }

    @Override
    public WebDriver createWebDriver(BrowserMobProxy proxy, CertificateAndKeySource certificateAndKeySource) throws IOException {
        DesiredCapabilities capabilities = toCapabilities(chromeOptions);
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
        capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
        capabilities.merge(capabilitiesOverrides);
        ChromeDriver driver = WebDriverSupport.chromeInEnvironment(environmentSupplier.get()).create(capabilities);
        return driver;
    }

    protected static DesiredCapabilities toCapabilities(ChromeOptions chromeOptions) {
        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
        capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
        return capabilities;
    }
}
