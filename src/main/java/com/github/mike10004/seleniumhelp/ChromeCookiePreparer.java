package com.github.mike10004.seleniumhelp;

import com.github.mike10004.chromecookieimplant.ChromeCookie;
import com.github.mike10004.chromecookieimplant.ChromeCookieImplanter;
import com.github.mike10004.seleniumhelp.ChromeWebDriverFactory.CookiePreparer;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of a cookie preparer that uses the Chrome Cookie Implant extension.
 */
class ChromeCookiePreparer implements CookiePreparer {

    private static final Logger log = LoggerFactory.getLogger(ChromeCookiePreparer.class);

    private final Path scratchDir;
    private final Supplier<? extends Collection<DeserializableCookie>> cookiesSupplier;
    private final transient ChromeCookieTransform chromeCookieTransform = new ChromeCookieTransform();
    private final ChromeCookieImplanter implanterClient;

    public ChromeCookiePreparer(Path scratchDir, Supplier<? extends Collection<DeserializableCookie>> cookiesSupplier) {
        this.scratchDir = checkNotNull(scratchDir);
        this.cookiesSupplier = checkNotNull(cookiesSupplier);
        implanterClient = new ChromeCookieImplanter();
    }

    @Override
    public void supplementOptions(ChromeOptions options) throws IOException {
        File crxFile = File.createTempFile("chrome-cookie-implant", ".crx", scratchDir.toFile());
        try (OutputStream crxOut = new FileOutputStream(crxFile)) {
            implanterClient.copyCrxTo(crxOut);
        }
        options.addExtensions(crxFile);
    }

    @Override
    public void prepareCookies(ChromeDriver driver) throws WebDriverException {
        Stream<DeserializableCookie> inputCookies = cookiesSupplier.get().stream();
        List<ChromeCookie> chromeCookies = inputCookies.map(chromeCookieTransform::transform).collect(Collectors.toList());
        implanterClient.implant(chromeCookies, driver);
        log.debug("{} cookies imported using implant extension", chromeCookies.size());
        driver.get("data:,"); // blank page
    }

}
