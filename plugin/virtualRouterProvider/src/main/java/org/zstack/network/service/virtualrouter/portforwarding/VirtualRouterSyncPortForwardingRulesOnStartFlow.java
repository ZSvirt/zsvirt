package org.zstack.network.service.virtualrouter.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.portforwarding.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SyncPortForwardingRuleCmd;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SyncPortForwardingRuleRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncPortForwardingRulesOnStartFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VirtualRouterSyncPortForwardingRulesOnStartFlow.class);
    
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private PortForwardingConfigProxy proxy;

    @Transactional
    private List<PortForwardingRuleVO> findRulesForThisRouter(VirtualRouterVmInventory vr, Map<String, Object> data, boolean isNewCreated) {
        if (!isNewCreated) {
            List<String> pfUuids = proxy.getServiceUuidsByRouterUuid(vr.getUuid(), PortForwardingRuleVO.class.getSimpleName());
            if (pfUuids == null || pfUuids.isEmpty()) {
                return new ArrayList<>();
            }

            return Q.New(PortForwardingRuleVO.class).in(PortForwardingRuleVO_.uuid, pfUuids).list();
        } else {
            List<VmNicInventory> guestNics = vr.getGuestNics();
            if (guestNics == null || guestNics.isEmpty()) {
                return new ArrayList<PortForwardingRuleVO>(0);
            }

            List<String> pubL3Uuids = new ArrayList<>();
            pubL3Uuids.add(vr.getPublicNic().getL3NetworkUuid());
            pubL3Uuids.addAll(vr.getAdditionalPublicNics().stream().map(VmNicInventory::getL3NetworkUuid).collect(Collectors.toList()));
            if (!vr.getPublicNic().getL3NetworkUuid().equals(vr.getManagementNetworkUuid())) {
                /* in old code, pf can be configured on management nic */
                pubL3Uuids.add(vr.getManagementNetworkUuid());
            }

            String sql = "select rule from PortForwardingRuleVO rule, VipVO vip, VmNicVO nic where " +
                    " rule.vipUuid = vip.uuid and rule.vmNicUuid = nic.uuid and vip.l3NetworkUuid in (:vipL3Uuids) and nic.l3NetworkUuid in (:guestL3Uuid)";
            TypedQuery<PortForwardingRuleVO> q = dbf.getEntityManager().createQuery(sql, PortForwardingRuleVO.class);
            q.setParameter("vipL3Uuids", pubL3Uuids);
            q.setParameter("guestL3Uuid", guestNics.stream().map(VmNicInventory::getL3NetworkUuid).collect(Collectors.toList()));

            List<PortForwardingRuleVO> rules =  q.getResultList();
            List<String> ruleUuids = rules.stream().map(PortForwardingRuleVO::getUuid).collect(Collectors.toList());

            proxy.attachNetworkService(vr.getUuid(), PortForwardingRuleVO.class.getSimpleName(), ruleUuids);
            data.put(VirtualRouterSyncPortForwardingRulesOnStartFlow.class.getName(), ruleUuids);

            return rules;
        }
    }
    
    @Transactional(readOnly=true)
    private Collection<PortForwardingRuleTO> calculateAllRules(Map<String, PortForwardingRuleVO> ruleMap, String vrUuid) {
        String sql = "select rule.uuid, nic.ip, vip.ip, vip.l3NetworkUuid from PortForwardingRuleVO rule, VmNicVO nic, VipVO vip " +
                " where rule.vmNicUuid = nic.uuid and rule.uuid in (:ruleUuids) and vip.uuid = rule.vipUuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("ruleUuids", ruleMap.keySet());
        List<Tuple> privateIps = q.getResultList();

        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(dbf.findByUuid(vrUuid, VirtualRouterVmVO.class));
        Map<String, PortForwardingRuleTO> tos = new HashMap<String, PortForwardingRuleTO>();
        for (Tuple t : privateIps) {
            String ruleUuid = t.get(0, String.class);
            String publicL3Uuid = t.get(3, String.class);
            PortForwardingRuleTO to = new PortForwardingRuleTO();
            to.setUuid(ruleUuid);
            to.setPrivateIp(t.get(1, String.class));

            PortForwardingRuleVO ruleVO = ruleMap.get(ruleUuid);
            to.setAllowedCidr(ruleVO.getAllowedCidr());
            to.setPrivatePortEnd(ruleVO.getPrivatePortEnd());
            to.setPrivatePortStart(ruleVO.getPrivatePortStart());
            to.setVipPortEnd(ruleVO.getVipPortEnd());
            to.setSnatInboundTraffic(PortForwardingGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
            to.setVipPortStart(ruleVO.getVipPortStart());
            to.setVipIp(t.get(2, String.class));
            to.setProtocolType(ruleVO.getProtocolType().toString());
            Optional<VmNicInventory> pubNic = vr.getVmNics().stream()
                    .filter(n -> n.getL3NetworkUuid().equals(publicL3Uuid))
                    .findFirst();
            if (!pubNic.isPresent()) {
                continue;
            }
            to.setPublicMac(pubNic.get().getMac());
            tos.put(ruleUuid, to);
        }
        
        assert tos.size() == ruleMap.size();
        
        sql = "select rule.uuid, vrnic.mac from PortForwardingRuleVO rule, VmNicVO vrnic, VmNicVO nic2, ApplianceVmVO vr " +
                " where vr.uuid = vrnic.vmInstanceUuid and vrnic.l3NetworkUuid = nic2.l3NetworkUuid and nic2.uuid = rule.vmNicUuid " +
                " and rule.uuid in (:ruleUuids) and vr.uuid = :vrUuid";
        TypedQuery<Tuple> privateMacQuery = dbf.getEntityManager().createQuery(sql, Tuple.class);
        privateMacQuery.setParameter("ruleUuids", ruleMap.keySet());
        privateMacQuery.setParameter("vrUuid", vrUuid);
        List<Tuple> privateMacs = privateMacQuery.getResultList();
        for (Tuple t: privateMacs) {
            String ruleUuid = t.get(0, String.class);
            PortForwardingRuleTO to = tos.get(ruleUuid);
            to.setPrivateMac(t.get(1, String.class));
        }
        
        return tos.values();
    }
    
    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        List<VmNicInventory> guestNics = vr.getGuestNics();
        if (guestNics == null || guestNics.isEmpty()) {
            chain.next();
            return;
        }
        List<String> l3Uuids = guestNics.stream().map(n -> n.getL3NetworkUuid()).collect(Collectors.toList());
        if (!vrMgr.isL3NetworksNeedingNetworkServiceByVirtualRouter(l3Uuids, PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE)) {
            chain.next();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(vr.getUuid()) && !VirtualRouterSystemTags.VR_PORT_FORWARDING_ROLE.hasTag(vr.getUuid())) {
            chain.next();
            return;
        }

        new VirtualRouterRoleManager().makePortForwardingRole(vr.getUuid());

        boolean isNewCreated = data.containsKey(Param.IS_NEW_CREATED.toString());

        List<PortForwardingRuleVO> ruleVOs = findRulesForThisRouter(vr, data, isNewCreated);
        if (ruleVOs.isEmpty()) {
            chain.next();
            return;
        }

        Map<String, PortForwardingRuleVO> ruleMap = new HashMap<String, PortForwardingRuleVO>(ruleVOs.size());
        for (PortForwardingRuleVO rvo : ruleVOs) {
            ruleMap.put(rvo.getUuid(), rvo);
        }

        Collection<PortForwardingRuleTO> tos = calculateAllRules(ruleMap, vr.getUuid());
        List<PortForwardingRuleTO> toList = new ArrayList<PortForwardingRuleTO>(tos.size());
        toList.addAll(tos);

        SyncPortForwardingRuleCmd cmd = new SyncPortForwardingRuleCmd();
        cmd.setRules(toList);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_SYNC_PORT_FORWARDING);
        msg.setVmInstanceUuid(vr.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    chain.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                SyncPortForwardingRuleRsp ret = re.toResponse(SyncPortForwardingRuleRsp.class);
                if (ret.isSuccess()) {
                    String info = String.format("successfully sync port forwarding rules served by virtual router[name: %s uuid: %s]",
                            vr.getName(), vr.getUuid());
                    logger.debug(info);
                    chain.next();
                } else {
                    ErrorCode err = operr("failed to sync port forwarding rules served by virtual router[name: %s, uuid: %s], because %s",
                            vr.getName(), vr.getUuid(), ret.getError());
                    chain.fail(err);
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        List<String> ruleUuids = (List<String>) data.get(VirtualRouterSyncPortForwardingRulesOnStartFlow.class.getName());
        if (ruleUuids != null) {
            proxy.detachNetworkService(vr.getUuid(), PortForwardingRuleVO.class.getSimpleName(), ruleUuids);
        }

        chain.rollback();
    }
}
