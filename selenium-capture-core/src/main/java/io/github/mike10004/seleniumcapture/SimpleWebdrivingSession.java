package io.github.mike10004.seleniumcapture;

import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.service.DriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

public class SimpleWebdrivingSession implements WebdrivingSession {

    private static final Logger log = LoggerFactory.getLogger(SimpleWebdrivingSession.class);

    private final WebDriver driver;

    public SimpleWebdrivingSession(WebDriver driver) {
        this.driver = requireNonNull(driver);
    }

    @Override
    public WebDriver getWebDriver() {
        return driver;
    }

    @Override
    public void tryQuit(Duration quitTimeout) throws WebdriverQuitException {
        ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            tryQuit(driver, quitTimeout, service);
        } finally {
            List<?> runnables = service.shutdownNow();
            log.trace("shutting down executor service {} which had {} tasks", service, runnables.size());
        }
    }

    @Override
    public void close() {
        log.trace("webdriver quitting");
        try {
            driver.quit();
        } catch (NoSuchSessionException e) {
            log.debug("tried to quit but no session exists; we consider this ignorable");
        }
    }

    @Override
    public String toString() {
        return String.format("SimpleWebdrivingSession{driver=%s}", driver.getClass().getName());
    }

    public static void tryQuit(WebDriver driver, Duration quitTimeout, ExecutorService service) throws WebdriverQuitException {
        Future<?> future = service.submit(() -> {
            try {
                driver.quit();
            } catch (RuntimeException e) {
                LoggerFactory.getLogger(WebdrivingSession.class).info("driver.quit() failed due to " + e);
            }
        });
        try {
            future.get(quitTimeout.toMillis(), TimeUnit.MILLISECONDS);
            log.trace("WebDriver quit() finished without timeout or interruption");
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            throw new WebdriverQuitException(new WeakReference<>(driver), "waiting for driver.quit aborted early", e);
        }
    }

    @Nullable
    @Override
    public DriverService getDriverService() {
        return null;
    }
}
