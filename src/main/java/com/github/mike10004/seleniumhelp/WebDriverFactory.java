package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import org.openqa.selenium.WebDriver;

import java.io.IOException;

public interface WebDriverFactory {

    WebDriver createWebDriver (BrowserMobProxy proxy, CertificateAndKeySource certificateAndKeySource) throws IOException;

}
