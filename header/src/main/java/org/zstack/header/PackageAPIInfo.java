package org.zstack.header;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Deprecated: please use {@link org.zstack.header.description.PackageDescription}
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface PackageAPIInfo {
    public static String PERMISSION_COMMUNITY_AVAILABLE =
            org.zstack.header.description.permission.PermissionAvailability.COMMUNITY_AVAILABLE;
    /**
     * If an api is available in ZSV basic version,
     * then the api is also available in ZSV advanced version.
     *
     * It contains {@link #PERMISSION_ZSV_ADVANCED_AVAILABLE}
     */
    public static String PERMISSION_ZSV_BASIC_AVAILABLE =
            org.zstack.header.description.permission.PermissionAvailability.ZSV_BASIC_AVAILABLE;
    public static String PERMISSION_ZSV_PRO_AVAILABLE =
            org.zstack.header.description.permission.PermissionAvailability.ZSV_PRO_AVAILABLE;
    public static String PERMISSION_ZSV_ADVANCED_AVAILABLE =
            org.zstack.header.description.permission.PermissionAvailability.ZSV_ADVANCED_AVAILABLE;

    /**
     * replace by {@link org.zstack.header.description.PackageDescription#permissionName()} soon
     */
    String APICategoryName() default "";

    /**
     * If a package with PackageAPIInfo annotation, default permissions of api which in this package is:
     * 1. community_NOT_available (only premium API)
     * 2. zsv_NOT_available
     *
     * If a package without PackageAPIInfo annotation, the permissions of api which in this package is:
     * 1. community_available ({@link #PERMISSION_COMMUNITY_AVAILABLE})
     * 2. zsv_NOT_available
     */
    String[] permissions() default {};
    String productName() default "";
}
