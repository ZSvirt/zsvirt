package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmNicSpec;
import org.zstack.appliancevm.ApplianceVmSpec;
import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.List;

public class VirtualRouterOperator {
    public static ApplianceVmSpec addVirtualRouterVmNicSpec(ApplianceVmSpec aspec, L3NetworkInventory mgmtNw, L3NetworkInventory pubNw,
                                                        List<L3NetworkInventory> priNws, List<L3NetworkInventory> additionalPubNws,
                                                            boolean isRouter, boolean haEnabled) {
        ApplianceVmNicSpec mgmtNicSpec = new ApplianceVmNicSpec();
        mgmtNicSpec.setL3NetworkUuid(mgmtNw.getUuid());
//        if (aspec.getStaticVip().containsKey(mgmtNw.getUuid())) {
//            mgmtNicSpec.setStaticIp(aspec.getStaticVip().get(mgmtNw.getUuid()));
//        }

        if (aspec.getStaticIp().containsKey(mgmtNw.getUuid())) {
            mgmtNicSpec.setStaticIp(aspec.getStaticIp().get(mgmtNw.getUuid()));
        }
        if (isRouter) {
            mgmtNicSpec.setMetaData(VirtualRouterNicMetaData.MANAGEMENT_NIC_MASK.toString());
        }
        aspec.setManagementNic(mgmtNicSpec);
        aspec.setDefaultL3Network(mgmtNw);

        if (pubNw != null && !pubNw.getUuid().equals(mgmtNw.getUuid())) {
            /* public nic spec for ha group is not created here */
            if (!haEnabled) {
                ApplianceVmNicSpec pnicSpec = new ApplianceVmNicSpec();
                pnicSpec.setL3NetworkUuid(pubNw.getUuid());
                if (aspec.getStaticIp().containsKey(pubNw.getUuid())) {
                    pnicSpec.setStaticIp(aspec.getStaticIp().get(pubNw.getUuid()));
                }
                if (isRouter) {
                    pnicSpec.setMetaData(VirtualRouterNicMetaData.PUBLIC_NIC_MASK.toString());
                }
                pnicSpec.setAllowDuplicatedAddress(true);
                aspec.getAdditionalNics().add(pnicSpec);
                aspec.setDefaultRouteL3Network(pubNw);
            }
        } else {
            if (isRouter) {
                mgmtNicSpec.setMetaData(VirtualRouterNicMetaData.PUBLIC_AND_MANAGEMENT_NIC_MASK.toString());
            }
            mgmtNicSpec.setAllowDuplicatedAddress(true);
            aspec.setDefaultRouteL3Network(mgmtNw);
        }

        if (pubNw != null) {
            aspec.setDefaultL3Network(pubNw);
        }

        for (L3NetworkInventory pri : priNws) {
            ApplianceVmNicSpec nicSpec = new ApplianceVmNicSpec();
            nicSpec.setL3NetworkUuid(pri.getUuid());
            if (aspec.getStaticIp().containsKey(pri.getUuid())) {
                nicSpec.setStaticIp(aspec.getStaticIp().get(pri.getUuid()));
            }
            if (isRouter) {
                nicSpec.setMetaData(VirtualRouterNicMetaData.GUEST_NIC_MASK.toString());
            }
            aspec.getAdditionalNics().add(nicSpec);
        }

        for (L3NetworkInventory pub : additionalPubNws) {
            ApplianceVmNicSpec nicSpec = new ApplianceVmNicSpec();
            nicSpec.setL3NetworkUuid(pub.getUuid());
            if (aspec.getStaticIp().containsKey(pub.getUuid())) {
                nicSpec.setStaticIp(aspec.getStaticIp().get(pub.getUuid()));
            }
            if (isRouter) {
                nicSpec.setMetaData(VirtualRouterNicMetaData.ADDITIONAL_PUBLIC_NIC_MASK.toString());
            }
            aspec.getAdditionalNics().add(nicSpec);
        }

        return aspec;
    }
}
