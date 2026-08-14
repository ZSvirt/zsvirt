package org.zstack.utils.path;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemotePathValidator {
    // Shell metacharacters that must not appear in paths sent to remote agents.
    // Mirrors zstacklib/utils/linux.py _SHELL_UNSAFE_RE.
    private static final Pattern SHELL_UNSAFE_PATTERN =
            Pattern.compile("[;|&$`'\"\\\\(){}\\[\\]<>!#~\\n\\r\\x00*?]");

    // Protected system directories that must never be used as a target path.
    private static final Set<String> PROTECTED_PATHS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "/", "/bin", "/boot", "/dev", "/etc", "/lib", "/lib64",
                    "/proc", "/run", "/sbin", "/srv", "/sys", "/usr", "/var")));

    public static final Set<String> ALLOWED_URL_SCHEMES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("http", "https", "ftp", "sftp")));

    // SSH username: only alphanumeric, dots, hyphens, underscores, optional trailing $
    private static final Pattern SSH_USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._-]+\\$?$");

    /**
     * Validate a remote path to be sent to an agent.
     * Returns null if valid, or an error message if invalid.
     *
     * Checks: non-empty, absolute, no traversal, no shell metacharacters,
     * no protected system directory.
     */
    public static String validateRemotePath(String path, String paramName) {
        if (path == null || path.isEmpty()) {
            return String.format("%s cannot be null or empty", paramName);
        }
        if (!path.startsWith("/")) {
            return String.format("%s [%s] must be an absolute path", paramName, path);
        }
        // Canonicalize by path components: collapse slashes, remove ".", reject ".."
        List<String> segments = new ArrayList<>();
        for (String component : path.split("/+")) {
            if (component.isEmpty() || ".".equals(component)) {
                continue;
            }
            if ("..".equals(component)) {
                return String.format("%s [%s] contains path traversal sequence", paramName, path);
            }
            segments.add(component);
        }
        String normalized = segments.isEmpty() ? "/" : "/" + String.join("/", segments);
        // Reject shell metacharacters (command injection via agent shell calls)
        Matcher m = SHELL_UNSAFE_PATTERN.matcher(path);
        if (m.find()) {
            return String.format("%s [%s] contains unsafe character '%s'", paramName, path, m.group());
        }
        // Reject protected system directories as direct targets
        if (PROTECTED_PATHS.contains(normalized)) {
            return String.format("%s [%s] targets a protected system directory", paramName, path);
        }
        return null;
    }

    /**
     * Validate a URL scheme against the allowed list and return the lowercase scheme.
     * Returns a two-element array: [0] = error message (null if valid), [1] = scheme (null if invalid).
     * Parses the URI only once, combining validation and extraction.
     */
    public static String[] validateAndExtractUrlScheme(String url) {
        if (url == null || url.isEmpty()) {
            return new String[]{"URL cannot be null or empty", null};
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return new String[]{String.format("failed to parse URL [%s]: %s", url, e.getMessage()), null};
        }
        String scheme = uri.getScheme();
        if (scheme == null || scheme.isEmpty()) {
            return new String[]{String.format("URL [%s] is missing a protocol prefix", url), null};
        }
        String lowerScheme = scheme.toLowerCase(Locale.ROOT);
        if (!ALLOWED_URL_SCHEMES.contains(lowerScheme)) {
            return new String[]{String.format("URL [%s] uses unsupported protocol [%s], only %s are allowed",
                    url, scheme, ALLOWED_URL_SCHEMES), null};
        }
        return new String[]{null, lowerScheme};
    }

    /**
     * Validate an SSH username.
     * Returns null if valid, or an error message if invalid.
     */
    public static String validateSshUsername(String username) {
        if (username == null || !SSH_USERNAME_PATTERN.matcher(username).matches()) {
            return String.format("SSH username [%s] is invalid, only alphanumeric characters, dots, hyphens, underscores and trailing dollar sign are allowed",
                    username);
        }
        return null;
    }

    /**
     * Validate a list of remote file paths.
     * Returns null if all valid, or the first error message encountered.
     */
    public static String validateFilePaths(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return null;
        }
        for (String filePath : filePaths) {
            String err = validateRemotePath(filePath, "filePath");
            if (err != null) {
                return err;
            }
        }
        return null;
    }
}
