package org.zstack.storage.memorySnapshot;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.*;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.storage.snapshot.group.MemorySnapshotValidatorExtensionPoint;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.memorySnapshot.MemorySnapshotGroupConfigsUtils.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by LiangHanYu on 2022/10/10 17:25
 */
public class VmNicMemorySnapshotGroupExtension implements MemorySnapshotResourceExtensionPoint, MemorySnapshotValidatorExtensionPoint
        , ResourceMetadataBuilder {
    private static final CLogger logger = Utils.getLogger(VmNicMemorySnapshotGroupExtension.class);
    @Autowired
    private VmInstanceResourceMetadataManager vidm;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VmInstanceManager vmMgr;

    @Override
    public String getArchiveBundleCanonicalName() {
        return ArchiveVmNicBundle.class.getCanonicalName();
    }

    @Override
    public Class getArchiveBundleClass() {
        return ArchiveVmNicBundle.class;
    }

    @Override
    public void archiveDeviceAddressByResources(String vmInstanceUuid, Completion completion) {
        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, vmInstanceUuid)
                .eq(VmNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE)
                .list();
        if (vmNicVOS.isEmpty()) {
            completion.success();
            return;
        }
        String defaultL3NetworkUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.defaultL3NetworkUuid)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                .findValue();
        if (defaultL3NetworkUuid == null) {
            completion.fail(Platform.argerr("defaultL3NetworkUuid not exist"));
            return;
        }

        new While<>(vmNicVOS).each((vmNicVO, whileCompletion) -> {
            ArchiveVmNicBundle archiveVmNicBundle = new ArchiveVmNicBundle(VmNicInventory.valueOf(vmNicVO));
            archiveVmNicBundle.setResourceConfigBundles(getVmNicResourceConfigsForArchive(vmNicVO.getUuid()));
            archiveVmNicBundle.setSystemTagBundles(getSystemTagsForArchive(vmNicVO.getUuid()));

            if (vmNicVO.getL3NetworkUuid().equals(defaultL3NetworkUuid)) {
                archiveVmNicBundle.setVmDefaultL3Network(true);
            }

            GetVmNicQosMsg msg = new GetVmNicQosMsg();
            msg.setVmInstanceUuid(vmInstanceUuid);
            msg.setUuid(vmNicVO.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        whileCompletion.addError(reply.getError());
                        whileCompletion.allDone();
                        return;
                    }
                    GetVmNicQosReply getVmNicQosReply = reply.castReply();
                    archiveVmNicBundle.setInboundBandwidth(getVmNicQosReply.getInboundBandwidth());
                    archiveVmNicBundle.setOutboundBandwidth(getVmNicQosReply.getOutboundBandwidth());
                    vidm.createOrUpdateVmResourceMetadata(vmNicVO.getUuid(), null,
                            vmNicVO.getVmInstanceUuid(), JSONObjectUtil.toJsonString(archiveVmNicBundle),
                            ArchiveVmNicBundle.class.getCanonicalName());
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    public static List<VmInstanceResourceMetadataVO> buildCurrentVmNicResourceMetadataVOs(String vmInstanceUuid) {
        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, vmInstanceUuid)
                .eq(VmNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE)
                .list();
        if (vmNicVOS.isEmpty()) {
            return Collections.emptyList();
        }
        String defaultL3NetworkUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.defaultL3NetworkUuid)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                .findValue();
        String vmType = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmInstanceUuid).select(VmInstanceVO_.type).findValue();
        List<VmInstanceResourceMetadataVO> vos = new ArrayList<>();
        vmNicVOS.forEach(vmNicVO -> {
            VmInstanceResourceMetadataVO vo = new VmInstanceResourceMetadataVO();
            vo.setVmInstanceUuid(vmInstanceUuid);
            vo.setResourceUuid(vmNicVO.getUuid());
            vo.setMetadataClass(ArchiveVmNicBundle.class.getCanonicalName());
            vo.setMetadata(JSONObjectUtil.toJsonString(buildVmNicArchiveBundles(vmInstanceUuid, vmType, vmNicVO, defaultL3NetworkUuid)));
            vos.add(vo);
        });
        return vos;
    }

    public static ArchiveVmNicBundle buildVmNicArchiveBundles(String vmInstanceUuid, String vmType, VmNicVO vmNicVO, String defaultL3NetworkUuid) {
        ArchiveVmNicBundle archiveVmNicBundle = new ArchiveVmNicBundle(VmNicInventory.valueOf(vmNicVO));
        archiveVmNicBundle.setResourceConfigBundles(getVmNicResourceConfigsForArchive(vmNicVO.getUuid()));
        archiveVmNicBundle.setSystemTagBundles(getSystemTagsForArchive(vmNicVO.getUuid()));
        if (vmNicVO.getL3NetworkUuid().equals(defaultL3NetworkUuid)) {
            archiveVmNicBundle.setVmDefaultL3Network(true);
        }
        VmInstanceManager vmMgr = Platform.getComponentLoader().getComponent(VmInstanceManager.class);
        VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(vmType);
        VmNicQosStruct struct = backend.getNicQos(vmInstanceUuid, vmNicVO.getUuid());
        archiveVmNicBundle.setInboundBandwidth(struct.inboundBandwidth);
        archiveVmNicBundle.setOutboundBandwidth(struct.outboundBandwidth);
        return archiveVmNicBundle;
    }

    @Override
    public void recoverDeviceByAddress(String vmInstanceUuid, String resourceUuid, List<?> bundles, Completion completion) {
        if (bundles.isEmpty()) {
            completion.success();
            return;
        }

        List<VmInstanceResourceMetadataArchiveVO> archiveVmNicList = vidm.getArchivedResourceMetadataInfoFromArchiveForResourceUuid(
                vmInstanceUuid,
                resourceUuid,
                ArchiveVmNicBundle.class.getCanonicalName());

        vmNicsAndConfigsRestoreFlow(vmInstanceUuid, archiveVmNicList, false, completion);
    }

    public void vmNicsAndConfigsRestoreFlow(String vmInstanceUuid, List<VmInstanceResourceMetadataArchiveVO> archiveVmNicList, boolean skipCheckDuplicateIp, Completion completion) {
        List<String> archiveVmNicUuidList = archiveVmNicList.stream().map(VmInstanceResourceMetadataArchiveVO::getResourceUuid).collect(Collectors.toList());

        List<String> currentVmNicUuidList = Q.New(VmNicVO.class).select(VmNicVO_.uuid).eq(VmNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE).eq(VmNicVO_.vmInstanceUuid, vmInstanceUuid).listValues();

        List<String> needToDetachVmNicUuids = currentVmNicUuidList.stream().filter(currentVmNicUuid -> !archiveVmNicUuidList.contains(currentVmNicUuid)).collect(Collectors.toList());

        List<VmInstanceResourceMetadataArchiveVO> needToAttachVmNic = archiveVmNicList.stream()
                .filter(vo -> !currentVmNicUuidList.contains(vo.getResourceUuid())).collect(Collectors.toList());

        List<VmInstanceResourceMetadataArchiveVO> intersection = archiveVmNicList.stream()
                .filter(originalVmNic -> currentVmNicUuidList.contains(originalVmNic.getResourceUuid())).collect(Collectors.toList());

        FlowChain fchain = FlowChainBuilder.newShareFlowChain();
        fchain.setName(String.format("revert-vm-%s-nic-info", vmInstanceUuid));
        fchain.then(new ShareFlow() {
            List<ArchiveVmNicBundle> needToSetNicQosArchiveVmNicBundleList = new ArrayList<>();

            private boolean checkDuplicateIp(String l3Uuid, String ip) {
                return Q.New(VmNicVO.class)
                        .eq(VmNicVO_.l3NetworkUuid, l3Uuid)
                        .eq(VmNicVO_.type, VmInstanceConstant.VIRTUAL_NIC_TYPE)
                        .eq(VmNicVO_.ip, ip)
                        .isExists();
            }

            private boolean checkDuplicateMac(String vmNicUuid, String hypervisorType, String mac) {
                return Q.New(VmNicVO.class)
                        .eq(VmNicVO_.hypervisorType, hypervisorType)
                        .eq(VmNicVO_.mac, mac.toLowerCase())
                        .notEq(VmNicVO_.uuid, vmNicUuid).count() >= 1;
            }

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "detach-vm-nics";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needToDetachVmNicUuids).step((vmNicUuid, whileCompletion) -> {
                            DetachNicFromVmMsg msg = new DetachNicFromVmMsg();
                            msg.setVmInstanceUuid(vmInstanceUuid);
                            msg.setVmNicUuid(vmNicUuid);
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
                            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.allDone();
                                        return;
                                    }
                                    whileCompletion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow()  {
                    String __name__ = "update-nic-info-for-memory-snapshot-group";

                    @Override
                    public boolean skip(Map data) {
                        return intersection.isEmpty();
                    }

                    private boolean isDefaultL3NetworkChange(String currentDefaultL3Network, String defaultL3Network) {
                        return !defaultL3Network.equals(currentDefaultL3Network);
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String currentDefaultL3NetworkUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.defaultL3NetworkUuid).eq(VmInstanceVO_.uuid, vmInstanceUuid).findValue();
                        new While<>(intersection).step((originalVmNic, whileCompletion) -> {
                            ArchiveVmNicBundle updateArchiveVmNicBundle = JSONObjectUtil.toObject(originalVmNic.getMetadata(), ArchiveVmNicBundle.class);

                            if (updateArchiveVmNicBundle.isVmDefaultL3Network() && isDefaultL3NetworkChange(currentDefaultL3NetworkUuid, updateArchiveVmNicBundle.getVmNicInventory().getL3NetworkUuid())) {
                                logger.info(String.format("update defaultL3NetworkUuid [%s->%s] before update nic info", currentDefaultL3NetworkUuid, updateArchiveVmNicBundle.getVmNicInventory().getL3NetworkUuid()));
                                SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, originalVmNic.getVmInstanceUuid())
                                        .set(VmInstanceVO_.defaultL3NetworkUuid, updateArchiveVmNicBundle.getVmNicInventory().getL3NetworkUuid()).update();
                            }

                            needToSetNicQosArchiveVmNicBundleList.add(updateArchiveVmNicBundle);

                            VmNicInventory updateNicInventory = updateArchiveVmNicBundle.getVmNicInventory();
                            VmNicVO currentNic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, updateNicInventory.getUuid()).find();

                            VmNicState state = VmNicState.valueOf(updateNicInventory.getState());
                            if (checkDuplicateMac(updateNicInventory.getUuid(), updateNicInventory.getHypervisorType(), updateNicInventory.getMac())) {
                                logger.warn(String.format("disabling vmNic[uuid:%s] due to mac conflict", updateNicInventory.getUuid()));
                                state = VmNicState.disable;
                            }

                            logger.info(String.format("start update vmNic[%s]: driverType[%s->%s], deviceId[%s->%s], mac[%s->%s], state[%s->%s] for memory snapshot group"
                                    , updateNicInventory.getUuid()
                                    , currentNic.getDriverType(), updateNicInventory.getDriverType()
                                    , currentNic.getDeviceId(), updateNicInventory.getDeviceId()
                                    , currentNic.getMac(), updateNicInventory.getMac()
                                    , currentNic.getState().toString(), state)
                            );
                            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, updateNicInventory.getUuid())
                                    .set(VmNicVO_.driverType, updateNicInventory.getDriverType())
                                    .set(VmNicVO_.deviceId, updateNicInventory.getDeviceId())
                                    .set(VmNicVO_.mac, updateNicInventory.getMac())
                                    .set(VmNicVO_.state, state)
                                    .update();

                            if (!currentNic.getL3NetworkUuid().equals(updateNicInventory.getL3NetworkUuid())) {
                                ChangeVmNicNetworkMsg cmsg = new ChangeVmNicNetworkMsg();
                                cmsg.setDestL3NetworkUuid(updateNicInventory.getL3NetworkUuid());
                                cmsg.setVmNicUuid(updateNicInventory.getUuid());
                                cmsg.setVmInstanceUuid(updateNicInventory.getVmInstanceUuid());

                                if (!CollectionUtils.isEmpty(updateNicInventory.getUsedIps())) {
                                    HashMap<String, List<String>> map = new HashMap<>();
                                    List<String> ips = updateNicInventory.getUsedIps().stream().map(UsedIpInventory::getIp).collect(Collectors.toList());
                                    map.put(updateNicInventory.getL3NetworkUuid(), ips);
                                    cmsg.setRequiredIpMap(map);

                                    VmNicParam vmNicParam = new VmNicParam();
                                    vmNicParam.setL3NetworkUuid(updateNicInventory.getL3NetworkUuid());
                                    updateNicInventory.getUsedIps().forEach(usedIp -> {
                                        if (checkDuplicateIp(updateNicInventory.getL3NetworkUuid(), usedIp.getIp())) {
                                            return;
                                        }
                                        if (IPv6NetworkUtils.isIpv6Address(usedIp.getIp())) {
                                            vmNicParam.setIp6(usedIp.getIp());
                                        } else {
                                            vmNicParam.setIp(usedIp.getIp());
                                        }
                                        vmNicParam.setNetmask(usedIp.getNetmask());
                                        vmNicParam.setGateway(usedIp.getGateway());
                                    });
                                    cmsg.setVmNicParams(Collections.singletonList(vmNicParam));
                                }

                                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, cmsg.getVmInstanceUuid());
                                bus.send(cmsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            whileCompletion.addError(reply.getError());
                                            whileCompletion.allDone();
                                            return;
                                        }

                                        whileCompletion.done();
                                    }
                                });
                            } else if (!checkDuplicateIp(updateNicInventory.getL3NetworkUuid(), updateNicInventory.getIp())) {
                                SetVmStaticIpMsg smsg = new SetVmStaticIpMsg();
                                if (IPv6NetworkUtils.isIpv6Address(updateNicInventory.getIp())) {
                                    smsg.setIp6(updateNicInventory.getIp());
                                    smsg.setIpv6Prefix(String.valueOf(NetworkUtils.getPrefixLengthFromNetmask(updateNicInventory.getNetmask())));
                                    smsg.setIpv6Gateway(updateNicInventory.getGateway());
                                } else {
                                    smsg.setIp(updateNicInventory.getIp());
                                    smsg.setNetmask(updateNicInventory.getNetmask());
                                    smsg.setGateway(updateNicInventory.getGateway());
                                }
                                smsg.setL3NetworkUuid(updateNicInventory.getL3NetworkUuid());
                                smsg.setVmInstanceUuid(updateNicInventory.getVmInstanceUuid());
                                bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, smsg.getVmInstanceUuid());
                                bus.send(smsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            whileCompletion.addError(reply.getError());
                                            whileCompletion.allDone();
                                            return;
                                        }

                                        whileCompletion.done();
                                    }
                                });
                            } else {
                                whileCompletion.done();
                            }
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "attach-nics-saved-by-memory-snapshot-group";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needToAttachVmNic).step((originalArchiveVmNic, whileCompletion) -> {
                            ArchiveVmNicBundle updateArchiveVmNicBundle = JSONObjectUtil.toObject(originalArchiveVmNic.getMetadata(), ArchiveVmNicBundle.class);

                            VmNicInventory originalNicInventory = updateArchiveVmNicBundle.getVmNicInventory();
                            VmAttachNicMsg msg = new VmAttachNicMsg();
                            msg.setVmInstanceUuid(vmInstanceUuid);
                            msg.setL3NetworkUuid(originalNicInventory.getL3NetworkUuid());
                            if (skipCheckDuplicateIp) {
                                HashMap<String, List<String>> map = new HashMap<>();
                                List<String> ips = new ArrayList<>();
                                for (UsedIpInventory ip : originalNicInventory.getUsedIps()) {
                                    ips.add(ip.getIp());
                                }
                                map.put(originalNicInventory.getL3NetworkUuid(), ips);
                                msg.setStaticIpMap(map);
                            } else {
                                if (!checkDuplicateIp(originalNicInventory.getL3NetworkUuid(), originalNicInventory.getIp())) {
                                    HashMap<String, List<String>> map = new HashMap<>();
                                    List<String> ips = new ArrayList<>();
                                    for (UsedIpInventory ip : originalNicInventory.getUsedIps()) {
                                        ips.add(ip.getIp());
                                    }
                                    map.put(originalNicInventory.getL3NetworkUuid(), ips);
                                    msg.setStaticIpMap(map);
                                }
                            }
                            msg.setAllowDuplicatedMac(true);
                            msg.setSystemTags(Arrays.asList(VmSystemTags.CUSTOM_MAC.instantiateTag(map(
                                    e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, originalNicInventory.getL3NetworkUuid()),
                                    e(VmSystemTags.MAC_TOKEN, originalNicInventory.getMac())))));
                            msg.setDriverType(originalNicInventory.getDriverType());
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
                            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.allDone();
                                        return;
                                    }
                                    VmAttachNicReply vmAttachNicReply = reply.castReply();
                                    VmNicInventory inv = vmAttachNicReply.getInventroy();

                                    vidm.createOrUpdateVmResourceMetadata(inv.getUuid(),
                                            DeviceAddress.fromString(originalArchiveVmNic.getDeviceAddress()), msg.getVmInstanceUuid(),
                                            originalArchiveVmNic.getMetadata(), originalArchiveVmNic.getMetadataClass());
                                    vidm.deleteVmResourceMetadata(originalNicInventory.getUuid(), originalNicInventory.getVmInstanceUuid());

                                    // restore resource configs for vmNic which is detached
                                    ArchiveVmNicBundle archiveVmNicBundle = JSONObjectUtil.toObject(originalArchiveVmNic.getMetadata(), ArchiveVmNicBundle.class);
                                    MemorySnapshotGroupConfigsUtils.restoreConfigs(inv.getUuid(), archiveVmNicBundle);

                                    if (updateArchiveVmNicBundle.isVmDefaultL3Network()) {
                                        String currentDefaultL3NetworkUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.defaultL3NetworkUuid).eq(VmInstanceVO_.uuid, vmInstanceUuid).findValue();
                                        logger.info(String.format("update defaultL3NetworkUuid [%s->%s] before update nic info", currentDefaultL3NetworkUuid, inv.getL3NetworkUuid()));
                                        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, originalArchiveVmNic.getVmInstanceUuid())
                                                .set(VmInstanceVO_.defaultL3NetworkUuid, inv.getL3NetworkUuid()).update();
                                    }

                                    VmNicState state = VmNicState.valueOf(originalNicInventory.getState());
                                    if (checkDuplicateMac(inv.getUuid(), inv.getHypervisorType(), inv.getMac())) {
                                        logger.warn(String.format("disabling vmNic[uuid:%s] due to mac conflict", inv.getUuid()));
                                        state = VmNicState.disable;
                                    }

                                    logger.info(String.format("update driverType[%s->%s] and deviceId[%d] and state[%s->%s] for new vmNic %s"
                                            , inv.getDriverType(), originalNicInventory.getDriverType(),
                                            originalNicInventory.getDeviceId(), inv.getUuid(),
                                            inv.getState(), state));
                                    SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, inv.getUuid())
                                            .set(VmNicVO_.driverType, originalNicInventory.getDriverType())
                                            .set(VmNicVO_.deviceId, originalNicInventory.getDeviceId())
                                            .set(VmNicVO_.state, state)
                                            .update();

                                    needToSetNicQosArchiveVmNicBundleList.add(new ArchiveVmNicBundle(inv, updateArchiveVmNicBundle.getOutboundBandwidth(), updateArchiveVmNicBundle.getInboundBandwidth()));

                                    whileCompletion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "set-nics-qos-by-memory-snapshot-group";

                    @Override
                    public boolean skip(Map data) {
                        return needToSetNicQosArchiveVmNicBundleList.isEmpty();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(needToSetNicQosArchiveVmNicBundleList).step((needToSetNicQosArchiveVmNicBundle, whileCompletion) -> {
                            SetVmNicQosMsg smsg = new SetVmNicQosMsg();
                            smsg.setInboundBandwidth(needToSetNicQosArchiveVmNicBundle.getInboundBandwidth());
                            smsg.setOutboundBandwidth(needToSetNicQosArchiveVmNicBundle.getOutboundBandwidth());
                            smsg.setUuid(needToSetNicQosArchiveVmNicBundle.getVmNicInventory().getUuid());
                            smsg.setVmInstanceUuid(needToSetNicQosArchiveVmNicBundle.getVmNicInventory().getVmInstanceUuid());
                            bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, smsg.getVmInstanceUuid());
                            bus.send(smsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        whileCompletion.addError(reply.getError());
                                        whileCompletion.allDone();
                                        return;
                                    }

                                    whileCompletion.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errorCodeList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "revert-configs-from-archive";

                    @Override
                    public boolean skip(Map data) {
                        return intersection.isEmpty();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<VmNicVO> intersectionVmNicVOs = Q.New(VmNicVO.class).in(VmNicVO_.uuid, intersection.stream()
                                .map(VmInstanceResourceMetadataArchiveVO::getResourceUuid).collect(Collectors.toList())).list();
                        intersection.forEach(vo -> {
                            ArchiveVmNicBundle archiveVmNicBundle = JSONObjectUtil.toObject(vo.getMetadata(), ArchiveVmNicBundle.class);
                            MemorySnapshotGroupConfigsUtils.restoreConfigs(archiveVmNicBundle.getVmNicInventory().getUuid(), archiveVmNicBundle);

                            VmNicInventory vmNicInventory = JSONObjectUtil.toObject(vo.getMetadata(), ArchiveVmNicBundle.class).getVmNicInventory();
                            intersectionVmNicVOs.forEach(intersectionVO -> {
                                if (intersectionVO.getUuid().equals(vmNicInventory.getUuid()) && !Objects.equals(intersectionVO.getDriverType(), vmNicInventory.getDriverType())) {
                                    logger.info(String.format("update driverType[%s->%s] for vmNic[uuid:%s]",
                                            intersectionVO.getDriverType(), vmNicInventory.getDriverType(), vmNicInventory.getUuid()));
                                    SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicInventory.getUuid())
                                            .set(VmNicVO_.driverType, vmNicInventory.getDriverType())
                                            .update();
                                }
                            });

                            logger.debug(String.format("update device address for resourceUuid[uuid:%s], vmInstanceUuid[uuid:%s], new deviceAddress[%s]",
                                    vo.getResourceUuid(), vmInstanceUuid, vo.getDeviceAddress()));
                            vidm.createOrUpdateVmResourceMetadata(vo.getResourceUuid(),
                                    DeviceAddress.fromString(vo.getDeviceAddress()), vmInstanceUuid,
                                    vo.getMetadata(), vo.getMetadataClass());
                        });
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void rollBackResourceAndConfigs(String vmInstanceUuid, Map<String, VmInstanceResourceMetadataVO> originDeviceAddressByResourceUuid, Completion completion) {
        List<VmInstanceResourceMetadataArchiveVO> archiveVmNicList = originDeviceAddressByResourceUuid.values()
                .stream().filter(vo -> Objects.equals(vo.getMetadataClass(), ArchiveVmNicBundle.class.getCanonicalName()))
                .map(vo -> {
                    VmInstanceResourceMetadataArchiveVO archiveVO = new VmInstanceResourceMetadataArchiveVO();
                    archiveVO.setMetadata(vo.getMetadata());
                    archiveVO.setMetadataClass(vo.getMetadataClass());
                    archiveVO.setResourceUuid(vo.getResourceUuid());
                    archiveVO.setDeviceAddress(vo.getDeviceAddress());
                    return archiveVO;
                })
                .collect(Collectors.toList());
        vmNicsAndConfigsRestoreFlow(vmInstanceUuid, archiveVmNicList, true, completion);
    }

    private boolean isMemorySnapshotAddressGroupUuid(String addressGroupUuid) {
        List<String> resourceUuids = Q.New(VmInstanceResourceMetadataGroupVO.class)
                .select(VmInstanceResourceMetadataGroupVO_.resourceUuid)
                .eq(VmInstanceResourceMetadataGroupVO_.uuid, addressGroupUuid)
                .listValues();
        if (resourceUuids.isEmpty()) {
            return false;
        }
        return Q.New(VolumeSnapshotGroupVO.class).in(VolumeSnapshotGroupVO_.uuid, resourceUuids).isExists();
    }

    @Override
    public ErrorCode checkL3IfReferencedByMemorySnapshot(String l3Uuid) {
        List<VmInstanceResourceMetadataArchiveVO> archiveNicInfoList = Q.New(VmInstanceResourceMetadataArchiveVO.class)
                .eq(VmInstanceResourceMetadataArchiveVO_.metadataClass, ArchiveVmNicBundle.class.getCanonicalName()).list();

        // Get the archiveNicInfo referenced by memory snapshot
        archiveNicInfoList = archiveNicInfoList.stream().filter(archiveNicInfo -> isMemorySnapshotAddressGroupUuid(archiveNicInfo.getAddressGroupUuid())).collect(Collectors.toList());
        if (archiveNicInfoList.isEmpty()) {
            return null;
        }

        List<String> QuotedArchiveGroupList;

        QuotedArchiveGroupList = archiveNicInfoList.stream()
                .filter(vmInstanceResourceMetadataArchiveVO -> !StringUtils.isEmpty(vmInstanceResourceMetadataArchiveVO.getMetadata()))
                .filter(vmInstanceResourceMetadataArchiveVO -> JSONObjectUtil.toObject(vmInstanceResourceMetadataArchiveVO.getMetadata(), ArchiveVmNicBundle.class)
                        .getVmNicInventory().getL3NetworkUuid().equals(l3Uuid))
                .map(VmInstanceResourceMetadataArchiveVO::getAddressGroupUuid)
                .collect(Collectors.toList());

        if (QuotedArchiveGroupList.isEmpty()) {
            return null;
        }

        List<String> memorySnapshotGroupUuidList = Q.New(VmInstanceResourceMetadataGroupVO.class)
                .select(VmInstanceResourceMetadataGroupVO_.resourceUuid)
                .in(VmInstanceResourceMetadataGroupVO_.uuid, QuotedArchiveGroupList).listValues();
        if (!memorySnapshotGroupUuidList.isEmpty()) {
            return operr("nic with l3 network[uuid: %s] is referenced by VolumeSnapshotGroup[uuid: %s], delete this VolumeSnapshotGroup before deleting this l3 network.",
                    l3Uuid, String.join("','", memorySnapshotGroupUuidList));
        }

        return null;
    }

    @Override
    public List<VmInstanceResourceMetadataVO> buildResourceMetadataVOs(String vmInstanceUuid) {
        return buildCurrentVmNicResourceMetadataVOs(vmInstanceUuid);
    }
}
