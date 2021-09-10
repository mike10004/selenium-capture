package io.github.mike10004.seleniumcapture;

import com.browserup.harreader.model.HttpMethod;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.io.CharSource;
import com.google.common.net.MediaType;
import com.browserup.harreader.model.HarContent;
import com.browserup.harreader.model.HarPostData;
import com.browserup.harreader.model.HarPostDataParam;
import com.browserup.harreader.model.HarRequest;
import com.browserup.harreader.model.HarResponse;
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

public final class HarInteractions {
    
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
    static ImmutableHttpMessage.HttpContentSource toContentSource(@Nullable HarPostData postData) {
        if (postData == null || (isNullOrEmpty(postData.getParams()) && Strings.isNullOrEmpty(postData.getText()))) {
            return ImmutableHttpMessage.HttpContentSource.empty();
        }
        if (!Strings.isNullOrEmpty(postData.getText())) {
            return ImmutableHttpMessage.HttpContentSource.fromChars(CharSource.wrap(postData.getText()));
        }
        if (!isNullOrEmpty(postData.getParams())) {
            Charset charset = maybeGetCharset(postData.getMimeType()).orElse(StandardCharsets.UTF_8);
            String encodedForm = URLEncodedUtils.format(postData.getParams().stream().map(paramToPair()).collect(Collectors.toList()), charset);
            return ImmutableHttpMessage.HttpContentSource.fromChars(CharSource.wrap(encodedForm));
        }
        LoggerFactory.getLogger(HarInteractions.class).warn("could not transform nonempty post data to nonempty content source: {}", HarAnalysis.describe(postData));
        return ImmutableHttpMessage.HttpContentSource.empty();
    }

    @VisibleForTesting
    static ImmutableHttpMessage.HttpContentSource toContentSource(@Nullable HarContent content) {
        if (content == null || Strings.isNullOrEmpty(content.getText())) {
            return ImmutableHttpMessage.HttpContentSource.empty();
        }
        if ("base64".equals(content.getEncoding())) {
            return ImmutableHttpMessage.HttpContentSource.fromBase64(content.getText());
        }
        return ImmutableHttpMessage.HttpContentSource.fromChars(CharSource.wrap(content.getText()));
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
                .method(Optional.ofNullable(harRequest.getMethod()).map(HttpMethod::name).orElse("GET"))
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
