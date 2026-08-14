package org.zstack.header.identity;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.identity.login.APIGetLoginProceduresMsg;
import org.zstack.header.identity.login.APILogInMsg;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.identity.role.api.APIAttachRoleToAccountMsg;
import org.zstack.header.identity.role.api.APICreateRoleMsg;
import org.zstack.header.identity.role.api.APIDeleteRoleMsg;
import org.zstack.header.identity.role.api.APIDetachRoleFromAccountMsg;
import org.zstack.header.identity.role.api.APIGetRolePolicyActionsMsg;
import org.zstack.header.identity.role.api.APIQueryRoleAccountRefMsg;
import org.zstack.header.identity.role.api.APIQueryRoleMsg;
import org.zstack.header.identity.role.api.APIUpdateRoleMsg;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "identity";
    }

    {
        permissionBuilder()
                .adminOnlyAPIs(
                        APICreateAccountMsg.class,
                        APIUpdateQuotaMsg.class,
                        APIChangeResourceOwnerMsg.class,
                        APIAttachRoleToAccountMsg.class,
                        APIDetachRoleFromAccountMsg.class
                )
                .targetResources(AccountVO.class, RoleVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("acf2695d8c7c4c5587f5b136098fe45e")
                .permissionBaseOnThis()
                .excludeActions(
                        APICreateRoleMsg.class,
                        APIAttachRoleToAccountMsg.class,
                        APIDetachRoleFromAccountMsg.class,
                        APIDeleteRoleMsg.class,
                        APIUpdateRoleMsg.class
                )
                .build();

        roleBuilder()
                .uuid("09380f1d01183826b97e36ad04083677")
                .name("role")
                .actions(
                        APICreateRoleMsg.class,
                        APIAttachRoleToAccountMsg.class,
                        APIDetachRoleFromAccountMsg.class,
                        APIDeleteRoleMsg.class,
                        APIUpdateRoleMsg.class,
                        APIQueryRoleMsg.class
                )
                .build();

        roleContributorBuilder()
                .actions(
                        APIGetAccountQuotaUsageMsg.class,
                        APIGetLoginProceduresMsg.class,
                        APIGetResourceAccountMsg.class,
                        APIGetRolePolicyActionsMsg.class,
                        APILogInByAccountMsg.class,
                        APILogInMsg.class,
                        APILogOutMsg.class,
                        APIQueryAccountMsg.class,
                        APIQueryAccountResourceRefMsg.class,
                        APIQueryQuotaMsg.class,
                        APIQueryRoleAccountRefMsg.class,
                        APIQueryRoleMsg.class,
                        APIRenewSessionMsg.class,
                        APIRevokeResourceSharingMsg.class,
                        APIShareResourceMsg.class,
                        APIUpdateAccountMsg.class,
                        APIValidateSessionMsg.class
                )
                .toOtherRole()
                .build();

        attributeSupportResourceBuilder()
                .resources(AccountVO.class)
                .build();
        apis()
                .api(
                        APIChangeAccountTypeMsg.class,
                        APIChangeResourceOwnerMsg.class,
                        APICreateAccountMsg.class,
                        APIDeleteAccountMsg.class,
                        APIGetAccountQuotaUsageMsg.class,
                        APIGetResourceAccountMsg.class,
                        APILogInByAccountMsg.class,
                        APILogOutMsg.class,
                        APIRenewSessionMsg.class,
                        APIRevokeResourceSharingMsg.class,
                        APIShareResourceMsg.class,
                        APIUpdateAccountMsg.class,
                        APIUpdateQuotaMsg.class,
                        APIValidateSessionMsg.class
                )
                .toService("identity")
                .build();

        apis()
                .api(
                        APIQueryAccountMsg.class,
                        APIQueryAccountResourceRefMsg.class,
                        APIQueryQuotaMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .inPackage("org.zstack.header.identity.login")
                .toService("login")
                .build();
        apis()
                .inPackage("org.zstack.header.identity.role.api")
                .toService("rbac")
                .build();
        apis()
                .api(APIGetRolePolicyActionsMsg.class)
                .toService("identity")
                .build();
        apis()
                .api(
                        APIQueryRoleAccountRefMsg.class,
                        APIQueryRoleMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
