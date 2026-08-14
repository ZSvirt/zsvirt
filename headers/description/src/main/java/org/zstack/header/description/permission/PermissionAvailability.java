package org.zstack.header.description.permission;

/**
 * Product / community availability flags used by {@link PermissionBuilder}.
 */
public final class PermissionAvailability {
    private PermissionAvailability() {
    }

    public static final String COMMUNITY_AVAILABLE = "community_available";
    public static final String ZSV_BASIC_AVAILABLE = "zsv_basic_available";
    public static final String ZSV_PRO_AVAILABLE = "zsv_pro_available";
    public static final String ZSV_ADVANCED_AVAILABLE = "zsv_advanced_available";
}
