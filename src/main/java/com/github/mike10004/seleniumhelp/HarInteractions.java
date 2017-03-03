package com.github.mike10004.seleniumhelp;

import com.github.mike10004.seleniumhelp.ImmutableHttpMessage.HttpContentSource;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.io.CharSource;
import com.google.common.net.MediaType;
import net.lightbody.bmp.core.har.HarContent;
import net.lightbody.bmp.core.har.HarPostData;
import net.lightbody.bmp.core.har.HarPostDataParam;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

final class HarInteractions {
    private HarInteractions() {}

    private static boolean isNullOrEmpty(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    private static java.util.function.Function<HarPostDataParam, org.apache.http.NameValuePair> paramToPair() {
        return param -> new BasicNameValuePair(param.getName(), param.getValue());
    }

    @VisibleForTesting
    static java.util.Optional<Charset> maybeGetCharset(@Nullable String contentType) {
        if (contentType != null) {
            try {
                return java.util.Optional.ofNullable(MediaType.parse(contentType).charset().orNull());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Optional.empty();
    }

    /**
     * Transform post data to a content source. This method needs a lot of work; it will only behave as expected
     * for arguments with every simple contents.
     * @param postData the post data
     * @return a content source
     */
    @VisibleForTesting
    static HttpContentSource toContentSource(@Nullable HarPostData postData) {
        if (postData == null || (isNullOrEmpty(postData.getParams()) && Strings.isNullOrEmpty(postData.getText()))) {
            return HttpContentSource.empty();
        }
        if (!Strings.isNullOrEmpty(postData.getText())) {
            return HttpContentSource.fromChars(CharSource.wrap(postData.getText()));
        }
        if (!isNullOrEmpty(postData.getParams())) {
            Charset charset = maybeGetCharset(postData.getMimeType()).orElse(StandardCharsets.UTF_8);
            String encodedForm = URLEncodedUtils.format(postData.getParams().stream().map(paramToPair()).collect(Collectors.toList()), charset);
            return HttpContentSource.fromChars(CharSource.wrap(encodedForm));
        }
        LoggerFactory.getLogger(HarInteractions.class).warn("could not transform nonempty post data to nonempty content source: {}", HarAnalysis.describe(postData));
        return HttpContentSource.empty();
    }

    @VisibleForTesting
    static HttpContentSource toContentSource(@Nullable HarContent content) {
        if (content == null || Strings.isNullOrEmpty(content.getText())) {
            return HttpContentSource.empty();
        }
        if ("base64".equals(content.getEncoding())) {
            return HttpContentSource.fromBase64(content.getText());
        }
        return HttpContentSource.fromChars(CharSource.wrap(content.getText()));
    }

    @VisibleForTesting
    static URI parseUri(String uriStr) {
        try {
            URL url = new URL(uriStr);
            return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static ImmutableHttpRequest freeze(HarRequest harRequest) {
        if (harRequest.getUrl() == null) {
            throw new IllegalArgumentException("url is null in " + HarAnalysis.describe(harRequest));
        }
        return ImmutableHttpRequest.builder(parseUri(harRequest.getUrl()))
                .method(Optional.ofNullable(harRequest.getMethod()).orElse("GET"))
                .addHeaders(harRequest.getHeaders().stream().map(pair -> new SimpleImmutableEntry<>(pair.getName(), pair.getValue())))
                .content(toContentSource(harRequest.getPostData()))
                .build();
    }

    public static ImmutableHttpResponse freeze(HarResponse harResponse) {
        return ImmutableHttpResponse.builder(harResponse.getStatus())
                .addHeaders(harResponse.getHeaders().stream().map(pair -> new SimpleImmutableEntry<>(pair.getName(), pair.getValue())))
                .content(toContentSource(harResponse.getContent()))
                .build();
    }

}
