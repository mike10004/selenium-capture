package com.github.mike10004.seleniumhelp;

import net.lightbody.bmp.mitm.CertificateAndKeySource;

import java.io.File;
import java.io.IOException;

public interface FirefoxCompatibleCertificateSource extends CertificateAndKeySource {

    void establishCertificateTrust(File profileFolder) throws IOException;

}
