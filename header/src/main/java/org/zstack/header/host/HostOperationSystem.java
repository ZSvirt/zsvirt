package org.zstack.header.host;

import java.util.Objects;

/**
 * Define the description field for the operating system version for host.
 * 
 * Created by Wenhao.Zhang on 23/04/07
 */
public class HostOperationSystem {
    public final String distribution;
    public final String release;
    public final String version;

    private HostOperationSystem(String osDistribution, String release, String version) {
        this.distribution = osDistribution;
        this.release = release;
        this.version = version;
    }

    public boolean isValid() {
        return distribution != null && release != null && version != null;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", distribution, release, version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        HostOperationSystem that = (HostOperationSystem) o;
        return Objects.equals(distribution, that.distribution)
                && Objects.equals(release, that.release)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distribution, release, version);
    }

    public static HostOperationSystem of(String distribution, String osRelease, String osVersion) {
        return new HostOperationSystem(distribution, osRelease, osVersion);
    }
}
