package org.zstack.header.identity.role;

public enum RolePolicyResourceEffect {
    Allow,
    Exclude, // not support now

    /**
     * allow in a range (clusterUuid, zoneUuid)
     */
    Range, // not support now
}
