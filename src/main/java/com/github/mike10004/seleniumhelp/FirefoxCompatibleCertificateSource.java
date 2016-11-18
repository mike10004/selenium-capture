package com.github.mike10004.seleniumhelp;

import com.google.common.io.ByteSource;
import net.lightbody.bmp.mitm.CertificateAndKeySource;

import java.io.IOException;

public interface FirefoxCompatibleCertificateSource extends CertificateAndKeySource {

    ByteSource getFirefoxCertificateDatabase() throws IOException;

}
