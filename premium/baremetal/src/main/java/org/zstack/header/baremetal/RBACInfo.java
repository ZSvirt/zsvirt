package org.zstack.header.baremetal;

import org.zstack.header.baremetal.chassis.APICheckBaremetalChassisConfigFileMsg;
import org.zstack.header.baremetal.chassis.APIGetBaremetalChassisPowerStatusMsg;
import org.zstack.header.baremetal.chassis.APIQueryBaremetalChassisMsg;
import org.zstack.header.baremetal.chassis.BaremetalChassisConstant;
import org.zstack.header.baremetal.chassis.BaremetalChassisVO;
import org.zstack.header.baremetal.instance.APIQueryBaremetalInstanceMsg;
import org.zstack.header.baremetal.instance.BaremetalInstanceConstant;
import org.zstack.header.baremetal.instance.BaremetalInstanceVO;
import org.zstack.header.baremetal.network.APIQueryBaremetalBondingMsg;
import org.zstack.header.baremetal.network.BaremetalNetworkConstant;
import org.zstack.header.baremetal.preconfiguration.APIQueryPreconfigurationTemplateMsg;
import org.zstack.header.baremetal.preconfiguration.PreconfigurationConstant;
import org.zstack.header.baremetal.pxeserver.APIQueryBaremetalPxeServerMsg;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerConstant;
import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "baremetal";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .normalAPIs(
                        APICheckBaremetalChassisConfigFileMsg.class,
                        APIQueryBaremetalChassisMsg.class,
                        APIQueryBaremetalPxeServerMsg.class,
                        APIQueryPreconfigurationTemplateMsg.class,
                        APIQueryBaremetalInstanceMsg.class,
                        APIQueryBaremetalBondingMsg.class,
                        APIGetBaremetalChassisPowerStatusMsg.class)
                .productName("baremetal")
                .build();

        roleBuilder()
                .uuid("7699f3c140192b0b95347a666d435751")
                .permissionBaseOnThis()
                .build();

        attributeSupportResourceBuilder()
                .resources(BaremetalChassisVO.class, BaremetalInstanceVO.class)
                .build();

        apis()
                .inPackage("org.zstack.header.baremetal.chassis")
                .toService(BaremetalChassisConstant.SERVICE_ID)
                .build();

        apis()
                .inPackage("org.zstack.header.baremetal.instance")
                .toService(BaremetalInstanceConstant.SERVICE_ID)
                .build();

        apis()
                .inPackage("org.zstack.header.baremetal.network")
                .toService(BaremetalNetworkConstant.SERVICE_ID)
                .build();

        apis()
                .inPackage("org.zstack.header.baremetal.preconfiguration")
                .toService(PreconfigurationConstant.SERVICE_ID)
                .build();

        apis()
                .inPackage("org.zstack.header.baremetal.pxeserver")
                .toService(BaremetalPxeServerConstant.SERVICE_ID)
                .build();

        apis()
                .api(
                        APIQueryBaremetalChassisMsg.class,
                        APIQueryBaremetalInstanceMsg.class,
                        APIQueryBaremetalBondingMsg.class,
                        APIQueryPreconfigurationTemplateMsg.class,
                        APIQueryBaremetalPxeServerMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
