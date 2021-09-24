package io.github.mike10004.seleniumcapture;

import inet.ipaddr.IPAddressString;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.URI;

class CidrBlockBypassRule implements HostBypassRule {

    private final IPAddressString blockSpec;

    public CidrBlockBypassRule(String blockSpec) {
        this.blockSpec = new IPAddressString(blockSpec);
    }

    @Override
    public boolean isBypass(URI httpRequestUri) {
        String uriHost = httpRequestUri.getHost();
        if (uriHost == null) {
            return false;
        }
        if (InetAddressValidator.getInstance().isValidInet4Address(uriHost)) {
            return blockSpec.getSequentialRange().contains(new IPAddressString(uriHost).getAddress());
        }
        return false;
    }
}
