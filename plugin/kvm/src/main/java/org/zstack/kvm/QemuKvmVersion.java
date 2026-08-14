package org.zstack.kvm;

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.Arrays;
import java.util.stream.Collectors;

public class QemuKvmVersion {
    final String version;

    private static final ComparableVersion supportMirrorBitmapVersion = new ComparableVersion("4.2.0-627");
    private static final ComparableVersion supportBackgroundBackupVersion = new ComparableVersion("6.2.0");

    public QemuKvmVersion(String version) {
        this.version = Arrays.stream(version.split("-")).
                filter(it -> Character.isDigit(it.charAt(0)))
                .collect(Collectors.joining("-"));
    }

    public boolean supportMirrorBitmap() {
        return new ComparableVersion(version).compareTo(supportMirrorBitmapVersion) >= 0;
    }

    public boolean supportBackgroundBackup() {
        return new ComparableVersion(version).compareTo(supportBackgroundBackupVersion) >= 0;
    }
}
