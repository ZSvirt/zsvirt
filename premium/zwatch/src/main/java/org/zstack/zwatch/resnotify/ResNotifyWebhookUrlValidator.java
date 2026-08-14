package org.zstack.zwatch.resnotify;

import org.apache.commons.lang.StringUtils;

import java.net.URI;

/**
 * Webhook endpoints are commonly deployed inside the same on-prem network as
 * ZStack/ZCF, so this validator intentionally does not reject private or
 * loopback addresses. It only enforces an explicit HTTP(S) URL with a host.
 */
public class ResNotifyWebhookUrlValidator {
    private ResNotifyWebhookUrlValidator() {
    }

    public static String validate(String url) {
        if (StringUtils.isEmpty(url)) {
            return "webhookUrl cannot be empty";
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return String.format("webhookUrl[%s] must use http or https scheme", url);
            }

            if (StringUtils.isEmpty(uri.getHost())) {
                return String.format("webhookUrl[%s] must include a valid host", url);
            }

            return null;
        } catch (Exception e) {
            return String.format("webhookUrl[%s] is invalid: %s", url, e.getMessage());
        }
    }
}
