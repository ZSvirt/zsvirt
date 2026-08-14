package org.zstack.storage.backup;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.*;
import org.zstack.header.host.*;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileSyncReason;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.HasThreadContextAspect;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.volume.backup.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.kvm.efi.KvmSecureBootManager;
import org.zstack.kvm.vmfiles.message.SyncVmHostFilesFromHostMsg;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.*;
import org.zstack.kvm.*;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.backup.imagestore.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.backup.VolumeBackupKvmCommands.*;
import static org.zstack.utils.CollectionUtils.*;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeBackupKvmBackend implements VolumeBackupHypervisorBackend {
    private static final CLogger logger = Utils.getLogger(VolumeBackupKvmBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private VmInstanceResourceMetadataManager vidm;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private KvmSecureBootManager secureBootManager;

    @Override
    public void handleMessage(VolumeBackupMessage msg) {
        if (msg instanceof APICreateVolumeBackupMsg) {
            handle((APICreateVolumeBackupMsg) msg);
        } else if (msg instanceof APICreateVmBackupMsg) {
            handle((APICreateVmBackupMsg) msg);
        } else if (msg instanceof CreateVolumeBackupMsg) {
            handle((CreateVolumeBackupMsg) msg);
        } else if (msg instanceof CreateVolumeBackupInnerMsg) {
            handle((CreateVolumeBackupInnerMsg) msg);
        } else if (msg instanceof CreateVmBackupOverlayInnerMsg) {
            handle((CreateVmBackupOverlayInnerMsg) msg);
        } else if (msg instanceof CreateVmBackupMsg) {
            handle((CreateVmBackupMsg) msg);
        } else if (msg instanceof CancelVolumeBackupJobMsg) {
            handle((CancelVolumeBackupJobMsg) msg);
        } else if (msg instanceof CancelVmBackupJobMsg) {
            handle((CancelVmBackupJobMsg) msg);
        } else {
            bus.dealWithUnknownMessage((Message) msg);
        }
    }

    private void handle(APICreateVolumeBackupMsg msg) {
        CreateVolumeBackupMsg cmsg = new CreateVolumeBackupMsg();
        cmsg.setResourceUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
        cmsg.setVolumeUuid(msg.getVolumeUuid());
        cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        cmsg.setName(msg.getName());
        cmsg.setDescription(msg.getDescription());

        if (msg.getMode() != null) {
            cmsg.setMode(BackupMode.valueOf(msg.getMode()));
        }

        BackupQosStruct struct = new BackupQosStruct();
        struct.setNetworkReadBandwidth(msg.getNetworkReadBandwidth());
        struct.setNetworkWriteBandwidth(msg.getNetworkWriteBandwidth());
        struct.setVolumeReadBandwidth(msg.getVolumeReadBandwidth());
        struct.setVolumeWriteBandwidth(msg.getVolumeWriteBandwidth());
        cmsg.setBackupQosStruct(struct);
        cmsg.setSystemTags(msg.getSystemTags());
        cmsg.addSystemTag(VolumeBackupSystemTag.MANUAL_CREATE_RESOURCE.instantiateTag(Collections.singletonMap(VolumeBackupSystemTag.API_ID_TOKEN, msg.getId())));
        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeBackupConstant.SERVICE_ID, msg.getVolumeUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APICreateVolumeBackupEvent evt = new APICreateVolumeBackupEvent(msg.getId());
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                } else {
                    CreateVolumeBackupReply r = reply.castReply();
                    evt.setInventory(r.getInventory());
                    evt.setActualExecuteTime(r.getActualExecuteTime());
                }

                bus.publish(evt);
            }
        });
    }

    private void handle(APICreateVmBackupMsg msg) {
        CreateVmBackupMsg cmsg = new CreateVmBackupMsg();
        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
        cmsg.setRootVolumeUuid(msg.getRootVolumeUuid());
        cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        cmsg.setName(msg.getName());
        cmsg.setDescription(msg.getDescription());
        cmsg.setResourceUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());

        if (msg.getMode() != null) {
            cmsg.setMode(BackupMode.valueOf(msg.getMode()));
        }

        BackupQosStruct struct = new BackupQosStruct();
        struct.setNetworkReadBandwidth(msg.getNetworkReadBandwidth());
        struct.setNetworkWriteBandwidth(msg.getNetworkWriteBandwidth());
        struct.setVolumeReadBandwidth(msg.getVolumeReadBandwidth());
        struct.setVolumeWriteBandwidth(msg.getVolumeWriteBandwidth());
        cmsg.setBackupQosStruct(struct);
        cmsg.setSystemTags(msg.getSystemTags());
        cmsg.addSystemTag(VolumeBackupSystemTag.MANUAL_CREATE_RESOURCE.instantiateTag(Collections.singletonMap(VolumeBackupSystemTag.API_ID_TOKEN, msg.getId())));
        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeBackupConstant.SERVICE_ID, msg.getVolumeUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APICreateVmBackupEvent evt = new APICreateVmBackupEvent(msg.getId());

                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                } else {
                    CreateVmBackupReply r = reply.castReply();
                    evt.setInventories(r.getInventories());
                    evt.setActualExecuteTime(r.getActualExecuteTime());
                }

                bus.publish(evt);
            }
        });
    }

    static class VolumeInfo {
        String vmUuid;
        String hostUuid;
        int deviceId;
    }

    @Transactional(readOnly = true)
    private VolumeInfo getVolumeHostAndVM(VolumeVO volumeVO) {
        if (volumeVO.isShareable()) {
            throw new ApiMessageInterceptionException(operr("Operation not supported on shared volume"));
        }

        VmInstanceVO v = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, volumeVO.getVmInstanceUuid()).find();
        if (v == null) {
            throw new ApiMessageInterceptionException(operr("No VM found for volume[uuid:%s]", volumeVO.getUuid()));
        }

        String hostUuid = v.getHostUuid() != null ? v.getHostUuid() : v.getLastHostUuid();

        VolumeInfo info = new VolumeInfo();
        info.vmUuid = v.getUuid();
        info.hostUuid = hostUuid;
        info.deviceId = volumeVO.getDeviceId();
        return info;
    }

    private Tuple getAndClearBackupHistory(String volumeUuid, String bsUuid) {
        return new SQLBatchWithReturn<Tuple>() {
            @Override
            protected Tuple scripts() {
                Tuple t = sql("select ref.installPath, h.bitmap from VolumeBackupStorageRefVO ref, VolumeBackupHistoryVO h"
                        + " where h.uuid = :volumeUuid"
                        + " and ref.volumeBackupUuid = h.lastBackupUuid"
                        + " and ref.backupStorageUuid = :bsUuid", Tuple.class)
                        .param("volumeUuid", volumeUuid)
                        .param("bsUuid", bsUuid)
                        .find();
                sql(VolumeBackupHistoryVO.class).eq(VolumeBackupHistoryVO_.uuid, volumeUuid).delete();
                return t;
            }
        }.execute();
    }

    private List<VolumeBackupKvmCommands.BackupInfo> getAndClearBackupsInfo(Set<String> volumeUuids, String bsUuid) {
        List<VolumeBackupKvmCommands.BackupInfo> res = new ArrayList<>();

        new SQLBatch() {
            @Override
            protected void scripts() {
                List<Tuple> ts = sql("select ref.installPath, h.bitmap, h.uuid from VolumeBackupStorageRefVO ref, VolumeBackupHistoryVO h"
                        + " where h.uuid in (:volumeUuids)"
                        + " and ref.volumeBackupUuid = h.lastBackupUuid"
                        + " and ref.backupStorageUuid = :bsUuid", Tuple.class)
                        .param("volumeUuids", volumeUuids)
                        .param("bsUuid", bsUuid)
                        .list();
                sql(VolumeBackupHistoryVO.class).in(VolumeBackupHistoryVO_.uuid, volumeUuids).delete();
                ts.forEach(t -> {
                    VolumeBackupKvmCommands.BackupInfo info = new VolumeBackupKvmCommands.BackupInfo();
                    info.lastBackup = t.get(0, String.class);
                    info.bitmap = t.get(1, String.class);
                    info.volumeUuid = t.get(2, String.class);
                    res.add(info);
                });
            }
        }.execute();

        return res;
    }

    private void doCreateVolumesBackups(VolumeBackupKvmCommands.TakeBackupsCmd cmd,
                                        CreateVmBackupOverlayInnerMsg msg,
                                        ReturnValueCompletion<VolumeBackupKvmCommands.TakeBackupsResponse> completion) {
        new KvmCommandSender(msg.getHostUuid()).send(cmd, TAKE_VOLUMES_BACKUP_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                VolumeBackupKvmCommands.TakeBackupsResponse rsp = wrapper.getResponse(VolumeBackupKvmCommands.TakeBackupsResponse.class);
                return rsp.isSuccess() ? null : operr("%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper w) {
                VolumeBackupKvmCommands.TakeBackupsResponse rsp = w.getResponse(VolumeBackupKvmCommands.TakeBackupsResponse.class);
                completion.success(rsp);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (!errorCode.isError(SysErrors.TIMEOUT)) {
                    completion.fail(errorCode);
                    return;
                }

                cancelBackupJobs(msg.getVmInstanceUuid(), msg.getHostUuid(), new Completion(completion) {
                    @Override
                    public void success() {
                        completion.fail(errorCode);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        });
    }

    private void doCreateVolumeBackup(final VolumeInfo info, VolumeBackupKvmCommands.TakeBackupCmd cmd,
                                      ReturnValueCompletion<VolumeBackupKvmCommands.TakeBackupResponse> completion) {
        new KvmCommandSender(info.hostUuid).send(cmd, TAKE_VOLUME_BACKUP_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                VolumeBackupKvmCommands.TakeBackupResponse rsp = wrapper.getResponse(VolumeBackupKvmCommands.TakeBackupResponse.class);
                return rsp.isSuccess() ? null : operr("%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper w) {
                VolumeBackupKvmCommands.TakeBackupResponse rsp = w.getResponse(VolumeBackupKvmCommands.TakeBackupResponse.class);
                completion.success(rsp);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (!errorCode.isError(SysErrors.TIMEOUT)) {
                    completion.fail(errorCode);
                    return;
                }

                cancelBackupJobs(info.vmUuid, info.hostUuid, new Completion(completion) {
                    @Override
                    public void success() {
                        completion.fail(errorCode);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        });
    }

    private void cancelBackupJobs(String vmUuid, String hostUuid, Completion completion) {
        VolumeBackupKvmCommands.CancelBackupJobsCmd cmd = new VolumeBackupKvmCommands.CancelBackupJobsCmd();
        cmd.setVmUuid(vmUuid);

        new KvmCommandSender(hostUuid).send(cmd, CANCEL_VOLUME_BACKUP_JOBS_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                VolumeBackupKvmCommands.CancelBackupJobsResponse rsp = wrapper.getResponse(VolumeBackupKvmCommands.CancelBackupJobsResponse.class);
                return rsp.isSuccess() ? null : operr("%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper w) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void cancelBackupJob(String vmUuid, String volumeUuid, String hostUuid, Completion completion) {
        VolumeBackupKvmCommands.CancelBackupJobCmd cmd = new VolumeBackupKvmCommands.CancelBackupJobCmd();
        cmd.setVmUuid(vmUuid);

        VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).find();
        KVMHostVO host = Q.New(KVMHostVO.class).eq(KVMHostVO_.uuid, hostUuid).find();
        cmd.setVolume(VolumeTO.valueOf(VolumeInventory.valueOf(volume), KVMHostInventory.valueOf(host)));

        new KvmCommandSender(hostUuid).send(cmd, CANCEL_VOLUME_BACKUP_JOB_PATH, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                VolumeBackupKvmCommands.CancelBackupJobsResponse rsp = wrapper.getResponse(VolumeBackupKvmCommands.CancelBackupJobsResponse.class);
                return rsp.isSuccess() ? null : operr("%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper w) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void handle(CreateVmBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("create-vm-backup-%s", msg.getRootVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                handleBackup(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handleVmBackup(final CreateVmBackupMsg msg, NoErrorCompletion completion) {
        CreateVmBackupReply r = new CreateVmBackupReply();
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.rootVolumeUuid, msg.getRootVolumeUuid())
                .find();
        if (vmInstanceVO == null) {
            r.setError(Platform.operr("No VM found with root volume uuid: %s", msg.getRootVolumeUuid()));
            bus.reply(msg, r);
            return;
        }

        VmInstanceInventory inv = VmInstanceInventory.valueOf(vmInstanceVO);
        List<String> volumeUuids = inv.getAllVolumes().stream()
                .filter(vol -> !vol.isShareable())
                .filter(vol -> vol.getType().equals(VolumeType.Data.toString()) || vol.getType().equals(VolumeType.Root.toString()))
                .map((VolumeInventory::getUuid))
                .collect(Collectors.toList());

        CreateVmBackupOverlayInnerMsg innerMsg = new CreateVmBackupOverlayInnerMsg();
        innerMsg.setName(msg.getName());
        innerMsg.setDescription(msg.getDescription());
        innerMsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        innerMsg.setVmInstanceUuid(inv.getUuid());
        innerMsg.setAccountUuid(msg.getAccountUuid());
        innerMsg.setVolumeUuids(new HashSet<>(volumeUuids));
        innerMsg.setVolumeUuid(volumeUuids.get(0));
        innerMsg.setLockedVolumeUuids(new HashSet<>(Arrays.asList(innerMsg.getVolumeUuid())));
        innerMsg.setHostUuid(inv.getHostUuid() == null ? inv.getLastHostUuid() : inv.getHostUuid());
        innerMsg.setBackupQosStruct(msg.getBackupQosStruct());
        innerMsg.setMode(msg.getMode());
        innerMsg.setSystemTags(msg.getSystemTags());
        innerMsg.setResourceUuid(msg.getResourceUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, VolumeBackupConstant.SERVICE_ID, innerMsg.getVolumeUuid());

        VolumeBackupOverlayMsg omsg = new VolumeBackupOverlayMsg();
        omsg.setVolumeUuid(innerMsg.getVolumeUuid());
        omsg.setMessage(innerMsg);
        bus.makeTargetServiceIdByResourceUuid(omsg, VolumeConstant.SERVICE_ID, omsg.getVolumeUuid());

        bus.send(omsg, new CloudBusCallBack(msg, completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    r.setError(reply.getError());
                } else {
                    CreateVmBackupOverlayInnerReply innerReply = reply.castReply();
                    r.setInventories(innerReply.getInventories());
                    r.setActualExecuteTime(innerReply.getActualExecuteTime());
                }

                bus.reply(msg, r);
                completion.done();
            }
        });
    }

    private void handle(CreateVmBackupOverlayInnerMsg msg) {
        DebugUtils.Assert(msg.getLockedVolumeUuids().size() <= msg.getVolumeUuids().size(),
                "size of locked volumes can not larger than jobs");

        if (msg.getLockedVolumeUuids().size() < msg.getVolumeUuids().size()) {
            CreateVmBackupOverlayInnerMsg innerMsg = new CreateVmBackupOverlayInnerMsg();
            innerMsg.setAccountUuid(msg.getAccountUuid());
            innerMsg.setVolumeUuids(msg.getVolumeUuids());
            innerMsg.setVmInstanceUuid(msg.getVmInstanceUuid());
            innerMsg.setHostUuid(msg.getHostUuid());
            innerMsg.setBackupStorageUuid(msg.getBackupStorageUuid());
            innerMsg.setName(msg.getName());
            innerMsg.setDescription(msg.getDescription());
            innerMsg.setBackupQosStruct(msg.getBackupQosStruct());
            innerMsg.setMode(msg.getMode());
            innerMsg.setResourceUuid(msg.getResourceUuid());

            Set<String> lockedVolumeUuids = new HashSet<>(msg.getLockedVolumeUuids());
            String newLockVolumeUuid = null;
            for (String volumeUuid : msg.getVolumeUuids()) {
                if (!lockedVolumeUuids.contains(volumeUuid)) {
                    newLockVolumeUuid = volumeUuid;
                    break;
                }
            }

            lockedVolumeUuids.add(newLockVolumeUuid);
            innerMsg.setLockedVolumeUuids(lockedVolumeUuids);
            innerMsg.setVolumeUuid(newLockVolumeUuid);
            innerMsg.setSystemTags(msg.getSystemTags());
            bus.makeTargetServiceIdByResourceUuid(innerMsg, VolumeBackupConstant.SERVICE_ID, innerMsg.getVolumeUuid());

            VolumeBackupOverlayMsg omsg = new VolumeBackupOverlayMsg();
            omsg.setVolumeUuid(newLockVolumeUuid);
            omsg.setMessage(innerMsg);
            bus.makeTargetServiceIdByResourceUuid(omsg, VolumeConstant.SERVICE_ID, omsg.getVolumeUuid());

            bus.send(omsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    bus.reply(msg, reply);
                }
            });

            return;
        }

        logger.info(String.format("creating volume backups for VM[uuid:%s] on host[uuid:%s]",
                msg.getVmInstanceUuid(), msg.getHostUuid()));

        long startTime = System.currentTimeMillis();
        CreateVmBackupOverlayInnerReply innerReply = new CreateVmBackupOverlayInnerReply();
        runCreateVolumesBackupsFlow(msg, new ReturnValueCompletion<List<VolumeBackupInventory>>(msg) {
            @Override
            public void success(List<VolumeBackupInventory> returnValue) {
                innerReply.setInventories(returnValue);
                innerReply.setActualExecuteTime(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
                bus.reply(msg, innerReply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                innerReply.setError(errorCode);
                bus.reply(msg, innerReply);
            }
        });
    }

    private BiMap<String, Integer> getDeviceIdMap(Set<String> volumeUuids) {
        List<Tuple> ts = Q.New(VolumeVO.class)
                .select(VolumeVO_.uuid, VolumeVO_.deviceId)
                .in(VolumeVO_.uuid, volumeUuids)
                .listTuple();

        return HashBiMap.create(ts.stream().collect(Collectors.toMap(
                t -> ((Tuple)t).get(0, String.class),
                t -> ((Tuple)t).get(1, Integer.class)
        )));
    }

    private static String findBitmap(VolumeBackupKvmCommands.TakeBackupsResponse rsp, int deviceId) {
        for (VolumeBackupKvmCommands.VolumeBackupInfo info : rsp.getBackupInfos()) {
            if (info.getDeviceId() == deviceId) {
                return info.getBitmap();
            }
        }

        return null;
    }

    private void runCreateVolumesBackupsFlow(CreateVmBackupOverlayInnerMsg msg, ReturnValueCompletion<List<VolumeBackupInventory>> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("take-volume-backups-for-vm-" + msg.getVmInstanceUuid());
        chain.enableProgressReport();
        chain.then(new ShareFlow() {
            String uploadDir;
            BackupMode backupMode = msg.getMode();
            final String groupUuid = msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid();
            String rootVolumeBackupUuid = Platform.getUuid();
            TakeBackupsResponse rsp;
            List<BackupInfo> res;
            final TakeBackupsCmd cmd = new TakeBackupsCmd();
            List<VolumeInventory> volumes;
            final Map<String, VolumeBackupVO> volumeBackups = new HashMap<>();
            final BiMap<String, Integer> deviceIdMap = getDeviceIdMap(msg.getVolumeUuids());
            final ImageStoreBackupStorageCommands.StorageInfo storageInfo = getStorageInfo(msg.getBackupStorageUuid());
            boolean nbdDefault = true;

            String nbdProcessDescription = null;
            final Map<Integer, AllocateImageInstallPathReply> ReplyByDeviceId = new HashMap<>();
            final Map<Integer, VolumeBackupStorageRefVO> refVOsByDeviceId = new HashMap<>();
            Timestamp backupStartTime;
            Timestamp backupEndTime;
            final String vmName = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                    .select(VmInstanceVO_.name).findValue();
            final int maxBackupNameLength = 255;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "check-vm-qemu-version";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetVirtualizerInfoMsg gmsg = new GetVirtualizerInfoMsg();
                        gmsg.setHostUuid(msg.getHostUuid());
                        gmsg.setVmInstanceUuids(Collections.singletonList(msg.getVmInstanceUuid()));
                        bus.makeLocalServiceId(gmsg, HostConstant.SERVICE_ID);
                        bus.send(gmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()){
                                    cmd.setPointInTime(false);
                                    nbdDefault = true;
                                    trigger.next();
                                    return;
                                }

                                GetVirtualizerInfoReply r = reply.castReply();
                                Optional<GetVirtualizerInfoReply.VmVirtualizerInfo> vmInfo = r.getVmInfoList().stream()
                                        .filter(item -> item.getVmInstanceUuid().equals(msg.getVmInstanceUuid()))
                                        .findAny();
                                if (!vmInfo.isPresent()) {
                                    cmd.setPointInTime(false);
                                    nbdDefault = true;
                                    trigger.next();
                                    return;
                                }

                                QemuKvmVersion version = new QemuKvmVersion(vmInfo.get().getCurrentQemuVersion());
                                if (version.supportMirrorBitmap()) {
                                    cmd.setPointInTime(false);
                                    nbdDefault = true;
                                } else {
                                    cmd.setPointInTime(true);
                                    nbdDefault = false;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "allocate-upload-workspace";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocateUploadWorkspaceMsg amsg = new AllocateUploadWorkspaceMsg();
                        amsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(amsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (reply.isSuccess()) {
                                            AllocateUploadWorkspaceReply r = reply.castReply();
                                            uploadDir = r.getUploadWorkspace();
                                            cmd.setBsPath(r.getBsInstallPath());
                                            trigger.next();
                                        } else {
                                            trigger.fail(reply.getError());
                                        }
                                    }
                                }
                        );
                    }
                });

                flow(new Flow() {
                    String __name__ = "prepare-vm-backup-records";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                for (String volumeUuid : msg.getVolumeUuids()) {
                                    VolumeBackupVO vbvo = new VolumeBackupVO();
                                    vbvo.setType(Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).select(VolumeVO_.type).findValue());
                                    vbvo.setUuid(vbvo.getType() == VolumeType.Root ? rootVolumeBackupUuid : Platform.getUuid());
                                    vbvo.setAccountUuid(msg.getAccountUuid());
                                    vbvo.setVolumeUuid(volumeUuid);
                                    vbvo.setName(volumeUuid);
                                    vbvo.setSize(0L);
                                    vbvo.setDescription(msg.getDescription());
                                    vbvo.setState(VolumeBackupState.Disabled);
                                    vbvo.setStatus(VolumeBackupStatus.Creating);
                                    vbvo.setGroupUuid(groupUuid);
                                    vbvo.setVmInstanceUuid(msg.getVmInstanceUuid());
                                    vbvo = dbf.persistAndRefresh(vbvo);
                                    volumeBackups.put(volumeUuid, vbvo);
                                    tagMgr.createTags(msg.getSystemTags(), msg.getUserTags(), vbvo.getUuid(), VolumeBackupVO.class.getSimpleName());
                                }
                            }
                        }.execute();

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        List<String> backupUuids = volumeBackups.values().stream()
                                .map(ResourceVO::getUuid)
                                .collect(Collectors.toList());
                        dbf.removeByPrimaryKeys(backupUuids, VolumeBackupVO.class);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "archive-volume-pci-address";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        vidm.archiveCurrentResourceMetadata(msg.getVmInstanceUuid(), groupUuid);
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "clean-backup-histories";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        res = getAndClearBackupsInfo(msg.getVolumeUuids(), msg.getBackupStorageUuid());
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "before-create-vm-backup-extension";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        pluginRgty.getExtensionList(CreateVolumeBackupExtensionPoint.class).forEach(it ->
                                it.beforeCreateVmBackup(volumeBackups.values(), msg.getBackupStorageUuid()));
                        trigger.next();
                    }
                });

                if (backupMode != BackupMode.full) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "compute-backup-mode";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            backupMode = BackupMode.auto;

                            res.forEach(info -> info.deviceId = deviceIdMap.get(info.volumeUuid));
                            ErrorCodeList err = new ErrorCodeList();
                            new While<>(res).each((info, whileCompletion) -> {
                                if (info.lastBackup == null) {
                                    whileCompletion.done();
                                    return;
                                }

                                GetImageChainInfoMsg gmsg = new GetImageChainInfoMsg();
                                gmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                                gmsg.setInstallPath(info.lastBackup);
                                bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                                bus.send(gmsg, new CloudBusCallBack(whileCompletion) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            logger.warn(reply.getError().getDetails());
                                            err.getCauses().add(reply.getError());
                                            whileCompletion.allDone();
                                            return;
                                        }

                                        GetImageChainInfoReply r = reply.castReply();
                                        if (r.getChain().size() >= VolumeBackupGlobalConfig.MAX_INCREMENTAL_BACKUP.value(Integer.class)) {
                                            backupMode = BackupMode.full;
                                            whileCompletion.allDone();
                                            return;
                                        }

                                        whileCompletion.done();
                                    }
                                });
                            }).run(new WhileDoneCompletion(trigger) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    if (!err.getCauses().isEmpty()) {
                                        trigger.fail(err.getCauses().get(0));
                                    } else {
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    });
                }

                if (targetIsNbd()) {
                    flow(syncVolumeSize(msg.getVolumeUuids()));
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "get-imagestore-backupstorage-cred";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetImageStoreBackupStorageDownloadCredentialMsg gmsg = new GetImageStoreBackupStorageDownloadCredentialMsg();
                        gmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        bus.send(gmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                GetImageStoreBackupStorageDownloadCredentialReply greply = reply.castReply();
                                KVMHostVO host = Q.New(KVMHostVO.class).eq(KVMHostVO_.uuid, msg.getHostUuid()).find();

                                cmd.setHostname(getBackupAddress(msg.getBackupStorageUuid()));
                                cmd.setUsername(greply.getUsername());
                                cmd.setPassword(greply.getPassword());
                                cmd.setSshPort(greply.getSshPort());

                                cmd.setUploadDir(uploadDir);
                                cmd.setVmUuid(msg.getVmInstanceUuid());
                                cmd.setBackupInfos(res);
                                cmd.setMaxIncremental(VolumeBackupGlobalConfig.MAX_INCREMENTAL_BACKUP.value(Integer.class));
                                cmd.setMode(backupMode.toString());
                                volumes = VolumeInventory.valueOf(dbf.listByPrimaryKeys(msg.getVolumeUuids(), VolumeVO.class));
                                volumes.sort(Comparator.comparingInt(VolumeInventory::getDeviceId));
                                cmd.setVolumes(VolumeTO.valueOf(volumes, KVMHostInventory.valueOf(host)));
                                cmd.setStorageInfo(storageInfo);

                                if (msg.getBackupQosStruct() != null) {
                                    cmd.setNetworkWriteBandwidth(msg.getBackupQosStruct().getNetworkWriteBandwidth());
                                    cmd.setVolumeWriteBandwidth(msg.getBackupQosStruct().getVolumeWriteBandwidth());
                                }
                                ResourceConfig rc = rcf.getResourceConfig(ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY.getIdentity());
                                cmd.setUploadConcurrency(rc.getResourceConfigValue(msg.getBackupStorageUuid(), Integer.class));
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "export-nbd-target-from-bs";

                    ExportNbdImagesReply r;

                    @Override
                    public boolean skip(Map data) {
                        return !targetIsNbd();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        // sorted by deviceIds
                        List<Long> sizes = volumes.stream()
                                .map(VolumeInventory::getSize)
                                .collect(Collectors.toList());

                        ExportNbdImagesMsg emsg = new ExportNbdImagesMsg();
                        emsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        emsg.setSizes(sizes);
                        emsg.setWorkspace(uploadDir);
                        bus.makeLocalServiceId(emsg, BackupStorageConstant.SERVICE_ID);
                        bus.send(emsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                r = reply.castReply();
                                cmd.setStorageInfo(ImageStoreBackupStorageCommands.StorageInfo.valueOfUrl(
                                        String.format("nbd://%s:%s", cmd.getHostname(), Strings.join(r.getPorts(), ','))
                                ));
                                cmd.setBackupPaths(r.getImagePaths());
                                nbdProcessDescription = r.getNbdDescription();
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback flowRollback, Map map) {
                        if (r == null) {
                            flowRollback.rollback();
                            return;
                        }

                        CancelExportNbdImagesMsg cmsg = new CancelExportNbdImagesMsg();
                        cmsg.setPorts(r.getPorts());
                        cmsg.setImagePaths(r.getImagePaths());
                        cmsg.setNbdDescription(r.getNbdDescription());
                        cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bus.makeLocalServiceId(cmsg, BackupStorageConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(flowRollback) {
                            @Override
                            public void run(MessageReply reply) {
                                flowRollback.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "generating-vm-backups";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        HasThreadContextAspect.setThreadContext(cmd); // context which set after new() has been invalid.
                        backupStartTime = new Timestamp(System.currentTimeMillis());
                        doCreateVolumesBackups(cmd, msg, new ReturnValueCompletion<TakeBackupsResponse>(trigger) {
                                    @Override
                                    public void success(TakeBackupsResponse returnValue) {
                                        if (returnValue.isSuccess()) {
                                            rsp = returnValue;
                                            backupEndTime = new Timestamp(System.currentTimeMillis());
                                            trigger.next();
                                        } else {
                                            trigger.fail(operr("%s", returnValue.getError()));
                                        }
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                    }
                                });
                    }
                });

                flow(buildBackupVmHostFileForVolumeBackupFlow(msg.getVmInstanceUuid(), msg.getHostUuid(), rootVolumeBackupUuid));

                flow(new Flow() {
                    String __name__ = "allocate-imagestore-install-path";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ErrorCodeList errList = new ErrorCodeList();
                        new While<>(rsp.getBackupInfos()).each((info, whileCompletion) -> {
                            AllocateImageInstallPathMsg amsg = new AllocateImageInstallPathMsg();
                            amsg.setParentInstallPath(info.getParentInstallPath());
                            amsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                            bus.makeTargetServiceIdByResourceUuid(amsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        AllocateImageInstallPathReply r = reply.castReply();
                                        ReplyByDeviceId.put(info.getDeviceId(), r);
                                        whileCompletion.done();
                                        return;
                                    }
                                    errList.getCauses().add(reply.getError());
                                    whileCompletion.allDone();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errList.getCauses().isEmpty()) {
                                    trigger.fail(errList.getCauses().get(0));
                                    return;
                                }

                                ReplyByDeviceId.forEach((deviceId, reply) -> {
                                    String volumeUuid = deviceIdMap.inverse().get(deviceId);
                                    if (volumeUuid == null) {
                                        return;
                                    }
                                    VolumeBackupVO vbvo = volumeBackups.get(volumeUuid);

                                    VolumeBackupStorageRefVO refVO = new VolumeBackupStorageRefVO();
                                    refVO.setBackupStorageUuid(msg.getBackupStorageUuid());
                                    refVO.setVolumeBackupUuid(vbvo.getUuid());
                                    refVO.setInstallPath(reply.getInstallPath());
                                    refVO.setStatus(VolumeBackupStatus.Creating);
                                    refVOsByDeviceId.put(deviceId, refVO);
                                });
                                dbf.persistCollection(refVOsByDeviceId.values());
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (!CollectionUtils.isEmpty(refVOsByDeviceId.values())) {
                            dbf.removeCollection(refVOsByDeviceId.values(), VolumeBackupStorageRefVO.class);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "importing-vm-backup";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ErrorCodeList errList = new ErrorCodeList();
                        new While<>(rsp.getBackupInfos())
                                .enableProgressReport("import-images")
                                .each((info, whileCompletion) -> {
                            ImportImageMsg imsg = new ImportImageMsg();

                            imsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                            imsg.setParent(info.getParentInstallPath());
                            imsg.setFilename(info.getBackupFile());
                            imsg.setProcessToRelease(nbdProcessDescription);
                            imsg.setName(ReplyByDeviceId.get(info.getDeviceId()).getName());
                            bus.makeTargetServiceIdByResourceUuid(imsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());

                            doImportImage(imsg, new ReturnValueCompletion<ImportImageReply>(imsg, trigger) {
                                @Override
                                public void success(ImportImageReply reply) {
                                    ImportImageReply ireply = reply.castReply();
                                    updateDb(info.getDeviceId(), ireply);
                                    whileCompletion.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errList.getCauses().add(errorCode);
                                    whileCompletion.allDone();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errList.getCauses().isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errList.getCauses().get(0));
                                }
                            }
                        });
                    }

                    private List<Long> getProgressWeight() {
                        List<Long> results = new ArrayList<>();
                        for (VolumeBackupInfo backupInfo : rsp.getBackupInfos()) {
                            // TODO: get weight by backup size
                            results.add(1L);
                        }
                        return results;
                    }

                    private void updateDb(int deviceId, ImportImageReply ireply) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                String volumeUuid = deviceIdMap.inverse().get(deviceId);
                                if (volumeUuid == null) {
                                    return;
                                }

                                VolumeBackupVO vbvo = volumeBackups.get(volumeUuid);
                                vbvo.setState(VolumeBackupState.Enabled);
                                vbvo.setStatus(VolumeBackupStatus.Ready);
                                vbvo.setSize(ireply.getSize());

                                if (vbvo.getType() == VolumeType.Root) {
                                    List<String> dataVolumeUuids = volumeBackups.values().stream()
                                            .filter(v -> v.getType() == VolumeType.Data)
                                            .map(VolumeBackupVO::getVolumeUuid)
                                            .collect(Collectors.toList());
                                    vbvo.setMetadata(
                                            VolumeMetaDataMaker.generateRootVolumeMetadata(
                                                    vbvo.getVolumeUuid(), dataVolumeUuids
                                            )
                                    );
                                    vbvo.setName(buildRootVolumeName());
                                } else {
                                    vbvo.setMetadata(
                                            VolumeMetaDataMaker.generateDataVolumeMetadata(vbvo.getVolumeUuid())
                                    );
                                    vbvo.setName(buildVolumeBackupName(volumeUuid));
                                }

                                vbvo.setMode(backupMode);
                                merge(vbvo);

                                VolumeBackupStorageRefVO refVO = refVOsByDeviceId.get(deviceId);
                                refVO.setInstallPath(ireply.getInstallPath());
                                refVO.setStatus(VolumeBackupStatus.Ready);
                                merge(refVO);

                                VolumeBackupHistoryVO hvo = new VolumeBackupHistoryVO();
                                hvo.setBitmap(findBitmap(rsp, deviceId));
                                hvo.setLastBackupUuid(vbvo.getUuid());
                                hvo.setUuid(vbvo.getVolumeUuid());

                                persist(hvo);
                            }
                        }.execute();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        List<VolumeBackupVO> backs = dbf.listByPrimaryKeys(volumeBackups.values().stream()
                                .map(ResourceVO::getUuid).collect(Collectors.toList()), VolumeBackupVO.class);
                        pluginRgty.getExtensionList(CreateVolumeBackupExtensionPoint.class).forEach(new Consumer<CreateVolumeBackupExtensionPoint>() {
                            @Override
                            @ExceptionSafe
                            public void accept(CreateVolumeBackupExtensionPoint ext) {
                                ext.afterCreateVmBackup(backs, msg.getBackupStorageUuid());
                            }
                        });

                        completion.success(VolumeBackupInventory.valueOf(backs));
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        pluginRgty.getExtensionList(CreateVolumeBackupExtensionPoint.class).forEach(it ->
                                it.failedToCreateVmBackup(volumeBackups.values(), msg.getBackupStorageUuid()));
                        completion.fail(errCode);
                    }
                });
            }

            private String buildVolumeBackupName(String volumeUuid) {
                String name = "-" + volumeUuid + "-";
                name = cmd.isPointInTime() ? name + backupStartTime : name + backupEndTime;

                return msg.getName().length() > maxBackupNameLength - name.length()
                        ? msg.getName().substring(0, maxBackupNameLength - name.length()) + name : msg.getName() + name;
            }

            private String buildRootVolumeName() {
                String time = '-' + String.valueOf(cmd.isPointInTime() ? backupStartTime : backupEndTime);
                return msg.getName().length() > maxBackupNameLength - time.length()
                        ? msg.getName().substring(0, maxBackupNameLength - time.length()) + time : msg.getName() + time;
            }

            private boolean targetIsNbd() {
                return (storageInfo == null && nbdDefault) || (storageInfo != null && storageInfo.getType().equals("nbd"));
            }
        }).start();
    }

    private void handle(CreateVolumeBackupInnerMsg msg) {
        CreateVolumeBackupInnerReply r = new CreateVolumeBackupInnerReply();
        long startTime = System.currentTimeMillis();
        runCreateVolumeBackupFlow(msg, new ReturnValueCompletion<VolumeBackupInventory>(msg) {
            @Override
            public void success(VolumeBackupInventory returnValue) {
                r.setInventory(returnValue);
                r.setActualExecuteTime(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    private void handle(CreateVolumeBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("take-backup-for-volume-%s", msg.getVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                handleBackup(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handleBackup(VolumeBackupMessage msg, NoErrorCompletion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("take-backup-on-bs-%s", msg.getBackupStorageUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                NoErrorCompletion com = new NoErrorCompletion(completion, chain) {
                    @Override
                    public void done() {
                        completion.done();
                        chain.next();
                    }
                };

                if (msg instanceof CreateVolumeBackupMsg) {
                    handleVolumeBackup((CreateVolumeBackupMsg) msg, com);
                } else if (msg instanceof CreateVmBackupMsg) {
                    handleVmBackup((CreateVmBackupMsg) msg, com);
                } else {
                    throw new CloudRuntimeException("should not be here");
                }
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            protected int getSyncLevel() {
                return Integer.parseInt(Optional.ofNullable(VolumeBackupSystemTag.LIVE_BACKUP_PARALLELISM
                        .getTokenByResourceUuid(msg.getBackupStorageUuid(), VolumeBackupSystemTag.DEGREE))
                        .orElse("10"));
            }
        });
    }

    private void handleVolumeBackup(CreateVolumeBackupMsg msg, NoErrorCompletion completion) {
        CreateVolumeBackupInnerMsg cmsg = new CreateVolumeBackupInnerMsg();
        cmsg.setResourceUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        cmsg.setAccountUuid(msg.getAccountUuid());
        cmsg.setVolumeUuid(msg.getVolumeUuid());
        cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        cmsg.setName(msg.getName());
        cmsg.setDescription(msg.getDescription());
        cmsg.setBackupQosStruct(msg.getBackupQosStruct());
        cmsg.setMode(msg.getMode());
        cmsg.setSystemTags(msg.getSystemTags());
        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeBackupConstant.SERVICE_ID, msg.getVolumeUuid());

        VolumeBackupOverlayMsg omsg = new VolumeBackupOverlayMsg();
        omsg.setVolumeUuid(msg.getVolumeUuid());
        omsg.setMessage(cmsg);
        bus.makeTargetServiceIdByResourceUuid(omsg, VolumeConstant.SERVICE_ID, omsg.getVolumeUuid());

        bus.send(omsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                CreateVolumeBackupReply r = new CreateVolumeBackupReply();
                if (!reply.isSuccess()) {
                    r.setError(reply.getError());
                } else {
                    CreateVolumeBackupInnerReply ir = reply.castReply();
                    r.setInventory(ir.getInventory());
                    r.setActualExecuteTime(ir.getActualExecuteTime());
                }

                bus.reply(msg, r);
                completion.done();
            }
        });
    }

    private void importImageInQueue(ImportImageMsg msg, ReturnValueCompletion<ImportImageReply> completion) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("import-image-parent-%s", msg.getParent());
            }

            @Override
            public void run(SyncTaskChain chain) {
                bus.send(msg, new CloudBusCallBack(msg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            chain.next();
                            return;
                        }

                        completion.success((ImportImageReply) reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void doImportImage(ImportImageMsg msg, ReturnValueCompletion<ImportImageReply> completion) {
        // create backup with full mode will get a backup info with null value in parent field
        if (Strings.isNotEmpty(msg.getParent())) {
            importImageInQueue(msg, completion);
            return;
        }

        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                completion.success((ImportImageReply) reply);
            }
        });
    }

    private void runCreateVolumeBackupFlow(CreateVolumeBackupInnerMsg msg, ReturnValueCompletion<VolumeBackupInventory> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("take-volume-backup-for-" + msg.getVolumeUuid());
        chain.enableProgressReport();
        chain.then(new ShareFlow() {
            BackupMode backupMode = msg.getMode();
            String uploadDir;
            final TakeBackupCmd cmd = new TakeBackupCmd();
            TakeBackupResponse rsp;
            ImportImageReply ireply;
            VolumeBackupVO vbvo;
            VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
            VolumeInfo info = getVolumeHostAndVM(volume);
            ImageStoreBackupStorageCommands.StorageInfo storageInfo = getStorageInfo(msg.getBackupStorageUuid());
            boolean nbdDefault = true;
            Tuple t;
            String nbdProcessDescription = null;
            VolumeBackupStorageRefVO refVO;
            String namespace;
            String volumeBackupUuid = msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid();

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "check-vm-qemu-version";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetVirtualizerInfoMsg gmsg = new GetVirtualizerInfoMsg();
                        gmsg.setHostUuid(info.hostUuid);
                        gmsg.setVmInstanceUuids(Collections.singletonList(info.vmUuid));
                        bus.makeLocalServiceId(gmsg, HostConstant.SERVICE_ID);
                        bus.send(gmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()){
                                    cmd.setPointInTime(false);
                                    nbdDefault = true;
                                    trigger.next();
                                    return;
                                }

                                GetVirtualizerInfoReply r = reply.castReply();
                                Optional<GetVirtualizerInfoReply.VmVirtualizerInfo> vmInfo =
                                        r.getVmInfoList().stream().filter(item -> item.getVmInstanceUuid().equals(info.vmUuid)).findAny();
                                if (!vmInfo.isPresent()) {
                                    cmd.setPointInTime(false);
                                    nbdDefault = true;
                                    trigger.next();
                                    return;
                                }

                                QemuKvmVersion version = new QemuKvmVersion(vmInfo.get().getCurrentQemuVersion());
                                if (version.supportMirrorBitmap()) {
                                    cmd.setPointInTime(false);
                                    nbdDefault = true;
                                } else {
                                    cmd.setPointInTime(true);
                                    nbdDefault = false;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "allocate-upload-workspace";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocateUploadWorkspaceMsg amsg = new AllocateUploadWorkspaceMsg();
                        amsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(amsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (reply.isSuccess()) {
                                            AllocateUploadWorkspaceReply r = reply.castReply();
                                            uploadDir = r.getUploadWorkspace();
                                            cmd.setBsPath(r.getBsInstallPath());
                                            trigger.next();
                                        } else {
                                            trigger.fail(reply.getError());
                                        }
                                    }
                                }
                        );
                    }
                });

                flow(new Flow() {
                    String __name__ = "prepare-volume-backup-records";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        vbvo = new VolumeBackupVO();
                        vbvo.setUuid(volumeBackupUuid);
                        vbvo.setType(Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).select(VolumeVO_.type).findValue());
                        vbvo.setAccountUuid(msg.getAccountUuid());
                        vbvo.setVolumeUuid(msg.getVolumeUuid());
                        vbvo.setName(msg.getName());
                        vbvo.setSize(0L);
                        vbvo.setMode(backupMode);
                        vbvo.setVmInstanceUuid(info.vmUuid);
                        vbvo.setDescription(msg.getDescription());
                        vbvo.setState(VolumeBackupState.Disabled);
                        vbvo.setStatus(VolumeBackupStatus.Creating);
                        vbvo = dbf.persistAndRefresh(vbvo);
                        tagMgr.createTags(msg.getSystemTags(), msg.getUserTags(), vbvo.getUuid(), VolumeBackupVO.class.getSimpleName());
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        dbf.removeByPrimaryKey(vbvo.getUuid(), VolumeBackupVO.class);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "clean-backup-history";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        t = getAndClearBackupHistory(msg.getVolumeUuid(), msg.getBackupStorageUuid());
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "before-create-volume-backup-extension";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        pluginRgty.getExtensionList(CreateVolumeBackupExtensionPoint.class).forEach(it ->
                                it.beforeCreateVolumeBackup(vbvo, msg.getBackupStorageUuid())
                        );
                        trigger.next();
                    }
                });

                if (backupMode != BackupMode.full) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "compute-backup-mode";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            backupMode = BackupMode.auto;

                            if (t == null) {
                                trigger.next();
                                return;
                            }

                            cmd.setLastBackup(t.get(0, String.class));
                            cmd.setBitmap(t.get(1, String.class));

                            GetImageChainInfoMsg gmsg = new GetImageChainInfoMsg();
                            gmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                            gmsg.setInstallPath(cmd.getLastBackup());
                            bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                            bus.send(gmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        GetImageChainInfoReply r = reply.castReply();
                                        if (r.getChain().size() >= VolumeBackupGlobalConfig.MAX_INCREMENTAL_BACKUP.value(Integer.class)) {
                                            backupMode = BackupMode.full;
                                        }

                                        trigger.next();
                                    } else {
                                        logger.warn(reply.getError().getDetails());
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        }
                    });
                }

                if (targetIsNbd()) {
                    flow(syncVolumeSize(Collections.singletonList(msg.getVolumeUuid())));
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "get-imagestore-backupstorage-cred";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetImageStoreBackupStorageDownloadCredentialMsg gmsg = new GetImageStoreBackupStorageDownloadCredentialMsg();
                        gmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        bus.send(gmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                prepareCmd(reply.castReply());
                                trigger.next();
                            }

                            @Transactional
                            private void prepareCmd(GetImageStoreBackupStorageDownloadCredentialReply greply) {
                                KVMHostVO host = Q.New(KVMHostVO.class).eq(KVMHostVO_.uuid, info.hostUuid).find();

                                cmd.setHostname(getBackupAddress(msg.getBackupStorageUuid()));
                                cmd.setUsername(greply.getUsername());
                                cmd.setPassword(greply.getPassword());
                                cmd.setSshPort(greply.getSshPort());

                                cmd.setUploadDir(uploadDir);
                                cmd.setVmUuid(info.vmUuid);
                                cmd.setVolume(VolumeTO.valueOf(VolumeInventory.valueOf(volume), KVMHostInventory.valueOf(host)));
                                cmd.setMaxIncremental(VolumeBackupGlobalConfig.MAX_INCREMENTAL_BACKUP.value(Integer.class));
                                cmd.setMode(backupMode.toString());
                                cmd.setStorageInfo(storageInfo);

                                final BackupQosStruct struct = msg.getBackupQosStruct();
                                if (struct != null) {
                                    cmd.setVolumeWriteBandwidth(struct.getVolumeWriteBandwidth());
                                    cmd.setNetworkWriteBandwidth(struct.getNetworkWriteBandwidth());
                                }
                                ResourceConfig rc = rcf.getResourceConfig(ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY.getIdentity());
                                cmd.setUploadConcurrency(rc.getResourceConfigValue(msg.getBackupStorageUuid(), Integer.class));
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "export-nbd-target-from-bs";

                    ExportNbdImagesReply r;

                    @Override
                    public boolean skip(Map data) {
                        return !targetIsNbd();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        Long vsize = Q.New(VolumeVO.class).select(VolumeVO_.size)
                                .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                                .findValue();

                        ExportNbdImagesMsg emsg = new ExportNbdImagesMsg();
                        emsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        emsg.setSizes(Collections.singletonList(vsize));
                        emsg.setWorkspace(uploadDir);
                        bus.makeLocalServiceId(emsg, BackupStorageConstant.SERVICE_ID);
                        bus.send(emsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                r = reply.castReply();
                                cmd.setStorageInfo(ImageStoreBackupStorageCommands.StorageInfo.valueOfUrl(
                                        String.format("nbd://%s:%d", cmd.getHostname(), r.getPorts().get(0))
                                ));
                                cmd.setBackupPath(r.getImagePaths().get(0));
                                nbdProcessDescription = r.getNbdDescription();
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback flowRollback, Map map) {
                        if (r == null) {
                            flowRollback.rollback();
                            return;
                        }

                        CancelExportNbdImagesMsg cmsg = new CancelExportNbdImagesMsg();
                        cmsg.setPorts(r.getPorts());
                        cmsg.setImagePaths(r.getImagePaths());
                        cmsg.setNbdDescription(r.getNbdDescription());
                        cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bus.makeLocalServiceId(cmsg, BackupStorageConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(flowRollback) {
                            @Override
                            public void run(MessageReply reply) {
                                flowRollback.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "generating-volume-backup";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        HasThreadContextAspect.setThreadContext(cmd); // context which set after new() has been invalid.
                        doCreateVolumeBackup(info, cmd,
                                new ReturnValueCompletion<TakeBackupResponse>(trigger) {
                                    @Override
                                    public void success(TakeBackupResponse returnValue) {
                                        if (returnValue.isSuccess()) {
                                            rsp = returnValue;
                                            trigger.next();
                                        } else {
                                            trigger.fail(operr("%s", returnValue.getError()));
                                        }
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                    }
                                });
                    }
                });

                flow(buildBackupVmHostFileForVolumeBackupFlow(info.vmUuid, info.hostUuid, volumeBackupUuid));

                flow(new Flow() {
                    String __name__ = "allocate-imagestore-install-path";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocateImageInstallPathMsg amsg = new AllocateImageInstallPathMsg();
                        amsg.setParentInstallPath(rsp.getParentInstallPath());
                        amsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(amsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            trigger.fail(reply.getError());
                                            return;
                                        }

                                        AllocateImageInstallPathReply r = reply.castReply();

                                        namespace = r.getName();

                                        refVO = new VolumeBackupStorageRefVO();
                                        refVO.setBackupStorageUuid(msg.getBackupStorageUuid());
                                        refVO.setVolumeBackupUuid(vbvo.getUuid());
                                        refVO.setInstallPath(r.getInstallPath());
                                        refVO.setStatus(VolumeBackupStatus.Creating);
                                        dbf.persist(refVO);

                                        trigger.next();
                                    }
                                }
                        );
                    }

                    @Override
                    public void rollback(FlowRollback flowRollback, Map map) {
                        if (refVO != null) {
                            dbf.remove(refVO);
                        }
                        flowRollback.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "importing-volume-backup";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ImportImageMsg imsg = new ImportImageMsg();
                        imsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        imsg.setParent(rsp.getParentInstallPath());
                        imsg.setFilename(rsp.getBackupFile());
                        imsg.setProcessToRelease(nbdProcessDescription);
                        imsg.setName(namespace);
                        bus.makeTargetServiceIdByResourceUuid(imsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        doImportImage(imsg, new ReturnValueCompletion<ImportImageReply>(imsg, trigger) {
                            @Override
                            public void success(ImportImageReply reply) {
                                ireply = reply;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                vbvo.setState(VolumeBackupState.Enabled);
                                vbvo.setStatus(VolumeBackupStatus.Ready);
                                vbvo.setSize(ireply.getSize());
                                vbvo.setMetadata(VolumeMetaDataMaker.generateDataVolumeMetadata(vbvo.getVolumeUuid()));
                                vbvo.setMode(backupMode);
                                merge(vbvo);

                                refVO.setInstallPath(ireply.getInstallPath());
                                refVO.setStatus(VolumeBackupStatus.Ready);
                                merge(refVO);

                                VolumeBackupHistoryVO hvo = new VolumeBackupHistoryVO();
                                hvo.setBitmap(rsp.getBitmap());
                                hvo.setLastBackupUuid(vbvo.getUuid());
                                hvo.setUuid(vbvo.getVolumeUuid());

                                persist(hvo);
                            }
                        }.execute();

                        vbvo = dbf.reload(vbvo);
                        afterCreateBackup(vbvo);
                        completion.success(VolumeBackupInventory.valueOf(vbvo));
                    }

                    private void afterCreateBackup(VolumeBackupVO vo) {
                        pluginRgty.getExtensionList(CreateVolumeBackupExtensionPoint.class).forEach(new Consumer<CreateVolumeBackupExtensionPoint>() {
                            @Override
                            @ExceptionSafe
                            public void accept(CreateVolumeBackupExtensionPoint ext) {
                                ext.afterCreateVolumeBackup(vo, msg.getBackupStorageUuid());
                            }
                        });
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }

            private boolean targetIsNbd() {
                return (storageInfo == null && nbdDefault) || (storageInfo != null && storageInfo.getType().equals("nbd"));
            }
        }).start();
    }

    private Flow syncVolumeSize(Collection<String> volumeUuids) {
        return new NoRollbackFlow() {
            String __name__ = "sync-volumes-size";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(volumeUuids).step((volUuid, compl) ->{
                    SyncVolumeSizeMsg smsg = new SyncVolumeSizeMsg();
                    smsg.setVolumeUuid(volUuid);
                    bus.makeLocalServiceId(smsg, VolumeConstant.SERVICE_ID);
                    bus.send(smsg, new CloudBusCallBack(compl) {
                        @Override
                        public void run(MessageReply reply) {
                            compl.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        };
    }

    public static String getBackupAddress(String bsUuid) {
        String mnAddress = Q.New(ImageStoreBackupStorageVO.class).select(ImageStoreBackupStorageVO_.hostname)
                .eq(ImageStoreBackupStorageVO_.uuid, bsUuid)
                .findValue();
        String backupAddress = ImageStoreBackupStorage.getNetworkAddress(bsUuid,
                ImageStoreSystemTags.BACKUP_CIDR, ImageStoreSystemTags.BACKUP_CIDR_TOKEN);

        return backupAddress != null ? backupAddress : mnAddress;
    }

    private ImageStoreBackupStorageCommands.StorageInfo getStorageInfo(String bsUuid) {
        String tag = ImageStoreSystemTags.STORAGE_INFO.getTag(bsUuid);
        if (tag == null) {
            return null;
        }

        return ImageStoreBackupStorageCommands.StorageInfo.valueOf(tag);
    }

    private void handle(CancelVolumeBackupJobMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("cancel-volume-backup-%s", msg.getVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                CancelVolumeBackupJobReply reply = new CancelVolumeBackupJobReply();
                VmInstanceVO vm = getVmInstance();

                cancelBackupJob(vm.getUuid(), msg.getVolumeUuid(), vm.getHostUuid(), new Completion(msg) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setSuccess(false);
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            private VmInstanceVO getVmInstance() {
                String sql = "select vm from VmInstanceVO vm, VolumeVO volume " +
                        "where vm.uuid = volume.vmInstanceUuid and volume.uuid = :volumeUuid";
                return SQL.New(sql, VmInstanceVO.class).param("volumeUuid", msg.getVolumeUuid()).find();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(CancelVmBackupJobMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("cancel-volume-backup-%s", msg.getVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                CancelVolumeBackupJobReply reply = new CancelVolumeBackupJobReply();

                Tuple tuple = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.rootVolumeUuid, msg.getVolumeUuid())
                        .select(VmInstanceVO_.uuid, VmInstanceVO_.hostUuid)
                        .findTuple();

                cancelBackupJobs(tuple.get(0, String.class), tuple.get(1, String.class), new Completion(msg) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setSuccess(false);
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    class BackupVmHostFileForVolumeBackupFlow implements Flow {
        String __name__ = "backup-vm-host-files";
        String vmUuid;
        String hostUuidBackupFrom;
        String volumeBackupUuid;

        public boolean skip(Map data) {
            return VolumeBackupGlobalProperty.SKIP_TPM_VOLUME_BACKUP;
        }

        @Override
        public void run(FlowTrigger trigger, Map data) {
            if (vmUuid == null || hostUuidBackupFrom == null) {
                trigger.next();
                return;
            }

            final List<VmHostFileVO> vmHostFileList = Q.New(VmHostFileVO.class)
                    .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                    .eq(VmHostFileVO_.hostUuid, hostUuidBackupFrom)
                    .list();
            if (vmHostFileList.isEmpty()) {
                trigger.next();
                return;
            }

            SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
            syncMsg.setHostUuid(hostUuidBackupFrom);
            syncMsg.setVmUuid(vmUuid);
            syncMsg.setSyncToBackup(true);
            syncMsg.setBackupResourceUuid(volumeBackupUuid);
            syncMsg.setSyncReason(VmHostFileSyncReason.VolumeBackup.reason());

            VmHostFileVO tpmFile = findOneOrNull(vmHostFileList, it -> it.getType() == VmHostFileType.TpmState);
            if (tpmFile != null) {
                syncMsg.setTpmStateFolder(tpmFile.getPath());
            }

            VmHostFileVO nvRamFile = findOneOrNull(vmHostFileList, it -> it.getType() == VmHostFileType.NvRam);
            if (nvRamFile != null) {
                syncMsg.setNvRamPath(nvRamFile.getPath());
            }

            bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
            bus.send(syncMsg, new CloudBusCallBack(trigger) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        trigger.next();
                        return;
                    }
                    trigger.fail(reply.getError());
                }
            });
        }

        @Override
        public void rollback(FlowRollback trigger, Map data) {
            secureBootManager.cleanVmHostBackupFile(volumeBackupUuid);
            trigger.rollback();
        }
    }

    private Flow buildBackupVmHostFileForVolumeBackupFlow(String vmUuid, String hostUuidBackupFrom, String volumeBackupUuid) {
        BackupVmHostFileForVolumeBackupFlow flow = new BackupVmHostFileForVolumeBackupFlow();
        flow.vmUuid = vmUuid;
        flow.hostUuidBackupFrom = hostUuidBackupFrom;
        flow.volumeBackupUuid = volumeBackupUuid;
        return flow;
    }
}
