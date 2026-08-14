package org.zstack.snmp.agent;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
/**
 *
 * @Author : jingwang
 * @create 2023/8/11 16:27
 */
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "snmp-agent";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .build();
        apis()
                .inThisPackage()
                .toService("SNMP")
                .build();

        apis()
                .api(
                        APIQuerySnmpAgentMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
