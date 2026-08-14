package org.zstack.acl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccessControlListUtils {
    private static String domainStr = "^((\\*)[-a-zA-Z0-9]{0,62}|[a-zA-Z0-9][-a-zA-Z0-9]{0,62})(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+$";
    private static String urlStr= "^(/[a-zA-Z0-9\\.\\-\\?%#&]+)([a-zA-Z0-9\\.\\-\\?/%#&])*$";

    public static Pattern domainPattern = Pattern.compile(domainStr);
    public static Pattern urlPattern = Pattern.compile(urlStr);

    public static boolean isValidateDomain(String domain) {
        Matcher m = domainPattern.matcher(domain);
        return m.matches();
    }

    public static boolean isValidateUrl(String url) {
        Matcher m = urlPattern.matcher(url);
        return m.matches();
    }
}
