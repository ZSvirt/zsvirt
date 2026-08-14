package org.zstack.network.securitygroup;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "security-group";
    }

    {
        permissionBuilder()
                .targetResources(SecurityGroupVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .build();

        roleBuilder()
                .uuid("4266a67e46cb4e68864899458187941e")
                .permissionBaseOnThis()
                .build();

        roleContributorBuilder()
                .toOtherRole()
                .actions(
                    APIGetCandidateVmNicForSecurityGroupMsg.class,
                    APIQuerySecurityGroupMsg.class,
                    APIQuerySecurityGroupRuleMsg.class,
                    APIQueryVmNicInSecurityGroupMsg.class,
                    APIQueryVmNicSecurityPolicyMsg.class,
                    APIValidateSecurityGroupRuleMsg.class
                )
                .build();

        roleContributorBuilder()
                .roleName("legacy")
                .actions("org.zstack.network.securitygroup.**")
                .build();
        apis()
                .inThisPackage()
                .toService("securityGroup")
                .build();

        apis()
                .api(
                        APIQuerySecurityGroupMsg.class,
                        APIQuerySecurityGroupRuleMsg.class,
                        APIQueryVmNicInSecurityGroupMsg.class,
                        APIQueryVmNicSecurityPolicyMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
