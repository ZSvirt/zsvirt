package org.zstack.network.service.virtualrouter.dhcp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmHaStatus;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.l3.L3NetworkSystemTags;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.AddDhcpEntryRsp;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.zstack.core.Platform.operr;

public class VirtualRouterDhcpBackend extends AbstractVirtualRouterBackend implements NetworkServiceDhcpBackend, VirtualRouterHaGetCallbackExtensionPoint {
    private final CLogger logger = Utils.getLogger(VirtualRouterDhcpBackend.class);

    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private VirtualRouterHaBackend haBackend;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected NetworkServiceManager nsMgr;

    private final String APPLY_DHCP_TASK = "applyDHCP";
    private final String RELEASE_DHCP_TASK = "releaseDHCP";

    @Override
    public NetworkServiceProviderType getProviderType() {
        return VirtualRouterConstant.PROVIDER_TYPE;
    }

    private void doApplyDhcpEntryToVirtualRouter(VirtualRouterVmInventory vr, VirtualRouterCommands.DhcpInfo info, Completion completion) {
        VirtualRouterCommands.AddDhcpEntryCmd cmd = new VirtualRouterCommands.AddDhcpEntryCmd();
        cmd.setDhcpEntries(Arrays.asList(info));
        VirtualRouterAsyncHttpCallMsg cmsg = new VirtualRouterAsyncHttpCallMsg();
        cmsg.setCommand(cmd);
        cmsg.setPath(VirtualRouterConstant.VR_ADD_DHCP_PATH);
        cmsg.setVmInstanceUuid(vr.getUuid());
        cmsg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                AddDhcpEntryRsp rsp =  re.toResponse(AddDhcpEntryRsp.class);
                if (rsp.isSuccess()) {
                    new VirtualRouterRoleManager().makeDhcpRole(vr.getUuid());
                    logger.debug(String.format("successfully add dhcp entry[%s] to virtual router vm[uuid:%s, ip:%s]",
                            JSONObjectUtil.toJsonString(info), vr.getUuid(), vr.getManagementNic().getIp()));
                    completion.success();
                } else {
                    ErrorCode err = operr("unable to add dhcp entries to virtual router vm[uuid:%s ip:%s], because %s, dhcp entry[%s]",
                            vr.getUuid(), vr.getManagementNic().getIp(), rsp.getError(), JSONObjectUtil.toJsonString(info));
                    completion.fail(err);
                }
            }
        });
    }

    private void applyDhcpEntryToHAVirtualRouter(VirtualRouterVmInventory vr, DhcpStruct struct, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(APPLY_DHCP_TASK);
        task.setOriginRouterUuid(vr.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(struct));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    private VirtualRouterCommands.DhcpInfo getDhcpInfo(VirtualRouterVmInventory vr, DhcpStruct struct) {
        VirtualRouterCommands.DhcpInfo info = new VirtualRouterCommands.DhcpInfo();
        info.setGateway(struct.getGateway());
        info.setIp(struct.getIp());
        info.setDefaultL3Network(struct.isDefaultL3Network());
        info.setMac(struct.getMac());
        info.setGateway(struct.getGateway());
        info.setNetmask(struct.getNetmask());
        info.setDnsDomain(struct.getDnsDomain());
        info.setHostname(struct.getHostname());
        info.setMtu(struct.getMtu());

        if (info.isDefaultL3Network()) {
            if (info.getHostname() == null) {
                info.setHostname(info.getIp().replaceAll("\\.", "-"));
            }

            if (info.getDnsDomain() != null) {
                info.setHostname(String.format("%s.%s", info.getHostname(), info.getDnsDomain()));
            }
        }

        VmNicInventory vrNic = CollectionUtils.findOneOrNull(vr.getVmNics(),
                arg -> arg.getL3NetworkUuid().equals(struct.getL3Network().getUuid()));
        info.setVrNicMac(vrNic.getMac());
        if (struct.isDefaultL3Network()) {
            /*if there is no DNS service, the DHCP uses the external DNS service. ZSTAC-13262 by miaozhanyong*/
            if (struct.getL3Network().getNetworkServiceTypes().contains(NetworkServiceType.DNS.toString())) {
                info.setDns(CollectionDSL.list(vrNic.getIp()));
            } else {
                info.setDns(struct.getL3Network().getDns());
            }
        }

        return info;
    }

    private void applyDhcpEntryToVirtualRouter(VirtualRouterVmInventory vr, DhcpStruct struct, Completion completion) {
        VirtualRouterCommands.DhcpInfo info = getDhcpInfo(vr, struct);
        doApplyDhcpEntryToVirtualRouter(vr, info, new Completion(completion) {
            @Override
            public void success() {
                applyDhcpEntryToHAVirtualRouter(vr, struct, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void applyDhcpEntry(final Iterator<DhcpStruct> it, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final DhcpStruct struct = it.next();

        VirtualRouterStruct s = new VirtualRouterStruct();
        s.setL3Network(struct.getL3Network());

        acquireVirtualRouterVmForDhcp(s, new ReturnValueCompletion<VirtualRouterVmInventory>(completion) {
            @Override
            public void success(final VirtualRouterVmInventory vr) {
                applyDhcpEntryToVirtualRouter(vr, struct, new Completion(completion) {
                    @Override
                    public void success() {
                        applyDhcpEntry(it, completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void applyDhcpService(final List<DhcpStruct> dhcpStructList, final VmInstanceSpec spec, final Completion completion) {
        if (dhcpStructList.isEmpty()) {
            completion.success();
            return;
        }

        applyDhcpEntry(dhcpStructList.iterator(), completion);
    }

    private void releaseDhcpFromHaVirtualRouter(VirtualRouterVmInventory vr, DhcpStruct struct, final NoErrorCompletion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(RELEASE_DHCP_TASK);
        task.setOriginRouterUuid(vr.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(struct));
        haBackend.submitVirtualRouterHaTask(task, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.done();
            }
        });
    }

    private void doReleaseDhcpFromVirtualRouter(VirtualRouterVmInventory vr, VirtualRouterCommands.DhcpInfo info, final NoErrorCompletion completion) {
        VirtualRouterCommands.RemoveDhcpEntryCmd cmd = new VirtualRouterCommands.RemoveDhcpEntryCmd();
        cmd.setDhcpEntries(Arrays.asList(info));

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCheckStatus(true);
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setPath(VirtualRouterConstant.VR_REMOVE_DHCP_PATH);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("unable to remove dhcp entry[%s] from virtual router vm[uuid:%s, ip:%s], %s",
                            JSONObjectUtil.toJsonString(info), vr.getUuid(), vr.getManagementNic().getIp(), reply.getError()));
                    //TODO: GC
                } else {
                    VirtualRouterAsyncHttpCallReply ret = reply.castReply();
                    if (ret.isSuccess()) {
                        logger.debug(String.format("successfully removed dhcp entry[%s] from virtual router vm[uuid:%s, ip:%s]",
                                JSONObjectUtil.toJsonString(info), vr.getUuid(), vr
                                .getManagementNic().getIp()));
                    } else {
                        logger.warn(String.format("unable to remove dhcp entry[%s] from virtual router vm[uuid:%s, ip:%s], %s",
                                JSONObjectUtil.toJsonString(info), vr.getUuid(), vr
                                .getManagementNic().getIp(), ret.getError()));
                        //TODO: GC
                    }
                }

                completion.done();
            }
        });
    }

    private void releaseDhcpFromVirtualRouter(VirtualRouterVmInventory vr, DhcpStruct struct, final NoErrorCompletion completion) {
        VirtualRouterCommands.DhcpInfo info = getDhcpInfo(vr, struct);
        doReleaseDhcpFromVirtualRouter(vr, info, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                releaseDhcpFromHaVirtualRouter(vr, struct, completion);
            }
        });
    }

    private void releaseDhcp(final Iterator<DhcpStruct> it, final VmInstanceSpec spec, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        final DhcpStruct struct = it.next();
        final VirtualRouterVmInventory vr = getVirtualRouterForVyosDhcp(struct.getL3Network());
        if (vr == null) {
            logger.debug(String.format("virtual router for l3Network[uuid:%s] is not found, skip releasing DHCP", struct.getL3Network().getUuid()));
            releaseDhcp(it, spec, completion);
            return;
        }

        releaseDhcpFromVirtualRouter(vr, struct, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                releaseDhcp(it, spec, completion);
            }
        });
    }

    @Override
    public void releaseDhcpService(List<DhcpStruct> dhcpStructList, VmInstanceSpec spec, NoErrorCompletion completion) {
        if (dhcpStructList.isEmpty()) {
            completion.done();
            return;
        }

        releaseDhcp(dhcpStructList.iterator(), spec, completion);
    }

    @Override
    public void vmDefaultL3NetworkChanged(VmInstanceInventory vm, String previousL3, String nowL3, Completion completion) {
        completion.success();
    }
    
    protected boolean isVRouterDhcpEnabled(String l3Uuid) {
        try {
            NetworkServiceProviderType providerType = nsMgr.getTypeOfNetworkServiceProviderForService(l3Uuid, NetworkServiceType.DHCP);
            if (VyosConstants.PROVIDER_TYPE == providerType) {
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    public VirtualRouterVmInventory getVirtualRouterForVyosDhcp(L3NetworkInventory l3Nw) {
        /* for vyos dhcp, it will not create virtual router, if virtual router not existed return error */
        String sql = "select vr from VirtualRouterVmVO vr, VmNicVO nic where vr.uuid = nic.vmInstanceUuid and nic.l3NetworkUuid = :l3Uuid and nic.metaData in (:meta)";
        TypedQuery<VirtualRouterVmVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterVmVO.class);
        q.setParameter("l3Uuid", l3Nw.getUuid());
        if (l3Nw.getCategory().equals(L3NetworkCategory.Public.toString())) {
            q.setParameter("meta", VirtualRouterNicMetaData.ALL_PUBLIC_NIC_MASK_STRING_LIST);
        } else {
            q.setParameter("meta", VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST);
        }
        List<VirtualRouterVmVO> vrs = q.getResultList();

        VirtualRouterVmVO masterVr = null;
        if (l3Nw.getCategory().equals(L3NetworkCategory.Public.toString())) {
            /* only public l3 network has systemTag: PUBLIC_NETWORK_DHCP_SERVER_UUID
            * tag value is virtual router uuid for non-ha virtual router, ha group uuid for ha router  */
            String uuid = L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID.getTokenByResourceUuid(l3Nw.getUuid(), L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN);
            if (uuid == null) {
                return null;
            }

            for (VirtualRouterVmVO vr : vrs) {
                if (vr.getUuid().equals(uuid)) {
                    return VirtualRouterVmInventory.valueOf(vr);
                }

                if (uuid.equals(haBackend.getVirtualRouterHaUuid(vr.getUuid()))) {
                    if (masterVr == null || vr.getHaStatus() == ApplianceVmHaStatus.Master) {
                        masterVr = vr;
                    }
                }
            }
        } else {
            for (VirtualRouterVmVO vr : vrs) {
                if (masterVr == null || vr.getHaStatus() == ApplianceVmHaStatus.Master) {
                    masterVr = vr;
                }
            }
        }

        if (masterVr != null) {
            return VirtualRouterVmInventory.valueOf(masterVr);
        } else {
            return null;
        }
    }

    private void acquireVirtualRouterVmForDhcp(VirtualRouterStruct dhcpStruct, ReturnValueCompletion<VirtualRouterVmInventory> completion) {
        L3NetworkInventory l3Nw = dhcpStruct.getL3Network();
        boolean vyosDhcpOnPublicNetwork = l3Nw.getCategory().equals(L3NetworkCategory.Public.toString()) && isVRouterDhcpEnabled(l3Nw.getUuid());
        if (vyosDhcpOnPublicNetwork) {
            VirtualRouterVmInventory vrInv = getVirtualRouterForVyosDhcp(l3Nw);
            if (vrInv == null) {
                completion.fail(Platform.operr("no virtual router is configured for vyos dhcp"));
            } else {
                completion.success(vrInv);
            }
            return;
        }

        acquireVirtualRouterVm(dhcpStruct, completion);
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct applyDhcp = new VirtualRouterHaCallbackStruct();
        applyDhcp.type = APPLY_DHCP_TASK;
        applyDhcp.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need apply Eip on backend", vrUuid));
                    completion.success();
                    return;
                }

                VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);
                DhcpStruct struct = JSONObjectUtil.toObject(task.getJsonData(), DhcpStruct.class);
                VirtualRouterCommands.DhcpInfo info = getDhcpInfo(vr, struct);
                doApplyDhcpEntryToVirtualRouter(vr, info, completion);
            }
        };
        structs.add(applyDhcp);

        VirtualRouterHaCallbackStruct releaseDhcp = new VirtualRouterHaCallbackStruct();
        releaseDhcp.type = RELEASE_DHCP_TASK;
        releaseDhcp.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need release Eip on backend", vrUuid));
                    completion.success();
                    return;
                }

                VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);
                DhcpStruct struct = JSONObjectUtil.toObject(task.getJsonData(), DhcpStruct.class);
                VirtualRouterCommands.DhcpInfo info = getDhcpInfo(vr, struct);
                doReleaseDhcpFromVirtualRouter(vr, info, new NoErrorCompletion(completion) {
                    @Override
                    public void done() {
                        completion.success();
                    }
                });
            }
        };
        structs.add(releaseDhcp);

        return structs;
    }

    @Override
    public void enableNetworkService(L3NetworkVO l3VO, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void disableNetworkService(L3NetworkVO l3VO, Completion completion) {
        completion.success();
    }
}
