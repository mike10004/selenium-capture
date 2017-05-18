package com.github.mike10004.seleniumhelp;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;

import static com.google.common.base.Preconditions.checkNotNull;

public class ArbitraryActions extends Actions {

    public ArbitraryActions(WebDriver driver) {
        super(checkNotNull(driver, "driver"));
    }

    @Override
    public ArbitraryActions keyDown(CharSequence theKey) {
        return (ArbitraryActions) super.keyDown(theKey);
    }

    @Override
    public ArbitraryActions keyDown(WebElement element, CharSequence theKey) {
        return (ArbitraryActions) super.keyDown(element, theKey);
    }

    @Override
    public ArbitraryActions keyUp(CharSequence theKey) {
        return (ArbitraryActions) super.keyUp(theKey);
    }

    @Override
    public ArbitraryActions keyUp(WebElement element, CharSequence theKey) {
        return (ArbitraryActions) super.keyUp(element, theKey);
    }

    @Override
    public ArbitraryActions sendKeys(CharSequence... keysToSend) {
        return (ArbitraryActions) super.sendKeys(keysToSend);
    }

    @Override
    public ArbitraryActions sendKeys(WebElement element, CharSequence... keysToSend) {
        return (ArbitraryActions) super.sendKeys(element, keysToSend);
    }

    @Override
    public ArbitraryActions clickAndHold(WebElement onElement) {
        return (ArbitraryActions) super.clickAndHold(onElement);
    }

    @Override
    public ArbitraryActions clickAndHold() {
        return (ArbitraryActions) super.clickAndHold();
    }

    @Override
    public ArbitraryActions release(WebElement onElement) {
        return (ArbitraryActions) super.release(onElement);
    }

    @Override
    public ArbitraryActions release() {
        return (ArbitraryActions) super.release();
    }

    @Override
    public ArbitraryActions click(WebElement onElement) {
        return (ArbitraryActions) super.click(onElement);
    }

    @Override
    public ArbitraryActions click() {
        return (ArbitraryActions) super.click();
    }

    @Override
    public ArbitraryActions doubleClick(WebElement onElement) {
        return (ArbitraryActions) super.doubleClick(onElement);
    }

    @Override
    public ArbitraryActions doubleClick() {
        return (ArbitraryActions) super.doubleClick();
    }

    @Override
    public ArbitraryActions moveToElement(WebElement toElement) {
        return (ArbitraryActions) super.moveToElement(toElement);
    }

    @Override
    public ArbitraryActions moveToElement(WebElement toElement, int xOffset, int yOffset) {
        return (ArbitraryActions) super.moveToElement(toElement, xOffset, yOffset);
    }

    @Override
    public ArbitraryActions moveByOffset(int xOffset, int yOffset) {
        return (ArbitraryActions) super.moveByOffset(xOffset, yOffset);
    }

    @Override
    public ArbitraryActions contextClick(WebElement onElement) {
        return (ArbitraryActions) super.contextClick(onElement);
    }

    @Override
    public ArbitraryActions contextClick() {
        return (ArbitraryActions) super.contextClick();
    }

    @Override
    public ArbitraryActions dragAndDrop(WebElement source, WebElement target) {
        return (ArbitraryActions) super.dragAndDrop(source, target);
    }

    @Override
    public ArbitraryActions dragAndDropBy(WebElement source, int xOffset, int yOffset) {
        return (ArbitraryActions) super.dragAndDropBy(source, xOffset, yOffset);
    }

    @Override
    public ArbitraryActions pause(long pause) {
        return (ArbitraryActions) super.pause(pause);
    }

    public ArbitraryActions act(Action action) {
        this.action.addAction(action);
        return this;
    }
}
