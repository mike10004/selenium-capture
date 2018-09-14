package com.github.mike10004.seleniumhelp;

import com.google.common.base.MoreObjects;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.service.DriverService;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

class ServicedSession extends SimpleWebdrivingSession {

    private final DriverService service;

    ServicedSession(WebDriver driver, DriverService service) {
        super(driver);
        this.service = requireNonNull(service);
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper h = MoreObjects.toStringHelper(this);
        h.add("driver", getWebDriver());
        h.add("service", service);
        h.add("service.running", service.isRunning());
        return h.toString();
    }

    @Nullable
    @Override
    public DriverService getDriverService() {
        return service;
    }
}
