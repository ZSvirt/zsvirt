package org.zstack.billing;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

import org.zstack.billing.table.APIQueryAccountPriceTableRefMsg;
import org.zstack.billing.table.APIQueryPriceTableMsg;
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "billing";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APICalculateAccountSpendingMsg.class,
                        APICalculateAccountBillingSpendingMsg.class,
                        APIQueryAccountBillingMsg.class
                )
                .communityAvailable()
                .build();

        roleBuilder()
                .uuid("c38192cc2a904abeb7104c36fcdc53cd")
                .permissionBaseOnThis()
                .build();
        apis()
                .api(
                        APICalculateAccountBillingSpendingMsg.class,
                        APICalculateAccountSpendingMsg.class,
                        APICalculateResourceSpendingMsg.class,
                        APICleanupBillingUsageMsg.class,
                        APICreateResourcePriceMsg.class,
                        APIDeleteBillingMsg.class,
                        APIDeleteResourcePriceMsg.class,
                        APIUpdateResourcePriceMsg.class
                )
                .toService("billing")
                .build();

        apis()
                .api(
                        APIQueryAccountBillingMsg.class,
                        APIQueryResourcePriceMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .inPackage("org.zstack.billing.generator")
                .toService("billing")
                .build();
        apis()
                .inPackage("org.zstack.billing.table")
                .toService("billing")
                .build();
        apis()
                .api(
                        APIQueryAccountPriceTableRefMsg.class,
                        APIQueryPriceTableMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.billing.userconfig")
                .toService("billing")
                .build();
    }
}
