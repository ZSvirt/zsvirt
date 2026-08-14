package org.zstack.compute.vm;


import org.apache.commons.lang.StringUtils;
import org.zstack.header.apimediator.ApiMessageInterceptionException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.argerr;

public class VmHostnameUtils {
    private static final Pattern HOSTNAME_PATTERN =
            Pattern.compile("^(?=.{1,255}$)([a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)(\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");

    private static final Pattern WINDOWS_HOSTNAME_PATTERN =
            Pattern.compile("^(?!-)(?![0-9]+$)[a-zA-Z0-9\\-\\p{IsHan}]{2,63}(?<!-)$");

    private static final Pattern WINDOWS_NETBIOS_PATTERN =
            Pattern.compile("^(?!-)(?![0-9]+$)[a-zA-Z0-9\\-\\p{IsHan}]{2,15}(?<!-)$");

    private static final Pattern WINDOWS_DOMAIN_LABEL_PATTERN =
            Pattern.compile("^(?!-)[a-zA-Z0-9\\-\\p{IsHan}]{1,63}(?<!-)$");

    private static final Set<String> WINDOWS_RESERVED_NAMES = new HashSet<>(Arrays.asList(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    ));

    private static final int MAX_WINDOWS_NETBIOS_LENGTH = 15;

    public static String safeSubstringForHostname(String hostname) {
        if (hostname == null) {
            return null;
        }
        int length = hostname.codePointCount(0, hostname.length());
        if (length <= VmHostnameUtils.MAX_WINDOWS_NETBIOS_LENGTH) {
            return hostname;
        }
        int endIndex = hostname.offsetByCodePoints(0, VmHostnameUtils.MAX_WINDOWS_NETBIOS_LENGTH);
        return hostname.substring(0, endIndex);
    }

    public static void validateHostname(String hostname, boolean isWindows) {
        if (hostname == null) {
            return;
        } else if (hostname.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("hostname is empty"));
        }

        if (isWindows) {
            String netBiosName = safeSubstringForHostname(hostname);
            if (!WINDOWS_NETBIOS_PATTERN.matcher(netBiosName).matches()) {
                throw new ApiMessageInterceptionException(
                        argerr("%s is not a valid Windows NetBIOS hostname", netBiosName)
                );
            }
            if (WINDOWS_RESERVED_NAMES.contains(netBiosName.toUpperCase())) {
                throw new ApiMessageInterceptionException(
                        argerr("%s is a reserved Windows NetBIOS hostname", netBiosName)
                );
            }

            if (!WINDOWS_HOSTNAME_PATTERN.matcher(hostname).matches()) {
                throw new ApiMessageInterceptionException(
                        argerr("%s is not a valid Windows hostname", hostname)
                );
            }
        } else {
            if (!HOSTNAME_PATTERN.matcher(hostname).matches()) {
                throw new ApiMessageInterceptionException(
                        argerr("%s is not a valid hostname", hostname)
                );
            }
        }
    }

    public static void validateDomain(String domain) {
        if (StringUtils.isEmpty(domain)) {
            return;
        }

        if (domain.length() > 255) {
            throw new ApiMessageInterceptionException(
                    argerr("%s exceeds max length 255", domain)
            );
        }

        String[] labels = domain.split("\\.");
        for (String label : labels) {
            if (!WINDOWS_DOMAIN_LABEL_PATTERN.matcher(label).matches()) {
                throw new ApiMessageInterceptionException(
                        argerr("%s is not a valid domain label in %s", label, domain)
                );
            }

            if (WINDOWS_RESERVED_NAMES.contains(label.toUpperCase(Locale.ROOT))) {
                throw new ApiMessageInterceptionException(
                        argerr("%s is a reserved Windows label", label)
                );
            }
        }
    }
}
