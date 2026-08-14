package org.zstack.vpc.ha;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.*;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vpc.*;
import org.zstack.header.vpc.ha.*;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.lb.LoadBalancerVO;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.eip.VirtualRouterEipRefVO;
import org.zstack.network.service.virtualrouter.eip.VirtualRouterEipRefVO_;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO_;
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefVO;
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefVO_;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO_;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.vpc.VpcStateEvent;
import org.zstack.vpc.VpcSystemTags;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;


/**
 * Created by shixin.ruan 2019/05/10
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class vpcHaGroupApplianceVmImpl implements ApplianceVmHaExtensionPoint, ApplianceVmSyncConfigToHaGroupExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private ApplianceVmFacade apvf;
    @Autowired
    private VirutalRouterDefaultL3ConfigProxy defaultL3ConfigProxy;

    @Override
    @Deferred
    public void attachVirtualRouterToHaGroup(String vrUuid, String haGroupUuid) {
        GLock lock = new GLock(String.format("attach-vr-to-ha-group-%s", haGroupUuid), TimeUnit.MINUTES.toSeconds(30));
        lock.lock();
        Defer.defer(lock::unlock);

        VpcHaGroupVO haGroupVO = dbf.findByUuid(haGroupUuid, VpcHaGroupVO.class);
        DebugUtils.Assert(haGroupVO.getVrs().size() <= 1, "can not create more than 2 vpc router in same group");

        if (!Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.uuid, vrUuid)
                .eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, haGroupUuid).isExists()){
            VpcHaGroupApplianceVmRefVO ref = new VpcHaGroupApplianceVmRefVO();
            ref.setUuid(vrUuid);
            ref.setVpcHaRouterUuid(haGroupUuid);
            dbf.persist(ref);
        }
    }

    @Override
    public void detachVirtualRouterFromHaGroup(String vrUuid, String haGroupUuid) {
        SQL.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.uuid, vrUuid)
                .eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, haGroupUuid).delete();
    }

    @Override
    @Deferred
    public void createApplianceVmHaSpec(ApplianceVmSpec aspec, String offeringUuid, String haUuid) {
        // TODO: ui will call APICreateVpcVRouterMsg at same time, we need a lock here
        GLock lock = new GLock(String.format("create-appliance-for-ha-group-%s", haUuid), TimeUnit.MINUTES.toSeconds(30));
        lock.lock();
        Defer.defer(lock::unlock);

        VirtualRouterOfferingVO offering = dbf.findByUuid(offeringUuid, VirtualRouterOfferingVO.class);
        VpcHaGroupVO haGroupVO = dbf.findByUuid(haUuid, VpcHaGroupVO.class);
        DebugUtils.Assert(haGroupVO.getVrs().size() <= 1, "can not create more than 2 vpc router in same group");
        VpcRouterVmVO vpcVo = null;
        if (haGroupVO.getVrs().size() > 0) {
            vpcVo = dbf.findByUuid(VpcHaGroupInventory.valueOf(haGroupVO).getVrRefs().get(0).getUuid(), VpcRouterVmVO.class);
        }

        if (!Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK).isExists()) {
            VpcHaGroupNetworkServiceRefVO refVO = new VpcHaGroupNetworkServiceRefVO();
            refVO.setVpcHaRouterUuid(haUuid);
            refVO.setNetworkServiceName(VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK);
            refVO.setNetworkServiceUuid(offering.getPublicNetworkUuid());
            dbf.persist(refVO);
        }

        String affinityGroupUuid = VpcHaGroupSystemTags.VPCHA_ROUTER_AFFINITYGROUP.getTokenByResourceUuid(haUuid,
                VpcHaGroupSystemTags.VPCHA_ROUTER_AFFINITYGROUP_TOKEN);

        ApplianceVmHaSpec haSpec = new ApplianceVmHaSpec();
        haSpec.setHaUuid(haUuid);
        haSpec.setAffinityGroupUuid(affinityGroupUuid);
        aspec.setHaSpec(haSpec);

        if (offering.getPublicNetworkUuid() != null) {
            /* if case 1: mgt is same to public network, allocate a new Ip as vip
            *  if case 2: mgt is different from public network, allocate a new Ip as vip and as pub nic ip */
            L3NetworkVO pubL3VO = dbf.findByUuid(offering.getPublicNetworkUuid(), L3NetworkVO.class);
            List<String> publicIp = new ArrayList<>();
            List<String> vipUuids = new VpcHaGroupOperator().getVpcHaGroupSystemVipUuids(haUuid);
            if (!vipUuids.isEmpty()) {
                publicIp = Q.New(VipVO.class).eq(VipVO_.system, true).in(VipVO_.uuid, vipUuids)
                        .eq(VipVO_.l3NetworkUuid, offering.getPublicNetworkUuid()).select(VipVO_.ip).listValues();
            }
            if (publicIp.isEmpty()) {
                /* allocate a new vip */
                for (Integer ipversion : pubL3VO.getIpVersions()) {
                    CreateVipMsg cmsg = new CreateVipMsg();
                    cmsg.setName(String.format("ha-vip%d-for-%s", ipversion, haGroupVO.getName()));
                    String l3pubNetworkUuid = offering.getPublicNetworkUuid();
                    cmsg.setL3NetworkUuid(l3pubNetworkUuid);
                    if (aspec.getStaticVip().get(l3pubNetworkUuid) != null) {
                        cmsg.setRequiredIp(aspec.getStaticVip().get(l3pubNetworkUuid).get(ipversion));
                    }
		            cmsg.setIpVersion(ipversion);
                    String accountUuid = aspec.getAccountUuid();
                    SessionInventory session = new SessionInventory();
                    session.setAccountUuid(accountUuid);
                    cmsg.setSession(session);
                    cmsg.setSystem(true);
                    bus.makeTargetServiceIdByResourceUuid(cmsg, VipConstant.SERVICE_ID, offering.getPublicNetworkUuid());
                    MessageReply reply = bus.call(cmsg);
                    if (!reply.isSuccess()) {
                        throw new OperationFailureException(reply.getError());
                    }
                    CreateVipReply r = reply.castReply();
                    VipInventory vip = r.getVip();
                    if (ipversion == 4) {
                        SQL.New(VipVO.class).eq(VipVO_.uuid, r.getVip().getUuid()).set(VipVO_.serviceProvider, VyosConstants.PROVIDER_TYPE.toString())
                                .set(VipVO_.useFor, NetworkServiceType.SNAT.toString()).update();
                    } else {
                        SQL.New(VipVO.class).eq(VipVO_.uuid, r.getVip().getUuid()).set(VipVO_.serviceProvider, VyosConstants.PROVIDER_TYPE.toString()).update();
                    }
                    if (ipversion == 4) {
                        if (!Q.New(VipNetworkServicesRefVO.class)
                                .eq(VipNetworkServicesRefVO_.uuid, haUuid)
                                .eq(VipNetworkServicesRefVO_.vipUuid, r.getVip().getUuid())
                                .eq(VipNetworkServicesRefVO_.serviceType, NetworkServiceType.SNAT.toString()).isExists()) {
                            VipNetworkServicesRefVO vipRef = new VipNetworkServicesRefVO();
                            vipRef.setUuid(haUuid);
                            vipRef.setServiceType(NetworkServiceType.SNAT.toString());
                            vipRef.setVipUuid(r.getVip().getUuid());
                            dbf.persist(vipRef);
                        }
                    }

                    VpcHaGroupNetworkServiceRefVO nref = new VpcHaGroupNetworkServiceRefVO();
                    nref.setVpcHaRouterUuid(haUuid);
                    nref.setNetworkServiceName(VipVO.class.getSimpleName());
                    nref.setNetworkServiceUuid(vip.getUuid());
                    dbf.persist(nref);
                    publicIp.add(vip.getIp());
                }
            }

            if (!offering.getManagementNetworkUuid().equals(offering.getPublicNetworkUuid())) {
                L3NetworkInventory pnw = L3NetworkInventory.valueOf(dbf.findByUuid(offering.getPublicNetworkUuid(), L3NetworkVO.class));
                ApplianceVmNicSpec pnicSpec = new ApplianceVmNicSpec();
                pnicSpec.setL3NetworkUuid(pnw.getUuid());
                pnicSpec.setMetaData(VirtualRouterNicMetaData.PUBLIC_NIC_MASK.toString());
                Map<Integer, String> staticIp = new HashMap<>();
                for (String ip : publicIp) {
                    if (NetworkUtils.isIpv4Address(ip)) {
                        staticIp.put(IPv6Constants.IPv4, ip);
                    } else if (IPv6NetworkUtils.isIpv6Address(ip)) {
                        staticIp.put(IPv6Constants.IPv6, ip);
                    }
                }
                pnicSpec.setStaticIp(staticIp);
                pnicSpec.setAllowDuplicatedAddress(true);

                aspec.getAdditionalNics().add(pnicSpec);
                aspec.setDefaultRouteL3Network(pnw);
            }
        }

        /* add additional nics */
        List<String> l3Uuids = Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, L3NetworkVO.class.getSimpleName())
                .select(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid).listValues();
        for (String uuid : l3Uuids) {
            VmNicVO oldNic = null;
            if (vpcVo != null) {
                for (VmNicVO nic : vpcVo.getVmNics()) {
                    if (nic.getL3NetworkUuid().equals(uuid)) {
                        oldNic = nic;
                        break;
                    }
                }
            }

            ApplianceVmNicSpec pnicSpec = new ApplianceVmNicSpec();
            pnicSpec.setL3NetworkUuid(uuid);
            L3NetworkVO l3NetworkVO = dbf.findByUuid(uuid, L3NetworkVO.class);
            if (l3NetworkVO.getCategory() == L3NetworkCategory.Public) {
                pnicSpec.setMetaData(VirtualRouterNicMetaData.ADDITIONAL_PUBLIC_NIC_MASK.toString());
            } else {
                pnicSpec.setMetaData(VirtualRouterNicMetaData.GUEST_NIC_MASK.toString());
            }
            if (oldNic != null) {
                Map<Integer, String> staticIp = new HashMap<>();
                for (UsedIpVO ip : oldNic.getUsedIps()) {
                    staticIp.put(ip.getIpVersion(), ip.getIp());
                }
                pnicSpec.setStaticIp(staticIp);
                pnicSpec.setAllowDuplicatedAddress(true);
            }

            aspec.getAdditionalNics().add(pnicSpec);
        }

        List<String> defaultL3s = defaultL3ConfigProxy.getServiceUuidsByHaGrupUuid(haUuid,
                VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK);
        if (!defaultL3s.isEmpty()) {
            aspec.setDefaultRouteL3Network(L3NetworkInventory.valueOf(dbf.findByUuid(defaultL3s.get(0), L3NetworkVO.class)));
        }
    }

    @Override
    @Deferred
    public void createApplianceVmHaSpecRollBack(ApplianceVmSpec aspec, String offeringUuid, String haUuid, NoErrorCompletion completion) {
        // TODO: ui will call APICreateVpcVRouterMsg at same time, we need a lock here
        GLock lock = new GLock(String.format("create-appliance-for-ha-group-%s-rollback", haUuid), TimeUnit.MINUTES.toSeconds(30));
        lock.lock();
        Defer.defer(lock::unlock);

        if (Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, haUuid).count() > 0) {
            completion.done();
            return;
        }

        /* only rollback system vip */
        List<String> vipUuids = new VpcHaGroupOperator().getVpcHaGroupSystemVipUuids(haUuid);
        if (vipUuids.isEmpty()) {
            SQL.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid).delete();
            completion.done();
            return;
        }

        new While<>(vipUuids).each((uuid, compl) -> {
            VipDeletionMsg rmsg = new VipDeletionMsg();
            rmsg.setVipUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(rmsg, VipConstant.SERVICE_ID, uuid);
            bus.send(rmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                SQL.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid).delete();
                completion.done();
            }
        });
    }

    @Override
    public void applianceVmSyncConfigToHa(ApplianceVmInventory inv, String haUuid) {
        VpcRouterVmVO vpcVo = dbf.findByUuid(inv.getUuid(), VpcRouterVmVO.class);
        if (vpcVo == null) {
            return;
        }

        /* add to haGroup  */
        attachVirtualRouterToHaGroup(inv.getUuid(), haUuid);

        /* add to new affinityGroup */
        String affinityGroupUuid = VpcHaGroupSystemTags.VPCHA_ROUTER_AFFINITYGROUP.getTokenByResourceUuid(haUuid,
                VpcHaGroupSystemTags.VPCHA_ROUTER_AFFINITYGROUP_TOKEN);
        apvf.dettachVmInstanceFromAffinityGroup(inv.getUuid());
        apvf.attachApplianceVmToAffinityGroup(inv.getUuid(), affinityGroupUuid);

        List<VpcHaGroupNetworkServiceRefVO> refs = new ArrayList<>();
        /* copy l3 network */
        for (VmNicVO nic : vpcVo.getVmNics()) {
            /* management and public network is skipped */
            if (nic.getL3NetworkUuid().equals(vpcVo.getPublicNetworkUuid()) || nic.getL3NetworkUuid().equals(vpcVo.getManagementNetworkUuid())) {
                continue;
            }

            if (Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, L3NetworkVO.class.getSimpleName())
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, nic.getL3NetworkUuid()).isExists()) {
                continue;
            }

            VpcHaGroupNetworkServiceRefVO ref = new VpcHaGroupNetworkServiceRefVO();
            ref.setVpcHaRouterUuid(haUuid);
            ref.setNetworkServiceName(L3NetworkVO.class.getSimpleName());
            ref.setNetworkServiceUuid(nic.getL3NetworkUuid());
            refs.add(ref);
        }

        /* copy vip */
        List<String> vipUuids = vpcVo.getVirtualRouterVips().stream().map(VirtualRouterVipVO::getUuid).distinct().collect(Collectors.toList());
        for (String uuid : vipUuids) {
            if (Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VipVO.class.getSimpleName())
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, uuid).isExists()) {
                continue;
            }

            VpcHaGroupNetworkServiceRefVO ref = new VpcHaGroupNetworkServiceRefVO();
            ref.setVpcHaRouterUuid(haUuid);
            ref.setNetworkServiceName(VipVO.class.getSimpleName());
            ref.setNetworkServiceUuid(uuid);
            refs.add(ref);
        }

        /* copy eip */
        List<String> eips = Q.New(VirtualRouterEipRefVO.class)
                .eq(VirtualRouterEipRefVO_.virtualRouterVmUuid, inv.getUuid())
                .select(VirtualRouterEipRefVO_.eipUuid).listValues();
        for (String uuid : eips) {
            if (Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, EipVO.class.getSimpleName())
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, uuid).isExists()) {
                continue;
            }

            VpcHaGroupNetworkServiceRefVO ref = new VpcHaGroupNetworkServiceRefVO();
            ref.setVpcHaRouterUuid(haUuid);
            ref.setNetworkServiceName(EipVO.class.getSimpleName());
            ref.setNetworkServiceUuid(uuid);
            refs.add(ref);
        }

        /* copy pf */
        List<String> pfs = Q.New(VirtualRouterPortForwardingRuleRefVO.class)
                .eq(VirtualRouterPortForwardingRuleRefVO_.virtualRouterVmUuid, inv.getUuid())
                .select(VirtualRouterPortForwardingRuleRefVO_.uuid).listValues();
        for (String uuid : pfs) {
            if (Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, PortForwardingRuleVO.class.getSimpleName())
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, uuid).isExists()) {
                continue;
            }

            VpcHaGroupNetworkServiceRefVO ref = new VpcHaGroupNetworkServiceRefVO();
            ref.setVpcHaRouterUuid(haUuid);
            ref.setNetworkServiceName(PortForwardingRuleVO.class.getSimpleName());
            ref.setNetworkServiceUuid(uuid);
            refs.add(ref);
        }

        /* copy lb */
        List<String> lbs = Q.New(VirtualRouterLoadBalancerRefVO.class)
                .eq(VirtualRouterLoadBalancerRefVO_.virtualRouterVmUuid, inv.getUuid())
                .select(VirtualRouterLoadBalancerRefVO_.loadBalancerUuid).listValues();
        for (String uuid : lbs) {
            if (Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, LoadBalancerVO.class.getSimpleName())
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, uuid).isExists()) {
                continue;
            }

            VpcHaGroupNetworkServiceRefVO ref = new VpcHaGroupNetworkServiceRefVO();
            ref.setVpcHaRouterUuid(haUuid);
            ref.setNetworkServiceName(LoadBalancerVO.class.getSimpleName());
            ref.setNetworkServiceUuid(uuid);
            refs.add(ref);
        }

        /* ipsec， ipsec associated with L3NetworkVO, no need to associated to ha group */

        /* vipqos, no need to associated to ha group */

        /* dns */
        List<String> dnses = vpcVo.getDns().stream().sorted(Comparator.comparingLong(VpcRouterDnsVO::getId))
                .map(VpcRouterDnsVO::getDns).distinct().collect(Collectors.toList());
        for (String dns : dnses) {
            if (Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VpcRouterDnsVO.class.getSimpleName())
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, dns).isExists()) {
                continue;
            }

            VpcHaGroupNetworkServiceRefVO ref = new VpcHaGroupNetworkServiceRefVO();
            ref.setVpcHaRouterUuid(haUuid);
            ref.setNetworkServiceName(VpcRouterDnsVO.class.getSimpleName());
            ref.setNetworkServiceUuid(dns);
            refs.add(ref);
        }

        /* snat disable */
        if(Q.New(VpcSnatStateVO.class).eq(VpcSnatStateVO_.state, VpcStateEvent.enable.toString()).eq(VpcSnatStateVO_.vpcUuid, inv.getUuid()).isExists()){
            Q.New(VpcSnatStateVO.class).select(VpcSnatStateVO_.l3NetworkUuid).eq(VpcSnatStateVO_.state, VpcStateEvent.enable.toString())
                    .eq(VpcSnatStateVO_.vpcUuid, inv.getUuid()).listValues().forEach(l3Uuid -> {
                if (!Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid)
                        .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, NetworkServiceType.SNAT.toString())
                        .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, l3Uuid).isExists()) {
                    VpcHaGroupNetworkServiceRefVO ref = new VpcHaGroupNetworkServiceRefVO();
                    ref.setVpcHaRouterUuid(haUuid);
                    ref.setNetworkServiceName(NetworkServiceType.SNAT.toString());
                    ref.setNetworkServiceUuid((String) l3Uuid);
                    refs.add(ref);
                }
            });
        }

        /* dr */
        String state = VpcSystemTags.VPC_DISTRIBUTED_ROUTING_ENABLED.getTokenByResourceUuid(
                inv.getUuid(), VirtualRouterVmVO.class, VpcSystemTags.VPC_DISTRIBUTED_ROUTING_ENABLED_TOKEN);
        if (state != null) {
            if (!Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName,VpcConstants.VR_DR_STATE)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, state).isExists()) {
                VpcHaGroupNetworkServiceRefVO ref = new VpcHaGroupNetworkServiceRefVO();
                ref.setVpcHaRouterUuid(haUuid);
                ref.setNetworkServiceName(VpcConstants.VR_DR_STATE);
                ref.setNetworkServiceUuid(state);
                refs.add(ref);
            }
        }

        /* other network service(like route table, ospf) implement in their owner code */

        if (!refs.isEmpty()) {
            dbf.persistCollection(refs);
        }
    }

    @Override
    public void applianceVmSyncConfigToHaRollback(ApplianceVmInventory inv, String haUuid) {
        /* detach from haGroup  */
        detachVirtualRouterFromHaGroup(inv.getUuid(), haUuid);

        /* add to default affinityGroup */
        apvf.dettachVmInstanceFromAffinityGroup(inv.getUuid());
        apvf.attachApplianceVmToAffinityGroup(inv.getUuid(), null);

        SQL.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid).delete();
    }

    @Override
    public void applianceVmSyncConfigAfterAddToHaGroup(ApplianceVmInventory inv, String haUuid, NoErrorCompletion completion) {
        SystemTagCreator creator = ApplianceVmSystemTags.APPLIANCEVM_HA_UUID.newSystemTagCreator(inv.getUuid());
        creator.setTagByTokens(map(e(
                ApplianceVmSystemTags.APPLIANCEVM_HA_UUID_TOKEN,
                haUuid
        )));
        creator.recreate = true;
        creator.create();

        /* delete refs */
        SQL.New(VirtualRouterVipVO.class).eq(VirtualRouterVipVO_.virtualRouterVmUuid, inv.getUuid()).delete();
        SQL.New(VirtualRouterEipRefVO.class).eq(VirtualRouterEipRefVO_.virtualRouterVmUuid, inv.getUuid()).delete();
        SQL.New(VirtualRouterPortForwardingRuleRefVO.class).eq(VirtualRouterPortForwardingRuleRefVO_.virtualRouterVmUuid, inv.getUuid()).delete();
        SQL.New(VirtualRouterLoadBalancerRefVO.class).eq(VirtualRouterLoadBalancerRefVO_.virtualRouterVmUuid, inv.getUuid()).delete();
        SQL.New(VpcRouterDnsVO.class).eq(VpcRouterDnsVO_.vpcRouterUuid, inv.getUuid()).delete();
        SQL.New(VpcSnatStateVO.class).eq(VpcSnatStateVO_.vpcUuid, inv.getUuid()).delete();

        completion.done();
    }
}
