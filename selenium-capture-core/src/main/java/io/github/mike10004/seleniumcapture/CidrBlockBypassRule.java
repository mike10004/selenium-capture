package io.github.mike10004.seleniumcapture;

import com.google.common.base.MoreObjects;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.URI;

import static java.util.Objects.requireNonNull;

class CidrBlockBypassRule implements HostBypassRule {

    private final IPAddressSeqRange range;

    public CidrBlockBypassRule(IPAddressSeqRange range) {
        this.range = requireNonNull(range);
    }

    @Override
    public boolean isBypass(URI httpRequestUri) {
        String uriHost = httpRequestUri.getHost();
        if (uriHost == null) {
            return false;
        }
        if (InetAddressValidator.getInstance().isValidInet4Address(uriHost)) {
            try {
                return range.contains(new IPAddressString(uriHost).toAddress());
            } catch (AddressStringException | RuntimeException ignore) {
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("range", range)
                .toString();
    }
}
