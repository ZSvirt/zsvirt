package org.zstack.zmigrate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zstack.zmigrate.ZMigrateConstant.GATEWAY_SSH_USERNAME;
import static org.zstack.zmigrate.ZMigrateConstant.VDDK_PACKAGE_DIRECTORY;
import static org.zstack.zmigrate.ZMigrateConstant.vddkUploadDirectory;
import static org.zstack.zmigrate.ZMigrateConstant.vddkUploadPath;

public final class ZMigrateVddkBundle {
    private static final Pattern PUBLISHED_FILE_PATTERN =
            Pattern.compile("^ZSTACK_VDDK_FILE=([A-Za-z0-9._-]+)$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final Pattern MD5_PATTERN = Pattern.compile("^[0-9a-f]{32}$");

    private ZMigrateVddkBundle() {
    }

    public static Map<String, String> parseVersionMd5Config(String value) {
        if (value == null) {
            throw new IllegalArgumentException("VDDK version/checksum configuration cannot be null");
        }

        String[] entries = value.split(";", -1);
        if (entries.length != 2) {
            throw new IllegalArgumentException("VDDK version/checksum configuration must contain exactly two entries");
        }

        Map<String, String> result = new LinkedHashMap<>();
        Set<String> checksums = new HashSet<>();
        for (String entry : entries) {
            if (!entry.equals(entry.trim())) {
                throw new IllegalArgumentException("VDDK version/checksum entries cannot contain surrounding whitespace");
            }
            int separator = entry.lastIndexOf('-');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw new IllegalArgumentException("invalid VDDK version/checksum entry");
            }
            String version = entry.substring(0, separator);
            String checksum = entry.substring(separator + 1);
            if (!VERSION_PATTERN.matcher(version).matches() || !MD5_PATTERN.matcher(checksum).matches()) {
                throw new IllegalArgumentException("invalid VDDK version or MD5 value");
            }
            if (result.put(version, checksum) != null) {
                throw new IllegalArgumentException("VDDK versions must be unique");
            }
            if (!checksums.add(checksum)) {
                throw new IllegalArgumentException("VDDK MD5 values must be unique");
            }
        }
        return result;
    }

    public static String buildPrepareCommand(long uploadTimestamp) {
        String taskDirectory = vddkUploadDirectory(uploadTimestamp);
        String archivePath = vddkUploadPath(uploadTimestamp);
        String extractedDirectory = taskDirectory + "/extracted";
        String script = String.join("\n",
                "set -eu",
                "task_dir=" + shellQuote(taskDirectory),
                "archive_path=" + shellQuote(archivePath),
                "extracted_dir=" + shellQuote(extractedDirectory),
                "if test -e \"$task_dir\" || test -L \"$task_dir\"; then",
                "  " + safeDirectoryTest("task_dir"),
                "else",
                "  mkdir -m 0700 -- \"$task_dir\"",
                "fi",
                "rm -rf -- \"$extracted_dir\"",
                "rm -f -- \"$archive_path\"");
        return "sh -c " + shellQuote(script);
    }

    public static String buildActivationCommand(long uploadTimestamp, Map<String, String> required) {
        if (required == null || required.size() != 2) {
            throw new IllegalArgumentException("exactly two VDDK version/checksum entries are required");
        }
        String[] checksums = getDistributionVersions(required).stream()
                .map(required::get).toArray(String[]::new);
        for (String checksum : checksums) {
            if (!MD5_PATTERN.matcher(checksum).matches()) {
                throw new IllegalArgumentException("invalid VDDK MD5 value");
            }
        }

        String taskDirectory = vddkUploadDirectory(uploadTimestamp);
        String archivePath = vddkUploadPath(uploadTimestamp);
        String extractedDirectory = taskDirectory + "/extracted";
        String script = String.join("\n",
                "set -eu",
                "export LC_ALL=C",
                "task_dir=" + shellQuote(taskDirectory),
                "archive_path=" + shellQuote(archivePath),
                "extracted_dir=" + shellQuote(extractedDirectory),
                "destination=" + shellQuote(VDDK_PACKAGE_DIRECTORY),
                "entries_file=\"$task_dir/entries\"",
                "modes_file=\"$task_dir/modes\"",
                "files_file=\"$task_dir/files\"",
                "checksums_file=\"$task_dir/checksums\"",
                "cleanup() { rm -rf -- \"$task_dir\"; }",
                "fail() { echo \"$1\" >&2; exit 1; }",
                "trap cleanup EXIT HUP INT TERM",
                safeDirectoryTest("task_dir") + " || fail \"invalid VDDK task directory\"",
                safeRegularFileTest("archive_path") + " || fail \"VDDK tar.gz is missing or unsafe\"",
                "command -v tar >/dev/null 2>&1 || fail \"tar is required\"",
                "command -v gzip >/dev/null 2>&1 || fail \"gzip is required\"",
                "command -v md5sum >/dev/null 2>&1 || fail \"md5sum is required\"",
                "tar --list --gzip --quoting-style=escape --file \"$archive_path\" > \"$entries_file\" || fail \"cannot list VDDK tar.gz\"",
                "tar --list --verbose --gzip --quoting-style=escape --file \"$archive_path\" > \"$modes_file\" || fail \"cannot inspect VDDK tar.gz\"",
                "test -s \"$entries_file\" || fail \"VDDK tar.gz is empty\"",
                "entry_count=$(wc -l < \"$entries_file\" | tr -d \" \t\")",
                "mode_count=$(grep -Ec \"^[bcdhlps-]\" \"$modes_file\" || true)",
                "test \"$entry_count\" -eq \"$mode_count\" || fail \"unrecognized VDDK tar.gz entry type\"",
                "if grep -Eq \"^[bchlps]\" \"$modes_file\"; then fail \"VDDK tar.gz contains a non-regular entry\"; fi",
                "regular_count=$(grep -Ec \"^-\" \"$modes_file\" || true)",
                "test \"$regular_count\" -eq 2 || fail \"VDDK tar.gz must contain exactly two regular files\"",
                "test -z \"$(sort \"$entries_file\" | uniq -d)\" || fail \"VDDK tar.gz contains duplicate paths\"",
                archiveEntryPathValidation(),
                "rm -rf -- \"$extracted_dir\"",
                "mkdir -m 0700 -- \"$extracted_dir\"",
                "chown " + GATEWAY_SSH_USERNAME + " -- \"$extracted_dir\"",
                "sudo -u " + GATEWAY_SSH_USERNAME + " tar --extract --gzip --no-same-owner --no-same-permissions --delay-directory-restore --file \"$archive_path\" --directory \"$extracted_dir\" || fail \"cannot extract VDDK tar.gz\"",
                "test -z \"$(find \"$extracted_dir\" -type l -print -quit)\" || fail \"VDDK tar.gz contains a symbolic link\"",
                "test -z \"$(find \"$extracted_dir\" -mindepth 1 ! -type d ! -type f -print -quit)\" || fail \"VDDK tar.gz contains an unsupported entry\"",
                "find \"$extracted_dir\" -type f -print | sort > \"$files_file\"",
                "test \"$(wc -l < \"$files_file\" | tr -d \" \t\")\" -eq 2 || fail \"VDDK tar.gz must extract exactly two files\"",
                "test -z \"$(sed 's#.*/##' \"$files_file\" | sort | uniq -d)\" || fail \"VDDK tar.gz contains duplicate basenames\"",
                ": > \"$checksums_file\"",
                "file_one=",
                "file_two=",
                "while IFS= read -r file; do",
                "  checksum=$(md5sum -- \"$file\")",
                "  checksum=${checksum%% *}",
                "  case \"$checksum\" in",
                "    " + checksums[0] + ") file_one=$file ;;",
                "    " + checksums[1] + ") file_two=$file ;;",
                "    *) fail \"VDDK MD5 validation failed\" ;;",
                "  esac",
                "  printf \"%s\\n\" \"$checksum\" >> \"$checksums_file\"",
                "done < \"$files_file\"",
                "test \"$(sort -u \"$checksums_file\" | wc -l | tr -d \" \t\")\" -eq 2 || fail \"both configured VDDK packages are required\"",
                "test -n \"$file_one\" && test -n \"$file_two\" || fail \"both configured VDDK packages are required\"",
                safeDirectoryTest("destination") + " || fail \"VDDK package directory is missing or unsafe\"",
                "chown www-data:www-data -- \"$file_one\" \"$file_two\"",
                buildIdempotentPublishScript());
        return "sudo -S -p '' sh -c " + shellQuote(script);
    }

    static String buildIdempotentPublishScript() {
        return String.join("\n",
                "publish_file() {",
                "  file=$1",
                "  base=${file##*/}",
                "  destination_file=$destination/$base",
                "  source_checksum=$(md5sum -- \"$file\")",
                "  source_checksum=${source_checksum%% *}",
                "  if test -e \"$destination_file\" || test -L \"$destination_file\"; then",
                "    " + safeRegularFileTest("destination_file") + " || fail \"VDDK destination path is unsafe\"",
                "    destination_checksum=$(md5sum -- \"$destination_file\")",
                "    destination_checksum=${destination_checksum%% *}",
                "    test \"$destination_checksum\" = \"$source_checksum\" || fail \"destination checksum does not match uploaded VDDK\"",
                "    return",
                "  fi",
                "  mv -n -- \"$file\" \"$destination_file\" || fail \"cannot publish VDDK file\"",
                "  " + safeRegularFileTest("destination_file") + " || fail \"VDDK destination path is unsafe\"",
                "  destination_checksum=$(md5sum -- \"$destination_file\")",
                "  destination_checksum=${destination_checksum%% *}",
                "  test \"$destination_checksum\" = \"$source_checksum\" || fail \"destination checksum does not match uploaded VDDK\"",
                "}",
                "publish_file \"$file_one\"",
                "publish_file \"$file_two\"",
                "printf 'ZSTACK_VDDK_FILE=%s\\n' \"${file_one##*/}\"",
                "printf 'ZSTACK_VDDK_FILE=%s\\n' \"${file_two##*/}\"");
    }

    public static List<String> parsePublishedFileNames(String stdout) {
        List<String> fileNames = new ArrayList<>();
        if (stdout == null || stdout.isEmpty()) {
            return fileNames;
        }
        for (String line : stdout.split("\\r?\\n")) {
            Matcher matcher = PUBLISHED_FILE_PATTERN.matcher(line);
            if (matcher.matches()) {
                fileNames.add(matcher.group(1));
            }
        }
        return fileNames;
    }

    public static List<String> getDistributionVersions(Map<String, ?> packages) {
        List<String> versions = new ArrayList<>(packages.keySet());
        // Management requires 8.0.3 before accepting dependent VDDK packages.
        // Keep version identity tied to the configured checksum, not the filename.
        if (versions.remove("8.0.3")) {
            versions.add(0, "8.0.3");
        }
        return versions;
    }

    public static String buildCleanupCommand(long uploadTimestamp) {
        return "rm -rf -- " + shellQuote(vddkUploadDirectory(uploadTimestamp));
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private static String safeDirectoryTest(String variableName) {
        return String.format("test -d \"$%1$s\" && test ! -L \"$%1$s\"", variableName);
    }

    private static String safeRegularFileTest(String variableName) {
        return String.format("test -f \"$%1$s\" && test ! -L \"$%1$s\"", variableName);
    }

    private static String archiveEntryPathValidation() {
        return String.join("\n",
                "while IFS= read -r entry; do",
                "  test -n \"$entry\" || fail \"VDDK tar.gz contains an empty path\"",
                "  printf \"%s\\n\" \"$entry\" | grep -Eq \"^[A-Za-z0-9._/-]+/?$\" || fail \"VDDK tar.gz contains an unsafe filename\"",
                "  case \"$entry\" in .|..|./*|../*|*/./*|*/../*|*/.|*/..|/*|*\\\\*|*//*) fail \"VDDK tar.gz contains path traversal\" ;; esac",
                "done < \"$entries_file\"");
    }
}
