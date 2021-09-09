package com.github.mike10004.seleniumhelp;

import com.google.common.io.ByteSource;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

public interface KeystoreInput {

    ByteSource getBytes();
    String getPassword();

    static KeystoreInput wrap(byte[] bytes, String password) {
        return wrap(ByteSource.wrap(bytes), password);
    }

    static KeystoreInput wrap(ByteSource bytes, String password) {
        requireNonNull(bytes);
        return new KeystoreInput() {
            @Override
            public ByteSource getBytes() {
                return bytes;
            }

            @Override
            public String getPassword() {
                return password;
            }
        };
    }

    default KeystoreInput copyFrozen() throws IOException {
        byte[] bytes = getBytes().read();
        String pw = getPassword();
        return new KeystoreInput() {
            @Override
            public ByteSource getBytes() {
                return ByteSource.wrap(bytes);
            }

            @Override
            public String getPassword() {
                return pw;
            }
        };
    }
}
