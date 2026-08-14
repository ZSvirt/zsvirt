package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

public class VirtualRouterVipBackend extends AbstractVirtualRouterBackend implements VirtualRouterHaGetCallbackExtensionPoint,
        VipBackend, VirtualRouterAfterAttachNicExtensionPoint, VirtualRouterBeforeDetachNicExtensionPoint, PreVipReleaseExtensionPoint,
        ReleaseNetworkServiceOnDetachingNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VirtualRouterVipBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private VipConfigProxy proxy;
    @Autowired
    VirtualRouterManager vrMgr;

    public static String RELEASE_VIP_TASK = "releaseVip";
    public static String APPLY_VIP_TASK = "applyVip";

    private String getOwnerMac(VirtualRouterVmInventory vr, VipInventory vip) {
        for (VmNicInventory nic : vr.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(vip.getL3NetworkUuid())) {
                return nic.getMac();
            }
        }

        throw new CloudRuntimeException(String.format("virtual router vm[uuid:%s] has no nic on l3Network[uuid:%s] for vip[uuid:%s, ip:%s]",
                vr.getUuid(), vip.getL3NetworkUuid(), vip.getUuid(), vip.getIp()));
    }

    public void createVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, List<VipInventory> vips, Boolean syncVip, final Completion completion) {
        final List<VipTO> tos = new ArrayList<VipTO>(vips.size());
        List<VipInventory> systemVip = vips.stream().filter(VipInventory::isSystem).collect(Collectors.toList());
        List<VipInventory> notSystemVip = vips.stream().filter(v -> !v.isSystem()).collect(Collectors.toList());

        for (VipInventory vip : systemVip) {
            /* TODO: not support ipv6 vip */
            if (!NetworkUtils.isIpv4Address(vip.getIp())) {
                continue;
            }
            String mac = getOwnerMac(vr, vip);
            VipTO to = VipTO.valueOf(vip, mac);
            tos.add(to);
        }
        for (VipInventory vip : notSystemVip) {
            /* TODO: not support ipv6 vip */
            if (!NetworkUtils.isIpv4Address(vip.getIp())) {
                continue;
            }
            String mac = getOwnerMac(vr, vip);
            VipTO to = VipTO.valueOf(vip, mac);
            tos.add(to);
        }

        List<NicIpTO> nicIps = new ArrayList<>();
        if (syncVip) {
            for (VmNicInventory nic : vr.getVmNics()) {
                /* TODO: not support ipv6 vip */
                if (!NetworkUtils.isIpv4Address(nic.getIp())) {
                    continue;
                }
                nicIps.add(NicIpTO.valueOf(nic));
            }
        }

        CreateVipCmd cmd = new CreateVipCmd();
        cmd.setSyncVip(syncVip);
        cmd.setNicIps(nicIps);
        cmd.setVips(tos);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_CREATE_VIP);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                CreateVipRsp ret = re.toResponse(CreateVipRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to create vip%s on virtual router[uuid:%s], because %s", tos, vr.getUuid(), ret.getError());
                    completion.fail(err);
                } else {
                    completion.success();
                }
            }
        });
    }

    public void releaseVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, VipInventory vip, final Completion completion) {
        List<VipInventory> invs = new ArrayList<VipInventory>();
        invs.add(vip);
        releaseVipOnVirtualRouterVm(vr, invs, completion);
    }

    public void releaseVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, List<VipInventory> vips, final Completion completion) {
        final List<VipTO> tos = new ArrayList<VipTO>(vips.size());
        for (VipInventory vip : vips) {
            try {
                String mac = getOwnerMac(vr, vip);
                VipTO to = VipTO.valueOf(vip, mac);
                tos.add(to);
            } catch (Exception e) {
                logger.warn(String.format("virtual router [uuid:%s] nic of l3 network [uuid:%s] has been removed",
                        vr.getUuid(), vip.getL3NetworkUuid()));
            }
        }

        if (tos.isEmpty()) {
            completion.success();
            return;
        }

        RemoveVipCmd cmd = new RemoveVipCmd();
        cmd.setVips(tos);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_REMOVE_VIP);
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
                RemoveVipRsp ret = re.toResponse(RemoveVipRsp.class);
                if (ret.isSuccess()) {
                    completion.success();
                } else {
                    ErrorCode err = operr("failed to remove vip%s, because %s", tos, ret.getError());
                    completion.fail(err);
                }
            }
        });
    }

    public void acquireVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, final VipInventory vip, final Completion completion) {
        createVipOnVirtualRouterVm(vr, list(vip), false, new Completion(completion) {
            @Override
            public void success() {
                proxy.attachNetworkService(vr.getUuid(), VipVO.class.getSimpleName(), asList(vip.getUuid()));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
            completion.success();
            return;
        }

        List<VipTO> vips = findVipsOnVirtualRouter(nic);

        if (vips == null || vips.isEmpty()) {
            completion.success();
            return;
        }

        CreateVipCmd cmd = new CreateVipCmd();
        cmd.setSyncVip(false);
        cmd.setVips(vips);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(nic.getVmInstanceUuid());
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_CREATE_VIP);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, nic.getVmInstanceUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                CreateVipRsp ret = re.toResponse(CreateVipRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to sync vips[ips: %s] on virtual router[uuid:%s]" +
                            " for attaching nic[uuid: %s, ip: %s], because %s",
                            vips.stream().map(VipTO::getIp).collect(Collectors.toList()),
                            nic.getVmInstanceUuid(), nic.getUuid(), nic.getIp(), ret.getError());
                    completion.fail(err);
                } else {
                    List<String> vipUuids = vips.stream().map(VipTO::getVipUuid).distinct().collect(Collectors.toList());
                    proxy.attachNetworkService(nic.getVmInstanceUuid(), VipVO.class.getSimpleName(), vipUuids);
                    completion.success();
                }
            }
        });
    }

    private List<VipTO> findVipsOnVirtualRouter(VmNicInventory nic) {
        List<String> vipUuids = SQL.New("select vip.uuid from VipVO vip, VipPeerL3NetworkRefVO ref " +
                "where ref.vipUuid = vip.uuid and vip.system = false " +
                "and ref.l3NetworkUuid = :l3Uuid")
                .param("l3Uuid", nic.getL3NetworkUuid())
                .list();

        vipUuids = vrMgr.getVirtualRouterVips(nic.getVmInstanceUuid(), vipUuids);
        if (vipUuids == null || vipUuids.isEmpty()) {
            return null;
        }

        List<VipVO> vips = Q.New(VipVO.class).in(VipVO_.uuid, vipUuids).list();
        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf((VirtualRouterVmVO)
                Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find());

        List<VipVO> systemVip = vips.stream().filter(VipVO::isSystem).collect(Collectors.toList());
        List<VipVO> notSystemVip = vips.stream().filter(v -> !v.isSystem()).collect(Collectors.toList());
        List<VipVO> vipss = new ArrayList<>();
        vipss.addAll(systemVip);
        vipss.addAll(notSystemVip);

        List<VipTO> vipTOS = new ArrayList<>();
        for (VipVO vip : vipss) {
            if (vipTOS.stream().anyMatch(v -> v.getIp().equals(vip.getIp()))) {
                logger.warn(String.format(
                        "found duplicate vip ip[uuid; %s, uuids: %s] for vr[uuid: %s]",
                        vip.getIp(),
                        vips.stream().
                                filter(v -> v.getIp().equals(vip.getIp()))
                                .map(ResourceVO::getUuid)
                                .collect(Collectors.toSet()),
                        nic.getVmInstanceUuid()));
                continue;
            }

            VipTO to = new VipTO();
            to.setIp(vip.getIp());
            to.setGateway(vip.getGateway());
            to.setNetmask(vip.getNetmask());
            Optional<VmNicInventory> pubNic = vr.getVmNics().stream()
                    .filter(n -> n.getL3NetworkUuid().equals(vip.getL3NetworkUuid()))
                    .findFirst();
            if (!pubNic.isPresent()) {
                continue;
            }
            to.setOwnerEthernetMac(pubNic.get().getMac());
            to.setVipUuid(vip.getUuid());
            to.setSystem(vip.isSystem());
            vipTOS.add(to);
        }

        return vipTOS;
    }

    @Override
    public String getServiceProviderTypeForVip() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    }

    private void detachVipForPrivateL3(VmNicInventory nic, Completion completion) {
        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf((VirtualRouterVmVO)
                Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find());

        List<VipVO> vips = SQL.New("select vip from VipVO vip, VipPeerL3NetworkRefVO ref " +
                "where ref.vipUuid = vip.uuid and ref.l3NetworkUuid in (:routerNetworks) " +
                "and vip.l3NetworkUuid = :l3Uuid and vip.system=false")
                .param("l3Uuid", nic.getL3NetworkUuid())
                .param("routerNetworks", vr.getAllL3Networks())
                .list();

        if (vips.isEmpty()) {
            completion.success();
            return;
        }

        releaseVipOnVirtualRouterVm(vr, VipInventory.valueOf(vips), completion);
    }

    private void detachVipForPublicL3(VmNicInventory nic, Completion completion) {
        List<String> vipUuids = proxy.getServiceUuidsByRouterUuid(nic.getVmInstanceUuid(), VipVO.class.getSimpleName());
        if (vipUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<VipVO> vips = Q.New(VipVO.class).in(VipVO_.uuid, vipUuids).eq(VipVO_.l3NetworkUuid, nic.getL3NetworkUuid()).list();
        if (vips.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(vips).each((vip, compl) -> {
            VipDeletionMsg dmsg = new VipDeletionMsg();
            /* if system vip is also a ip of vmnic, don't return ip address in vip flow */
            boolean exists = Q.New(VmNicVO.class).eq(VmNicVO_.usedIpUuid, vip.getUsedIpUuid()).eq(VmNicVO_.l3NetworkUuid, vip.getL3NetworkUuid()).isExists();
            dmsg.setVipUuid(vip.getUuid());
            dmsg.setReturnIp(!exists);
            bus.makeTargetServiceIdByResourceUuid(dmsg, VipConstant.SERVICE_ID, vip.getUuid());
            bus.send(dmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
            completion.success();
            return;
        }

        if (VirtualRouterNicMetaData.isGuestNic(nic)) {
            detachVipForPrivateL3(nic, completion);
        } else if (VirtualRouterNicMetaData.isAddinitionalPublicNic(nic)) {
            detachVipForPublicL3(nic, completion);
        } else {
            completion.success();
        }
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void preReleaseServicesOnVip(VipInventory vip, Completion completion) {
        /* this ugly */
        SQL.New(VirtualRouterVipVO.class).eq(VirtualRouterVipVO_.uuid, vip.getUuid()).delete();
        completion.success();
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct applyVip = new VirtualRouterHaCallbackStruct();
        applyVip.type = APPLY_VIP_TASK;
        applyVip.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need applyVip on backend", vrUuid));
                    completion.success();
                    return;
                }

                VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vrVO);
                VipInventory vipInventory = JSONObjectUtil.toObject(task.getJsonData(), VipInventory.class);
                createVipOnVirtualRouterVm(vrInv, Collections.singletonList(vipInventory), false, completion);
            }
        };
        structs.add(applyVip);

        VirtualRouterHaCallbackStruct releaseVip = new VirtualRouterHaCallbackStruct();
        releaseVip.type = RELEASE_VIP_TASK;
        releaseVip.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need releaseVip on backend", vrUuid));
                    completion.success();
                    return;
                }

                VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vrVO);
                VipInventory vipInventory = JSONObjectUtil.toObject(task.getJsonData(), VipInventory.class);
                releaseVipOnVirtualRouterVm(vrInv, Collections.singletonList(vipInventory), completion);
            }
        };
        structs.add(releaseVip);

        return structs;
    }

    @Override
    public void releaseResourceOnDetachingNic(VmInstanceSpec spec, VmNicInventory nic, NoErrorCompletion completion) {
        if (!VirtualRouterNicMetaData.isGuestNic(nic)) {
            completion.done();
            return;
        }

        logger.debug(String.format("check detaching nic[uuid:%s] in peer l3 of vip", nic.getUuid()));
        List<VipPeerL3NetworkRefVO> refVOS = Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.l3NetworkUuid,
                nic.getL3NetworkUuid()).list();
        if (refVOS == null || refVOS.isEmpty()) {
            completion.done();
            return;
        }

        Set<String> refUuids = refVOS.stream().map(VipPeerL3NetworkRefVO::getVipUuid).collect(Collectors.toSet());
        logger.debug(String.format("release peer l3[uuid:%s] from vips[uuid:%s] for detaching nic[uuid:%s]",
                nic.getL3NetworkUuid(), refUuids, nic.getUuid()));
        List<VipVO> vipVOS = Q.New(VipVO.class).in(VipVO_.uuid, refUuids).list();
        for (VipVO vipVO : vipVOS) {
            VipBase v = new VipBase(vipVO);
            v.deletePeerL3NetworkUuid(nic.getL3NetworkUuid());
        }

        completion.done();
    }
}
