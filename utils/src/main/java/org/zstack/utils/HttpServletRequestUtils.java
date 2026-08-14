package org.zstack.utils;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Objects;

public class HttpServletRequestUtils {
    public static String getClientIP(HttpServletRequest req) {
        String ipAddress = req.getHeader("X-Request-Ip");
        if (isValidClientIP(ipAddress)) {
            return ipAddress.split(",")[0].trim();
        }

        final Enumeration<?> headers = req.getHeaderNames();
        for (; headers.hasMoreElements();) {
            Object headerObject = headers.nextElement();
            if (headerObject == null) {
                continue;
            }

            String header = Objects.toString(headerObject);
            switch (header) {
            case "X-Forwarded-For":
                ipAddress = req.getHeader(header).split(",")[0].trim();
                break;
            case "X-Request-Ip":
            case "X-Real-IP":
            case "Proxy-Client-IP":
            case "WL-Proxy-Client-IP":
            case "HTTP_X_FORWARDED_FOR":
            case "HTTP_X_FORWARDED":
            case "HTTP_X_CLUSTER_CLIENT_IP":
            case "HTTP_FORWARDED_FOR":
            case "HTTP_FORWARDED":
                ipAddress = req.getHeader(header);
                break;
            default:
                continue;
            }

            if (isValidClientIP(ipAddress)) {
                return ipAddress;
            }
        }

        return req.getRemoteAddr();
    }

    private static boolean isValidClientIP(String s) {
        return s != null && !s.isEmpty() && !"unknown".equalsIgnoreCase(s);
    }

    public static String getClientBrowser(HttpServletRequest req) {
        String userAgent = req.getHeader("User-Agent");
        if (StringUtils.isBlank(userAgent)) return "";

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("edge")) {
            return "Edge";
        } else if (userAgent.contains("trident") || userAgent.contains("msie")) {
            return "IE";
        } else if (userAgent.contains("360")) {
            return "360";
        } else if (userAgent.contains("tencenttraveler")) {
            return "Tencent Traveler";
        } else if (userAgent.contains("metas")) {
            return "Sogou Explorer";
        } else if (userAgent.contains("safari")) {
            return "Safari";
        } else if (userAgent.contains("cli")) {
            return "cli";
        }else {
            return "Other";
        }
    }
}
