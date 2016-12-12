package com.github.mike10004.seleniumhelp;

import com.google.common.net.MediaType;

import static com.google.common.base.Preconditions.checkNotNull;

public class ContentTypes {
    
    public static boolean isSameTypeWithoutParameters(String contentType1, String contentType2) {
        MediaType mediaType1 = MediaType.parse(contentType1);
        boolean same = isSameTypeWithoutParameters(mediaType1, contentType2);
        return same;
    }
    
    public static boolean isSameTypeWithoutParameters(MediaType contentType1, String contentType2) {
        MediaType mediaType1 = checkNotNull(contentType1);
        MediaType mediaType2 = MediaType.parse(contentType2);
        boolean same = isSameTypeWithoutParameters(mediaType1, mediaType2);
        return same;
    }
    
    public static boolean isSameTypeWithoutParameters(MediaType contentType1, MediaType contentType2) {
        MediaType unparameterized1 = contentType1.withoutParameters();
        MediaType unparameterized2 = contentType2.withoutParameters();
        boolean same = unparameterized1.is(unparameterized2);
        return same;
    }
}
