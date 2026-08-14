package org.zstack.drs;

import org.zstack.header.description.PackageDescription;

import org.zstack.drs.api.APIQueryClusterDRSMsg;
import org.zstack.drs.api.APIQueryDRSAdviceMsg;
import org.zstack.drs.api.APIQueryDRSVmMigrationActivityMsg;
import org.zstack.header.search.SearchConstant;
/**
 * Created by lining on 2018/10/18.
 */
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "drs";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvProAvailable()
                .build();

        apis()
                .inPackage("org.zstack.drs.api")
                .toService("drs")
                .build();
        apis()
                .api(
                        APIQueryClusterDRSMsg.class,
                        APIQueryDRSAdviceMsg.class,
                        APIQueryDRSVmMigrationActivityMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
