package io.github.mike10004.seleniumcapture;

import com.browserup.harreader.model.Har;

/**
 * Interface for classes that apply modifications to HARs after traffic collection.
 */
public interface HarPostProcessor {
    void process(Har har);
}
