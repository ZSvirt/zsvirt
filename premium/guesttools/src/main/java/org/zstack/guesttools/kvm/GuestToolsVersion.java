package org.zstack.guesttools.kvm;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Objects;

/**
 * Created by Wenhao.Zhang on 22/11/04
 */
public class GuestToolsVersion implements Comparable<GuestToolsVersion> {
    public final int metricValue;
    private int major;
    private int minor;
    private int patch;

    private static Comparator<GuestToolsVersion> comparator =
            Comparator.comparing(GuestToolsVersion::getMajor)
                    .thenComparing(GuestToolsVersion::getMinor)
                    .thenComparing(GuestToolsVersion::getPatch);

    public GuestToolsVersion(int metricValue) {
        this.metricValue = metricValue;
        this.major = (metricValue & 0xFF000000) >> 24;
        this.minor = (metricValue & 0xFF0000) >> 16;
        this.patch = (metricValue & 0xFF00) >> 8;
    }

    public GuestToolsVersion() {
        this.metricValue = 0x10000;
        this.major = 1;
    }

    @Override
    public int compareTo(@Nonnull GuestToolsVersion o) {
        return comparator.compare(this, o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuestToolsVersion that = (GuestToolsVersion) o;
        return metricValue == that.metricValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(metricValue);
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }
}
