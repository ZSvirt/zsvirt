package org.zstack.network.service.virtualrouter.nat;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVm;
import org.zstack.appliancevm.ApplianceVmFactory;
import org.zstack.appliancevm.ApplianceVmSubTypeFactory;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.RemoveSNATRsp;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class VirtualRouterSnatBackend extends AbstractVirtualRouterBackend implements
        NetworkServiceSnatBackend, VirtualRouterAfterAttachNicExtensionPoint, VirtualRouterBeforeDetachNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VirtualRouterSnatBackend.class);

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected VirtualRouterHaBackend haBackend;
    @Autowired
    private ApplianceVmFactory apvmFactory;

    @Override
    public NetworkServiceProviderType getProviderType() {
        return VirtualRouterConstant.PROVIDER_TYPE;
    }

    private void applySnat(final Iterator<SnatStruct> it, final VmInstanceSpec spec, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final SnatStruct struct = it.next();
        final L3NetworkInventory guestL3 = struct.getL3Network();

        VirtualRouterStruct s = new VirtualRouterStruct();
        s.setL3Network(guestL3);
        s.setOfferingValidator(new VirtualRouterOfferingValidator() {
            @Override
            public void validate(VirtualRouterOfferingInventory offering) throws OperationFailureException {
                if (offering.getPublicNetworkUuid().equals(guestL3.getUuid())) {
                    throw new OperationFailureException(operr("guest l3Network[uuid:%s, name:%s] needs SNAT service provided by virtual router, but public l3Network[uuid:%s] of virtual router offering[uuid: %s, name:%s] is the same to this guest l3Network",
                            guestL3.getUuid(), guestL3.getName(), offering.getPublicNetworkUuid(), offering.getUuid(), offering.getName()));
                }
            }
        });

        acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(completion) {
            @Override
            public void success(final VirtualRouterVmInventory vr) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void applySnatService(List<SnatStruct> snatStructList, VmInstanceSpec spec, Completion completion) {
        if (snatStructList.isEmpty()) {
            completion.success();
            return;
        }

        applySnat(snatStructList.iterator(), spec, completion);
    }

    @Override
    public void releaseSnatService(List<SnatStruct> snatStructList, VmInstanceSpec spec, NoErrorCompletion completion) {
        completion.done();
    }

    private void releasePrivateNicSnat(String vrUuid, VmNicInventory privateNic, Completion completion) {
        if (privateNic.isIpv6OnlyNic()) {
            completion.success();
            return;
        }

        try {
            nwServiceMgr.getTypeOfNetworkServiceProviderForService(privateNic.getL3NetworkUuid(), NetworkServiceType.SNAT);
        } catch (OperationFailureException e) {
            completion.success();
            return;
        }

        VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid).find();
        if (vrVO == null) {
            completion.success();
            return;
        }

        final VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);

        ApplianceVmSubTypeFactory subTypeFactory = apvmFactory.getApplianceVmSubTypeFactory(vrVO.getApplianceVmType());
        ApplianceVm app = subTypeFactory.getSubApplianceVm(vrVO);
        List<String> snatL3Uuids = app.getSnatL3NetworkOnRouter(vrUuid);
        if (snatL3Uuids.isEmpty()) {
            completion.success();
            return;
        }

        final List<VirtualRouterCommands.SNATInfo> snatInfo = new ArrayList<VirtualRouterCommands.SNATInfo>();
        List<VmNicInventory> pubNics = new ArrayList<>();
        pubNics.add(vr.getPublicNic());
        pubNics.addAll(vr.getAdditionalPublicNics());

        for (VmNicInventory pubNic : pubNics) {
            if (pubNic.isIpv6OnlyNic()) {
                continue;
            }

            if (!snatL3Uuids.contains(pubNic.getL3NetworkUuid())) {
                continue;
            }

            pubNic = vrMgr.getSnatPubicInventory(vr, pubNic.getL3NetworkUuid());
            VirtualRouterCommands.SNATInfo info = new VirtualRouterCommands.SNATInfo();
            info.setPrivateNicIp(privateNic.getIp());
            info.setPrivateNicMac(privateNic.getMac());
            info.setPublicIp(pubNic.getIp());
            info.setPublicNicMac(pubNic.getMac());
            info.setSnatNetmask(privateNic.getNetmask());
            info.setPrivateGatewayIp(privateNic.getGateway());
            snatInfo.add(info);
        }

        if (snatInfo.isEmpty()) {
            completion.success();
            return;
        }

        VirtualRouterCommands.RemoveSNATCmd cmd = new VirtualRouterCommands.RemoveSNATCmd();
        cmd.setNatInfo(snatInfo);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCheckStatus(true);
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_REMOVE_SNAT_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to release snat on virtual router[name:%s, uuid:%s] for private l3[uuid:%s], %s",
                            vr.getName(), vr.getUuid(), privateNic.getL3NetworkUuid(), reply.getError()));
                    //TODO GC
                } else {
                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    RemoveSNATRsp ret = re.toResponse(RemoveSNATRsp.class);
                    if (!ret.isSuccess()) {
                        String err = String.format(
                                "virtual router[uuid:%s, ip:%s] failed to release snat for L3Network[uuid:%s], because %s",
                                vr.getUuid(), vr.getManagementNic().getIp(), privateNic.getL3NetworkUuid(), ret.getError());
                        logger.warn(err);
                        //TODO GC
                    } else {
                        String msg = String.format(
                                "virtual router[uuid:%s, ip:%s] successfully released snat for L3Network[uuid:%s]",
                                vr.getUuid(), vr.getManagementNic().getIp(), privateNic.getL3NetworkUuid());
                        logger.warn(msg);
                    }
                }

                completion.success();
            }
        });
    }

    private void releasePublicNicSnat(String vrUuid, VmNicInventory publicNic, Completion completion) {
        if (publicNic.isIpv6OnlyNic()) {
            completion.success();
            return;
        }

        VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid).find();
        if (vrVO == null) {
            completion.success();
            return;
        }
        final VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);

        /* priviate l3 which has snat service enabled */
        List<String> nwServed = vr.getAllL3Networks();
        nwServed = vrMgr.selectL3NetworksNeedingSpecificNetworkService(nwServed, NetworkServiceType.SNAT);
        if (nwServed.isEmpty()) {
            completion.success();
            return;
        }

        ApplianceVmSubTypeFactory subTypeFactory = apvmFactory.getApplianceVmSubTypeFactory(vrVO.getApplianceVmType());
        ApplianceVm app = subTypeFactory.getSubApplianceVm(vrVO);
        List<String> snatL3Uuids = app.getSnatL3NetworkOnRouter(vrUuid);
        if (!snatL3Uuids.contains(publicNic.getL3NetworkUuid())) {
            completion.success();
            return;
        }

        final List<VirtualRouterCommands.SNATInfo> snatInfo = new ArrayList<VirtualRouterCommands.SNATInfo>();
        for (VmNicInventory priNic : vr.getGuestNics()) {
            if (priNic.isIpv6OnlyNic()) {
                continue;
            }

            publicNic = vrMgr.getSnatPubicInventory(vr, publicNic.getL3NetworkUuid());
            VirtualRouterCommands.SNATInfo info = new VirtualRouterCommands.SNATInfo();
            info.setPrivateNicIp(priNic.getIp());
            info.setPrivateNicMac(priNic.getMac());
            info.setPublicIp(publicNic.getIp());
            info.setPublicNicMac(publicNic.getMac());
            info.setSnatNetmask(priNic.getNetmask());
            info.setPrivateGatewayIp(priNic.getGateway());
            snatInfo.add(info);
        }

        if (snatInfo.isEmpty()) {
            completion.success();
            return;
        }

        VirtualRouterCommands.RemoveSNATCmd cmd = new VirtualRouterCommands.RemoveSNATCmd();
        cmd.setNatInfo(snatInfo);

        VmNicInventory finalNic = publicNic;

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCheckStatus(true);
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_REMOVE_SNAT_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to release snat on virtual router[name:%s, uuid:%s] for public l3[uuid:%s], %s",
                            vr.getName(), vr.getUuid(), finalNic.getL3NetworkUuid(), reply.getError()));
                    //TODO GC
                } else {
                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    RemoveSNATRsp ret = re.toResponse(RemoveSNATRsp.class);
                    if (!ret.isSuccess()) {
                        String err = String.format(
                                "virtual router[uuid:%s, ip:%s] failed to release snat for public l3[uuid:%s], because %s",
                                vr.getUuid(), vr.getManagementNic().getIp(), finalNic.getL3NetworkUuid(), ret.getError());
                        logger.warn(err);
                        //TODO GC
                    } else {
                        String msg = String.format(
                                "virtual router[uuid:%s, ip:%s] successfully released snat for public l3[uuid:%s]",
                                vr.getUuid(), vr.getManagementNic().getIp(), finalNic.getL3NetworkUuid());
                        app.detachNetworkService(vr.getUuid(), NetworkServiceType.SNAT.toString(), finalNic.getL3NetworkUuid());
                        logger.warn(msg);
                    }
                }

                completion.success();
            }
        });
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
        if (VirtualRouterNicMetaData.isGuestNic(nic)) {
            releasePrivateNicSnat(nic.getVmInstanceUuid(), nic, completion);
        } else if (VirtualRouterNicMetaData.isPublicNic(nic) || VirtualRouterNicMetaData.isAddinitionalPublicNic(nic)) {
            releasePublicNicSnat(nic.getVmInstanceUuid(), nic, completion);
        } else {
            completion.success();
        }
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find();
        DebugUtils.Assert(vrVO != null,
                String.format("can not find virtual router[uuid: %s] for nic[uuid: %s, ip: %s, l3NetworkUuid: %s]",
                        nic.getVmInstanceUuid(), nic.getUuid(), nic.getIp(), nic.getL3NetworkUuid()));

        ApplianceVmSubTypeFactory subTypeFactory = apvmFactory.getApplianceVmSubTypeFactory(vrVO.getApplianceVmType());
        ApplianceVm app = subTypeFactory.getSubApplianceVm(vrVO);

        List<String> snatL3Uuids = app.getSnatL3NetworkOnRouter(vrVO.getUuid());
        if (snatL3Uuids.isEmpty()) {
            completion.success();
            return;
        }
        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);

        List<String> nwServed = vr.getAllL3Networks();
        nwServed = vrMgr.selectL3NetworksNeedingSpecificNetworkService(nwServed, NetworkServiceType.SNAT);
        if (nwServed.isEmpty()) {
            completion.success();
            return;
        }

        new VirtualRouterRoleManager().makeSnatRole(vr.getUuid());

        final List<VirtualRouterCommands.SNATInfo> snatInfo = new ArrayList<VirtualRouterCommands.SNATInfo>();
        List<VmNicInventory> pubNics = new ArrayList<>();
        pubNics.add(vr.getPublicNic());
        pubNics.addAll(vr.getAdditionalPublicNics());

        for (VmNicInventory pubnic : pubNics) {
            if (pubnic.isIpv6OnlyNic()) {
                continue;
            }

            pubnic = vrMgr.getSnatPubicInventory(vr, pubnic.getL3NetworkUuid());
            for (VmNicInventory priNic : vr.getGuestNics()) {
                if (!nwServed.contains(priNic.getL3NetworkUuid()) || priNic.isIpv6OnlyNic()) {
                    continue;
                }

                VirtualRouterCommands.SNATInfo info = new VirtualRouterCommands.SNATInfo();
                info.setPrivateNicIp(priNic.getIp());
                info.setPrivateNicMac(priNic.getMac());
                info.setPublicIp(pubnic.getIp());
                info.setPublicNicMac(pubnic.getMac());
                info.setSnatNetmask(priNic.getNetmask());
                info.setPrivateGatewayIp(priNic.getGateway());
                if (snatL3Uuids.contains(pubnic.getL3NetworkUuid())) {
                    info.setState(Boolean.TRUE);
                } else {
                    info.setState(Boolean.FALSE);
                }
                snatInfo.add(info);
            }
        }
        if (snatInfo.isEmpty()) {
            completion.success();
            return;
        }

        VirtualRouterCommands.SyncSNATCmd cmd = new VirtualRouterCommands.SyncSNATCmd();
        cmd.setSnats(snatInfo);
        cmd.setEnable(true);
        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_SYNC_SNAT_PATH);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vr.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.SyncSNATRsp ret = re.toResponse(VirtualRouterCommands.SyncSNATRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("virtual router[name: %s, uuid: %s] failed to sync snat%s, %s",
                            vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(snatInfo), ret.getError());
                    completion.fail(err);
                    return;
                }

                Vip vip = getVipWithSnatService(vr, nic);
                if (vip == null) {
                    completion.success();
                    return;
                }

                vip.acquire(new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        });
    }

    private Vip getVipWithSnatService(VirtualRouterVmInventory vr, VmNicInventory nic){
        String vipUuid = null;
        for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
            vipUuid = ext.getPublicIpUuid(vr.getUuid(), vr.getDefaultRouteL3NetworkUuid());
        }

        if (vipUuid == null) {
            VmNicInventory publicNic = vrMgr.getSnatPubicInventory(vr);
            vipUuid = Q.New(VipVO.class).eq(VipVO_.ip, publicNic.getIp()).select(VipVO_.uuid).findValue();
        }
        if (vipUuid == null){
            return null;
        }

        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
        struct.setUseFor(NetworkServiceType.SNAT.toString());
        struct.setServiceUuid(vr.getUuid());
        Vip vip = new Vip(vipUuid);
        String l3NetworkUuuid = nic.getL3NetworkUuid();
        try {
            NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(l3NetworkUuuid, NetworkServiceType.SNAT);
            struct.setPeerL3NetworkUuid(l3NetworkUuuid);
            struct.setServiceProvider(providerType.toString());
        } catch (OperationFailureException e){
            logger.debug(String.format("Get providerType exception %s", e.toString()));
        }
        vip.setStruct(struct);
        return vip;
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }
}
