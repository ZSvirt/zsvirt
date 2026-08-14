package org.zstack.mevoco;

import org.zstack.header.identity.rbac.RBAC;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.header.PackageAPIInfo.*;

/**
 * Created by Wenhao.Zhang on 2024/07/17
 */
public class ZSpherePermissions {
    private static final CLogger logger = Utils.getLogger(ZSpherePermissions.class);

    public static List<PermissionBucket> scanPermissionBuckets() {
        List<PermissionBucket> results = new ArrayList<>();

        for (org.zstack.header.description.permission.Permission permission : RBAC.permissions) {
            PermissionBucket bucket = new PermissionBucket();
            bucket.category = permission.name;
            bucket.className = permission.basePackage;

            if (!CollectionUtils.isEmpty(permission.productList)) {
                bucket.addOnsNeeded = permission.productList.toArray(new String[0]);
            }

            bucket.communityAvailable = permission.requirementList.contains(PERMISSION_COMMUNITY_AVAILABLE);
            bucket.zsvBasicAvailable = permission.requirementList.contains(PERMISSION_ZSV_BASIC_AVAILABLE);
            bucket.zsvProAvailable = permission.requirementList.contains(PERMISSION_ZSV_PRO_AVAILABLE);
            bucket.zsvAdvancedAvailable = bucket.zsvBasicAvailable || bucket.zsvProAvailable ||
                        permission.requirementList.contains(PERMISSION_ZSV_ADVANCED_AVAILABLE);
            results.add(bucket);
        }

        return results;
    }

    public static class PermissionBucket {
        public String className;
        public String category;

        public boolean communityAvailable;
        public boolean zsvBasicAvailable;
        public boolean zsvProAvailable;
        public boolean zsvAdvancedAvailable;

        public String[] addOnsNeeded = new String[0];
    }
}
