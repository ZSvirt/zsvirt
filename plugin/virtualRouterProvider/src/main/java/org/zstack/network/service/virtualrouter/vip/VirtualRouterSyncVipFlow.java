package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;

import java.util.*;
import java.util.stream.Collectors;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncVipFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    @Qualifier("VirtualRouterVipBackend")
    protected VirtualRouterVipBackend vipExt;
    @Autowired
    protected VipConfigProxy proxy;
    @Autowired
    protected VirtualRouterManager vrMgr;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());

        List<String> vrVips = proxy.getServiceUuidsByRouterUuid(vr.getUuid(), VipVO.class.getSimpleName());
        List<String> peerL3Vips = null;
        if (vr.getGuestL3Networks() != null && !vr.getGuestL3Networks().isEmpty()) {
            peerL3Vips = Q.New(VipPeerL3NetworkRefVO.class).select(VipPeerL3NetworkRefVO_.vipUuid)
                    .in(VipPeerL3NetworkRefVO_.l3NetworkUuid, vr.getGuestL3Networks())
                    .listValues();
        }

        Set<String> vipUuids = null;
        if (vrVips != null && !vrVips.isEmpty()) {
            vipUuids = new HashSet<>(vrVips);
        }

        if (peerL3Vips != null && !peerL3Vips.isEmpty()) {
            if (vipUuids == null) {
                vipUuids = new HashSet<>(peerL3Vips);
            } else {
                vipUuids.addAll(new HashSet<String>(peerL3Vips));
            }
        }

        if (vipUuids == null || vipUuids.isEmpty()) {
            chain.next();
            return;
        }

        List<String> vipUuidss = new ArrayList<>(vipUuids);
        vipUuidss = vrMgr.getVirtualRouterVips(vr.getUuid(), vipUuidss);
        if (vipUuidss.isEmpty()) {
            chain.next();
            return;
        }

        /*just only vrouter vip and skip the flat vip*/
        List<VipVO> vips = Q.New(VipVO.class)
                            .in(VipVO_.serviceProvider, Arrays.asList(VyosConstants.PROVIDER_TYPE.toString(),
                                VirtualRouterConstant.PROVIDER_TYPE.toString()))
                            .in(VipVO_.uuid, vipUuidss).list();
        if (vips.isEmpty()) {
            chain.next();
            return;
        }

        List<VipInventory> invs = VipInventory.valueOf(vips);

        vipExt.createVipOnVirtualRouterVm(vr, invs, true, new Completion(chain) {
            @Override
            public void success() {
                proxy.attachNetworkService(vr.getUuid(), VipVO.class.getSimpleName(), vips.stream().map(VipVO::getUuid).collect(Collectors.toList()));
                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                chain.fail(errorCode);
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        chain.rollback();
    }
}
