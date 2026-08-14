package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.UsedIpVO_;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.identity.Account;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;
import org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterCreatePublicVipFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected VipConfigProxy vipConfigProxy;
    @Autowired
    protected VirtualRouterManager vrMgr;
    @Autowired
    private NetworkServiceManager nwServiceMgr;

    private final static CLogger logger = Utils.getLogger(VirtualRouterCreatePublicVipFlow.class);

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        VmNicInventory nic = (VmNicInventory) data.get(VirtualRouterConstant.Param.VR_NIC.toString());
        Boolean snat = (Boolean) data.get(VirtualRouterConstant.Param.SNAT.toString());
        boolean isNewCreated = data.containsKey(VirtualRouterConstant.Param.IS_NEW_CREATED.toString());

        /* this flow will be called when:
        *  1. virtual router post create flows, including ha virtual router, non-ha virtual router
        *  2. after attach nic flow, including ha virtual router, non-ha virtual router */
        if (isNewCreated && vr.isHaEnabled()) {
            /* ha public vip is created in ha code */
            chain.next();
            return;
        }

        if (!VirtualRouterNicMetaData.isPublicNic(nic) && !VirtualRouterNicMetaData.isAddinitionalPublicNic(nic)) {
            logger.debug(String.format("nic is not in public network [uuid:%s]", nic.getL3NetworkUuid()));
            chain.next();
            return;
        }

        String vipIp = null, vipIp6 = null;
        VmNicInventory publicNic = nic;
        if (nic.getL3NetworkUuid().equals(vr.getManagementNetworkUuid()) && vr.isHaEnabled()) {
            publicNic = vrMgr.getSnatPubicInventory(vr);
        }
        for (UsedIpInventory ip : publicNic.getUsedIps()) {
            if (ip.getIpVersion() == IPv6Constants.IPv4) {
                vipIp = ip.getIp();
            } else {
                vipIp6 = ip.getIp();
            }
        }

        boolean vip4Created = true;
        boolean vip6Created = true;
        if (vipIp != null) {
            vip4Created = Q.New(VipVO.class).eq(VipVO_.ip, vipIp).eq(VipVO_.l3NetworkUuid, nic.getL3NetworkUuid()).isExists();
            if (vip4Created) {
                VipVO vip = Q.New(VipVO.class).eq(VipVO_.ip, vipIp).eq(VipVO_.l3NetworkUuid, nic.getL3NetworkUuid()).find();
                vip.setSystem(true);
                vip.setUsedIpUuid(publicNic.getUsedIpUuid());
                dbf.updateAndRefresh(vip);
                vipConfigProxy.attachNetworkService(vr.getUuid(), VipVO.class.getSimpleName(), Arrays.asList(vip.getUuid()));
            }
        }
        if (vipIp6 != null) {
            vip6Created = Q.New(VipVO.class).eq(VipVO_.ip, vipIp6).eq(VipVO_.l3NetworkUuid, nic.getL3NetworkUuid()).isExists();
            if (vip6Created) {
                VipVO vip = Q.New(VipVO.class).eq(VipVO_.ip, vipIp6).eq(VipVO_.l3NetworkUuid, nic.getL3NetworkUuid()).find();
                vip.setSystem(true);
                vip.setUsedIpUuid(publicNic.getUsedIpUuid());
                dbf.updateAndRefresh(vip);
                vipConfigProxy.attachNetworkService(vr.getUuid(), VipVO.class.getSimpleName(), Arrays.asList(vip.getUuid()));
            }
        }
        if (vip4Created && vip6Created) {
            logger.debug(String.format("vip [ip:%s] in l3 network [uuid:%s] already created", vipIp, nic.getL3NetworkUuid()));
            chain.next();
            return;
        }

        L3NetworkVO publicL3 = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
        List<CreateVipMsg> msgs = new ArrayList<>();
        for (Integer ipVersion : publicL3.getIpVersions()) {
            if (ipVersion == IPv6Constants.IPv4 && vipIp != null && vip4Created) {
                continue;
            }

            if (ipVersion == IPv6Constants.IPv6 && vipIp6 != null && vip6Created) {
                continue;
            }

            CreateVipMsg cmsg = new CreateVipMsg();
            if (ipVersion == IPv6Constants.IPv4) {
                cmsg.setName(String.format("vip-for-%s", vr.getName()));
            } else {
                cmsg.setName(String.format("vip6-for-%s", vr.getName()));
            }
            cmsg.setL3NetworkUuid(nic.getL3NetworkUuid());
            if (ipVersion == IPv6Constants.IPv4 && vipIp != null) {
                cmsg.setRequiredIp(vipIp);
            } else if (ipVersion == IPv6Constants.IPv6 && vipIp6 != null) {
                cmsg.setRequiredIp(vipIp6);
            }
            cmsg.setIpVersion(ipVersion);
            cmsg.setSystem(true);
            String accountUuid = Account.getAccountUuidOfResource(vr.getUuid());
            SessionInventory session = new SessionInventory();
            session.setAccountUuid(accountUuid);
            cmsg.setSession(session);
            bus.makeTargetServiceIdByResourceUuid(cmsg, VipConstant.SERVICE_ID, nic.getUsedIpUuid());
            msgs.add(cmsg);
        }

        List<ErrorCode> errs = new ArrayList<>();
        List<VipInventory> vips = new ArrayList<>();
        /* use for rollback */
        data.put(VirtualRouterConstant.Param.PUB_VIP_UUID.toString(), vips);
        new While<>(msgs).each((msg, wcoml) -> {
            bus.send(msg, new CloudBusCallBack(wcoml) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errs.add(reply.getError());
                        wcoml.allDone();
                        return;
                    }

                    CreateVipReply r = reply.castReply();
                    VipInventory vipInventory = r.getVip();
                    vips.add(vipInventory);
                    vipConfigProxy.attachNetworkService(vr.getUuid(), VipVO.class.getSimpleName(), Arrays.asList(vipInventory.getUuid()));
                    wcoml.done();
                }
            });
        }).run(new WhileDoneCompletion(chain) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (snat != null && !snat) {
                    logger.debug(String.format("SNAT is not enabled on virtual router [uuid:%s]", vr.getUuid()));
                    chain.next();
                    return;
                }

                /* if there is no guest nic, don't apply snat */
                boolean hasGuestNic = false;
                for (VmNicInventory nic : vr.getVmNics()) {
                    if (VirtualRouterNicMetaData.isGuestNic(nic)) {
                        hasGuestNic = true;
                    }
                }
                if (!hasGuestNic) {
                    chain.next();
                    return;
                }

                /* only ipv4 has snat */
                VipInventory ipv4Vip = null;
                for (VipInventory vip : vips) {
                    if (NetworkUtils.isIpv4Address(vip.getIp())) {
                        ipv4Vip = vip;
                    }
                }
                if (ipv4Vip == null) {
                    chain.next();
                    return;
                }

                /* only default route network nic ip will apply snat */
                if (!ipv4Vip.getL3NetworkUuid().equals(vr.getDefaultRouteL3NetworkUuid())) {
                    chain.next();
                    return;
                }

                ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                struct.setUseFor(VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE);
                struct.setServiceUuid(vr.getUuid());
                String l3NetworkUuuid = vr.getGuestL3Networks().get(0);
                try {
                    NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(l3NetworkUuuid, NetworkServiceType.SNAT);
                    struct.setPeerL3NetworkUuid(l3NetworkUuuid);
                    struct.setServiceProvider(providerType.toString());
                } catch (OperationFailureException e){
                    logger.debug(String.format("Get providerType exception %s", e.toString()));
                }
                Vip vip = new Vip(ipv4Vip.getUuid());
                vip.setStruct(struct);
                vip.acquire(new Completion(chain) {
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
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        List<VipInventory> vips = (List<VipInventory>) data.get(VirtualRouterConstant.Param.PUB_VIP_UUID.toString());
        if (vips == null || vips.isEmpty()) {
            chain.rollback();
            return;
        }

        new While<>(vips).each((vip, wcomp) -> {
                    VipDeletionMsg dmsg = new VipDeletionMsg();
                    dmsg.setVipUuid(vip.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, VipConstant.SERVICE_ID, vip.getUuid());
                    bus.send(dmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            wcomp.done();

                        }
                    });
                }
        ).run(new WhileDoneCompletion(chain) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                chain.rollback();
            }
        });
    }
}
