package org.zstack.utils;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class URLBuilder {
    public static String buildUrl(String scheme, String host, int port, String...paths) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        builder.scheme(scheme).host(host).port(port);
        for (String p : paths) {
            builder.path(p);
        }
        return builder.build().toUriString();
    }
    
    public static String buildHttpUrl(String host, int port, String...paths) {
        return buildUrl("http", host, port, paths);
    }
    
    public static String buildSslHttpUrl(String host, int port, String...paths) {
        return buildUrl("https", host, port, paths);
    }
    
    public static String buildUrlFromBase(String baseUrl, String...paths) {
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        for (String p : paths) {
            ub.path(p);
        }
        return ub.build().toString();
    }

    /**
     * <p>Encodes a URI by replacing each instance of certain characters by escape sequences.</p>
     * 
     * <p>Example:
     *   <li>input:  123 qwe
     *   <li>output: 123%20qwe
     *   <li>input:  A-Za-z0-9-_.@()!~*'$&:,_+=
     *   <li>output: A-Za-z0-9-_.@()!~*'$&:,_+=
     *   <li>input:  #%^{}[]"<>?/
     *   <li>output: %23%25%5E%7B%7D%5B%5D%22%3C%3E%3F%2F
     *   <li>input:  http://9.9.9.9:9999/999#99
     *   <li>output: http:%2F%2F9.9.9.9:9999%2F999%2399
     *   </li>
     * </p>
     */
    public static String buildUrlComponent(String uri) {
        return UriComponentsBuilder.newInstance().pathSegment(uri).toUriString().substring(1);
    }

    public static String hideUrlPassword(String url){
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        UriComponents u = builder.build();
        if (u.getUserInfo() == null) {
            return url;
        } else {
            return builder.userInfo(u.getUserInfo().split(":")[0]).build().toUriString();
        }
    }
}
