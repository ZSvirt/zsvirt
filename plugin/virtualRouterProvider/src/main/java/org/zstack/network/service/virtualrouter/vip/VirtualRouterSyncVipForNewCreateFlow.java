package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.portforwarding.PortForwardingConstant;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterSystemTags;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncVipForNewCreateFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    @Qualifier("VirtualRouterVipBackend")
    protected VirtualRouterVipBackend vipExt;
    @Autowired
    protected VirtualRouterManager vrMgr;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        final List<VmNicInventory> guestNics = vr.getGuestNics();
        final VmNicInventory publicNic = vr.getPublicNic();
        List<String> l3Uuids = guestNics.stream().map(n -> n.getL3NetworkUuid()).collect(Collectors.toList());
        List<String> vipUuids = new ArrayList<String>();
        if (vrMgr.isL3NetworksNeedingNetworkServiceByVirtualRouter(l3Uuids, EipConstant.EIP_NETWORK_SERVICE_TYPE) &&
                !(VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(vr.getUuid()) && !VirtualRouterSystemTags.VR_EIP_ROLE.hasTag(vr.getUuid()))) {
            vipUuids.addAll(new Callable<List<String>>() {
                @Override
                @Transactional(readOnly = true)
                public List<String> call() {
                    String sql = "select vip.uuid from EipVO eip, VmNicVO nic, VipVO vip, VmInstanceVO vm where vm.uuid = nic.vmInstanceUuid "+
                            " and eip.vipUuid = vip.uuid and eip.vmNicUuid = nic.uuid and vip.l3NetworkUuid = :vipL3Uuid "
                            +" and nic.l3NetworkUuid in (:guestL3Uuid)";
                    TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                    q.setParameter("vipL3Uuid", publicNic.getL3NetworkUuid());
                    q.setParameter("guestL3Uuid", l3Uuids);
                    return q.getResultList();
                }
            }.call());
        }
        if (vrMgr.isL3NetworksNeedingNetworkServiceByVirtualRouter(l3Uuids, PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE) &&
                !(VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(vr.getUuid()) && !VirtualRouterSystemTags.VR_PORT_FORWARDING_ROLE.hasTag(vr.getUuid()))) {
            vipUuids.addAll(new Callable<List<String>>() {
                @Override
                @Transactional(readOnly = true)
                public List<String> call() {
                    String sql = "select vip.uuid from PortForwardingRuleVO rule, VmNicVO nic, VipVO vip, VmInstanceVO vm where " +
                            " vm.uuid = nic.vmInstanceUuid and rule.vipUuid = vip.uuid and rule.vmNicUuid = nic.uuid " +
                            " and vip.l3NetworkUuid = :vipL3Uuid and nic.l3NetworkUuid in (:guestL3Uuid)";
                    TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                    q.setParameter("vipL3Uuid", publicNic.getL3NetworkUuid());
                    q.setParameter("guestL3Uuid", l3Uuids);
                    return q.getResultList();
                }
            }.call());
        }

        if (vipUuids.isEmpty()) {
            chain.next();
            return;
        }

        List<VirtualRouterVipVO> refs = new ArrayList<VirtualRouterVipVO>();
        for (String vipUuid : vipUuids) {
            VirtualRouterVipVO ref = new VirtualRouterVipVO();
            ref.setUuid(vipUuid);
            ref.setVirtualRouterVmUuid(vr.getUuid());
            refs.add(ref);
        }

        dbf.persistCollection(refs);
        data.put(VirtualRouterSyncVipForNewCreateFlow.class.getName(), refs);

        List<VipVO> vips = dbf.listByPrimaryKeys(vipUuids, VipVO.class);
        List<VipInventory> invs = VipInventory.valueOf(vips);
        vipExt.createVipOnVirtualRouterVm(vr, invs, false, new Completion(chain) {
            @Override
            public void success() {
                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                chain.fail(errorCode);
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        List<VirtualRouterVipVO> refs = (List<VirtualRouterVipVO>) data.get(VirtualRouterSyncVipForNewCreateFlow.class.getName());
        if (refs != null) {
            dbf.removeCollection(refs, VirtualRouterVipVO.class);
        }

        trigger.rollback();
    }
}
