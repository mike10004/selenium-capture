package com.github.mike10004.seleniumhelp;

import com.github.mike10004.xvfbmanager.XvfbController;
import com.github.mike10004.xvfbtesting.XvfbRule;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.net.HttpHeaders;
import io.github.bonigarcia.wdm.MarionetteDriverManager;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarNameValuePair;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FirefoxCookieUsageTest extends CookieUsageTestBase {

    @BeforeClass
    public static void setup() {
        MarionetteDriverManager.getInstance().setup();
    }

    @Override
    protected WebDriverFactory createCookielessWebDriverFactory(XvfbController xvfbController) {
        return new FirefoxWebDriverFactory(xvfbController.configureEnvironment(new HashMap<>()), ImmutableMap.of(), ImmutableList.of());
    }

    @Override
    protected WebDriverFactory createCookiefulWebDriverFactory(XvfbController xvfbController, List<DeserializableCookie> cookiesSetByServer) {
        return new FirefoxWebDriverFactory(xvfbController.configureEnvironment(new HashMap<>()), ImmutableMap.of(), cookiesSetByServer);
    }

    @Test
    public void testCookieUsage() throws Exception {
        exerciseCookieCapabilities();
    }
}
