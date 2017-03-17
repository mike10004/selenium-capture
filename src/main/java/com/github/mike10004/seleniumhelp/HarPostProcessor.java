package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.core.har.Har;

/**
 * Interface for classes that apply modifications to HARs after traffic collection.
 */
public interface HarPostProcessor {
    void process(Har har);
}
