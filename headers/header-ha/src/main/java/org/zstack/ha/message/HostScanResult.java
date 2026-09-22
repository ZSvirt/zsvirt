package org.zstack.ha.message;

/**
 * Result of host alive scan used by HA checkers and host-scan extension points.
 */
public class HostScanResult {
    public boolean alive;
    public String evidence;

    public static HostScanResult ofAlive(String evidence) {
        HostScanResult r = new HostScanResult();
        r.alive = true;
        r.evidence = evidence;
        return r;
    }

    public static HostScanResult ofDead(String evidence) {
        HostScanResult r = new HostScanResult();
        r.alive = false;
        r.evidence = evidence;
        return r;
    }

    public boolean isDead() {
        return !alive;
    }
}
