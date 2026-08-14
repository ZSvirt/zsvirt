package org.zstack.kvm.efi;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.PreVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.AfterReimageVmInstanceExtensionPoint;
import org.zstack.header.vm.BeforeHaStartVmInstanceExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.ConvertVmInstanceToTemplatedVmExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstantiateResourceException;
import org.zstack.header.vm.VmMigrationType;
import org.zstack.header.vm.VmPreMigrationExtensionPoint;
import org.zstack.header.vm.VmReleaseResourceExtensionPoint;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileContentVO;
import org.zstack.header.vm.additions.VmHostFileContentVO_;
import org.zstack.header.vm.additions.VmHostFileOperation;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.header.storage.snapshot.ConsistentType;
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotOverlayInnerMsg;
import org.zstack.header.storage.snapshot.TakeVolumesSnapshotOnKvmReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotCreationExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.vm.devices.NvRamSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmExtensionPoint;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.KvmResponseWrapper;
import org.zstack.kvm.VolumeTO;
import org.zstack.kvm.vmfiles.message.SyncVmHostFilesFromHostMsg;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.zstack.core.Platform.operr;
import static org.zstack.header.vm.additions.VmHostFileSyncReason.*;
import static org.zstack.header.vm.additions.VmHostFileType.NvRam;
import static org.zstack.kvm.KVMConstant.*;
import static org.zstack.utils.CollectionDSL.list;

public class KvmSecureBootExtensions implements KVMStartVmExtensionPoint,
        PreVmInstantiateResourceExtensionPoint,
        VmPreMigrationExtensionPoint,
        AfterReimageVmInstanceExtensionPoint,
        VmReleaseResourceExtensionPoint,
        VmInstanceMigrateExtensionPoint,
        VolumeSnapshotCreationExtensionPoint,
        BeforeHaStartVmInstanceExtensionPoint,
        ConvertVmInstanceToTemplatedVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmSecureBootExtensions.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;
    @Autowired
    private DatabaseFacade databaseFacade;
    @Autowired
    private KvmVmHostFileFactory vmHostFileFactory;
    @Autowired
    private KvmSecureBootManager secureBootManager;

    private final Object hostFileLock = new Object();

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (isUefiBootMode(cmd.getBootMode())) {
            ResourceConfig resourceConfig;
            resourceConfig = resourceConfigFacade.getResourceConfig(VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT.getIdentity());
            cmd.setSecureBoot(resourceConfig.getResourceConfigValue(spec.getVmInventory().getUuid(), Boolean.class));

            resourceConfig = resourceConfigFacade.getResourceConfig(KVMGlobalConfig.VM_EDK_VERSION_CONFIG.getIdentity());
            final String edkVersion = resourceConfig.getResourceConfigValue(spec.getVmInventory().getUuid(), String.class);
            if (!Objects.equals(edkVersion, EDK_VERSION_NONE)) {
                cmd.setEdkVersion(edkVersion);
            }
        }

        final NvRamSpec nvRam = spec.getDevicesSpec().getNvRam();
        if (nvRam != null && nvRam.isNeedRegister()) {
            prepareNvRamToStartVmCmd(cmd, nvRam, host);
        }
    }

    private void prepareNvRamToStartVmCmd(KVMAgentCommands.StartVmCmd cmd, NvRamSpec nvRam, KVMHostInventory host) {
        VolumeTO volume = new VolumeTO();
        volume.setDeviceType(VolumeTO.FILE);
        volume.setInstallPath(buildNvramFilePath(cmd.getVmInstanceUuid()));
        volume.setVolumeUuid(null); // not a volume
        cmd.setNvRam(volume);

        synchronized (hostFileLock) {
            final Timestamp now = Timestamp.from(Instant.now());
            VmHostFileVO nvRamFile = Q.New(VmHostFileVO.class)
                    .eq(VmHostFileVO_.vmInstanceUuid, cmd.getVmInstanceUuid())
                    .eq(VmHostFileVO_.type, NvRam)
                    .eq(VmHostFileVO_.hostUuid, host.getUuid())
                    .find();
            if (nvRamFile == null) {
                nvRamFile = new VmHostFileVO();
                nvRamFile.setUuid(Platform.getUuid());
                nvRamFile.setHostUuid(host.getUuid());
                nvRamFile.setVmInstanceUuid(cmd.getVmInstanceUuid());
                nvRamFile.setType(NvRam);
                nvRamFile.setPath(volume.getInstallPath());
                nvRamFile.setCreateDate(now);
                nvRamFile.setResourceName("NvRam file for " + cmd.getVmInstanceUuid());
                databaseFacade.persist(nvRamFile);
            } else {
                SQL.New(VmHostFileVO.class)
                        .eq(VmHostFileVO_.uuid, nvRamFile.getUuid())
                        .set(VmHostFileVO_.path, volume.getInstallPath())
                        .set(VmHostFileVO_.lastOpDate, now)
                        .update();
            }
        }
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid, Completion completion) {
        prepareNvRamBeforeMigration(inv, destHostUuid, completion);
    }

    @Override
    public void preVmMigration(VmInstanceInventory vm, VmMigrationType type, String dstHostUuid, Completion completion) {
        completion.success(); // use preMigrateVm instead of preVmMigration to prevent from handle twice
    }

    private void prepareNvRamBeforeMigration(VmInstanceInventory vm, String dstHostUuid, Completion completion) {
        if (dstHostUuid == null) {
            completion.success();
            return;
        }

        boolean needRegisterNvRam = vmHostFileFactory.needRegister(NvRam, vm.getUuid());
        if (!needRegisterNvRam) {
            completion.success();
            return;
        }

        SimpleFlowChain.of("prepare-nvram-before-vm-" + vm.getUuid() + "-migrate")
                .then("sync-vm-host-files-before-migration", trigger -> {
                    String vmUuid = vm.getUuid();

                    Tuple tuple = Q.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, vmUuid)
                            .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                            .findTuple();
                    String hostUuid = tuple.get(0, String.class);
                    if (hostUuid == null) {
                        hostUuid = tuple.get(1, String.class);
                    }

                    List<VmHostFileVO> fileList = Q.New(VmHostFileVO.class)
                            .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                            .eq(VmHostFileVO_.hostUuid, hostUuid)
                            .list();

                    if (fileList.isEmpty()) {
                        trigger.next();
                        return;
                    }

                    if (fileList.stream().allMatch(it -> it.getChangeDate() == null)) {
                        logger.debug(String.format("skip syncing VM host files before migration for VM[uuid=%s], " +
                                "All files are clean (changeDate is null)", vmUuid));
                        trigger.next();
                        return;
                    }

                    SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
                    syncMsg.setHostUuid(hostUuid);
                    syncMsg.setVmUuid(vmUuid);
                    syncMsg.setSyncReason(PreMigration.reason());

                    for (VmHostFileVO file : fileList) {
                        if (file.getType() == NvRam) {
                            syncMsg.setNvRamPath(file.getPath());
                        } else if (file.getType() == VmHostFileType.TpmState) {
                            syncMsg.setTpmStateFolder(file.getPath());
                        }
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
                })
                .then("prepare-nvram-folder-on-dest-host", trigger -> {
                    VmHostFileTO to = new VmHostFileTO();
                    to.setPath(buildNvramFilePath(vm.getUuid()));
                    to.setType(NvRam.toString());
                    to.setOperation(VmHostFileOperation.Prepare.toString());

                    RewriteVmHostFilesContext context = new RewriteVmHostFilesContext();
                    context.hostUuid = dstHostUuid;
                    context.hostFiles = list(to);

                    rewriteVmHostFiles(context, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                })
                // TpmState folder is not needed to prepare
                .propagateExceptionTo(completion)
                .done(completion::success)
                .error(completion::fail)
                .start();
    }

    public static class RewriteVmHostFilesContext {
        public String hostUuid;
        public List<KVMAgentCommands.VmHostFileTO> hostFiles;
    }

    public void rewriteVmHostFiles(RewriteVmHostFilesContext context, Completion completion) {
        KvmCommandSender sender = new KvmCommandSender(context.hostUuid);
        KVMAgentCommands.WriteVmHostFileContentCmd cmd = new KVMAgentCommands.WriteVmHostFileContentCmd();
        cmd.setHostFiles(context.hostFiles);

        sender.send(cmd, WRITE_VM_HOST_FILE_PATH, wrapper -> {
            KVMAgentCommands.WriteVmHostFileContentResponse writeRsp = wrapper.getResponse(KVMAgentCommands.WriteVmHostFileContentResponse.class);
            return writeRsp.isSuccess() ? null :
                    operr("failed to write file content response").withException(writeRsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                KVMAgentCommands.WriteVmHostFileContentResponse writeRsp = wrapper.getResponse(KVMAgentCommands.WriteVmHostFileContentResponse.class);
                if (!writeRsp.isSuccess()) {
                    completion.fail(operr("failed to write file content response").withException(writeRsp.getError()));
                    return;
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
        // do-nothing
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
        // do-nothing
    }

    private boolean isUefiBootMode(String bootMode) {
        return VmTpmManager.isUefiBootMode(bootMode);
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
        // do-nothing
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        final NvRamSpec nvRamSpec = spec.getDevicesSpec() == null ? null : spec.getDevicesSpec().getNvRam();
        if (nvRamSpec == null) {
            completion.success();
            return;
        }

        PrepareHostFileContext context = new PrepareHostFileContext();
        context.hostUuid = spec.getDestHost().getUuid();
        context.vmUuid = spec.getVmInventory().getUuid();
        context.type = NvRam;
        context.backupUuid = nvRamSpec.getBackupFileUuid();
        context.syncReason = "pre-instantiate VM resource";
        prepareHostFileOnHost(context, completion);
    }

    public static class PrepareHostFileContext {
        public String hostUuid;
        public String vmUuid;
        public VmHostFileType type;
        public String backupUuid;
        public String syncReason;

        public String path;
        // whether the NvRam is on the same host as before
        private boolean sameHost = false;
        private boolean syncFromOriginHostSuccess = false;
        private boolean writeSuccess = false;
        private VmHostFileVO vmHostFile;
        private VmHostBackupFileVO vmBackupFileVO;

        // property: VmHostBackupFileVO (when "backupUuid" is set)
        //             > VmHostFileVO (read success)
        //             > VmHostFileVO (read fail, but has content)
        //             > VmHostBackupFileVO (vmInstanceUuid matched)  <-  read this only if VmHostFileVO is not exist
    }

    @SuppressWarnings("rawtypes")
    public void prepareHostFileOnHost(PrepareHostFileContext context, Completion completion) {
        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setName("prepare-vm-host-file");
        chain.then(new NoRollbackFlow() {
            String __name__ = "read-vm-host-file-from-origin-host";

            @Override
            public boolean skip(Map data) {
                return StringUtils.isNotBlank(context.backupUuid);
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmHostFileVO vmHostFile = context.vmHostFile = Q.New(VmHostFileVO.class)
                        .eq(VmHostFileVO_.type, context.type)
                        .eq(VmHostFileVO_.vmInstanceUuid, context.vmUuid)
                        .orderByDesc(VmHostFileVO_.lastOpDate)
                        .limit(1)
                        .find();
                if (vmHostFile == null) {
                    logger.debug(String.format("skip to read/write %s host file for VM[vmUuid=%s]: file is not registered in MN",
                            context.type, context.vmUuid));
                    trigger.next();
                    return;
                }

                context.sameHost = vmHostFile.getHostUuid().equals(context.hostUuid);
                SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
                syncMsg.setHostUuid(vmHostFile.getHostUuid());
                syncMsg.setVmUuid(context.vmUuid);
                syncMsg.setSyncReason(PrepareRead.reason(context.syncReason));

                if (vmHostFile.getType() == NvRam) {
                    context.path = vmHostFile.getPath();
                    syncMsg.setNvRamPath(context.path);
                } else if (vmHostFile.getType() == VmHostFileType.TpmState) {
                    context.path = vmHostFile.getPath();
                    syncMsg.setTpmStateFolder(context.path);
                } else {
                    throw new CloudRuntimeException("unsupported vm host file type: " + vmHostFile.getType());
                }

                bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                bus.send(syncMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            context.syncFromOriginHostSuccess = true;
                            trigger.next();
                            return;
                        }

                        logger.warn(String.format("failed to read vm host file for VM[vmUuid=%s] but still continue: %s",
                                context.vmUuid, reply.getError().getReadableDetails()));
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "check-content-for-vm-host-file-if-sync-failed";

            @Override
            public boolean skip(Map data) {
                return context.syncFromOriginHostSuccess || context.vmHostFile == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                Timestamp latestChangeDate = Q.New(VmHostFileVO.class)
                        .select(VmHostFileVO_.changeDate)
                        .eq(VmHostFileVO_.uuid, context.vmHostFile.getUuid())
                        .findValue();
                if (latestChangeDate != null && !context.sameHost) {
                    trigger.fail(operr(
                            "vm host file content on management node may be stale: change is pending on host "
                                    + "but sync from origin host failed, cannot use cached content[vmUuid=%s, type=%s]",
                            context.vmUuid, context.type));
                    return;
                }

                if (!Q.New(VmHostFileContentVO.class)
                        .eq(VmHostFileContentVO_.uuid, context.vmHostFile.getUuid())
                        .isExists()) {
                    logger.debug(String.format(
                            "VmHostFileVO[vmUuid=%s, hostUuid=%s, type=%s] has no content",
                            context.vmUuid, context.hostUuid, context.type));
                    context.vmHostFile = null;
                }
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "read-vm-host-file-from-backup";

            @Override
            public boolean skip(Map data) {
                return context.vmHostFile != null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (context.backupUuid == null) {
                    context.vmBackupFileVO = Q.New(VmHostBackupFileVO.class)
                            .eq(VmHostBackupFileVO_.type, context.type)
                            .eq(VmHostBackupFileVO_.resourceUuid, context.vmUuid)
                            .orderByDesc(VmHostBackupFileVO_.lastOpDate)
                            .limit(1)
                            .find();
                } else {
                    context.vmBackupFileVO = Q.New(VmHostBackupFileVO.class)
                            .eq(VmHostBackupFileVO_.uuid, context.backupUuid)
                            .find();
                    if (context.vmBackupFileVO == null) {
                        trigger.fail(operr("cannot find matched vm-host backup file[backupUuid:%s]",
                                context.backupUuid));
                        return;
                    }
                }
                if (context.vmBackupFileVO != null) {
                    logger.debug(String.format("use %s[type=%s] VM-host backup file for VM[uuid=%s]",
                            context.vmBackupFileVO.getUuid(), context.type, context.vmUuid));
                    context.path = buildPathForVmHostFileType(context.type, context.vmUuid);
                }
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "write-vm-host-file-to-dest-host";

            @Override
            public boolean skip(Map data) {
                return (context.vmHostFile == null && context.vmBackupFileVO == null)
                        || (context.sameHost && context.vmHostFile != null);
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String contentUuid = context.vmHostFile == null ?
                        context.vmBackupFileVO.getUuid() : context.vmHostFile.getUuid();
                VmHostFileContentVO content = Q.New(VmHostFileContentVO.class)
                        .eq(VmHostFileContentVO_.uuid, contentUuid)
                        .find();
                if (content == null) {
                    logger.debug(String.format("skip to write vm host file for VM[vmUuid=%s]: file content is not saved in MN",
                            context.vmUuid));
                    trigger.next();
                    return;
                }

                VmHostFileTO to = new VmHostFileTO();
                to.setPath(context.path);
                to.setType(context.type.toString());
                to.setFileFormat(content.getFormat().toString());
                to.setOperation(VmHostFileOperation.Write.toString());

                String contentBase64 = Base64.getEncoder().encodeToString(content.getContent());
                to.setContentBase64(contentBase64);

                RewriteVmHostFilesContext rewriteContext = new RewriteVmHostFilesContext();
                rewriteContext.hostUuid = context.hostUuid;
                rewriteContext.hostFiles = Collections.singletonList(to);

                rewriteVmHostFiles(rewriteContext, new Completion(trigger) {
                    @Override
                    public void success() {
                        context.writeSuccess = true;
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "re-read-vm-host-file-from-dest-host";

            @Override
            public boolean skip(Map data) {
                return !context.writeSuccess;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
                syncMsg.setHostUuid(context.hostUuid);
                syncMsg.setVmUuid(context.vmUuid);
                syncMsg.setSyncReason(PrepareReRead.reason(context.syncReason));

                if (context.type == NvRam) {
                    syncMsg.setNvRamPath(context.path);
                } else if (context.type == VmHostFileType.TpmState) {
                    syncMsg.setTpmStateFolder(context.path);
                }

                bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                bus.send(syncMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

    static class NvRamVolumeContext {
        String vmUuid;
        DiskAO nvRamSpec;
        VmInstanceSpec spec;

        VolumeInventory inventory;
    }

    @Override
    public void afterReimageVmInstance(VolumeInventory inventory) {
        String vmUuid = inventory.getVmInstanceUuid();
        if (vmUuid == null) {
            return;
        }

        SQL.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .delete();
        secureBootManager.cleanVmHostBackupFile(vmUuid);

        logger.debug(String.format("reset TPM state for VM[uuid:%s] after reimage: " +
                "deleted all VmHostFileVO and VmHostBackupFileVO records", vmUuid));
    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {
        if (spec.getDestHost() == null) {
            completion.success();
            return;
        }

        String hostUuid = spec.getDestHost().getUuid();
        String vmUuid = spec.getVmInventory().getUuid();
        List<VmHostFileVO> vmHostFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .eq(VmHostFileVO_.hostUuid, hostUuid)
                .list();
        if (vmHostFiles.isEmpty()) {
            completion.success();
            return;
        }

        logger.info(String.format("try to sync VM host file[vmUuid=%s] from host[uuid=%s]", vmUuid, hostUuid));
        SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
        syncMsg.setHostUuid(hostUuid);
        syncMsg.setVmUuid(vmUuid);
        syncMsg.setSyncReason(ResourceRelease.reason());

        for (VmHostFileVO file : vmHostFiles) {
            if (file.getType() == NvRam) {
                syncMsg.setNvRamPath(file.getPath());
            } else if (file.getType() == VmHostFileType.TpmState) {
                syncMsg.setTpmStateFolder(file.getPath());
            } else {
                logger.warn(String.format("unsupported vm host file type: %s, skip syncing for VM[uuid:%s] from host[uuid:%s]",
                        file.getType(), vmUuid, hostUuid));
            }
        }

        bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
        bus.send(syncMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to sync VM host file[vmUuid=%s] from host[uuid=%s] but still continue: %s",
                            vmUuid, hostUuid, reply.getError().getReadableDetails()));
                }
                completion.success();
            }
        });
    }

    @Override
    public void beforeHaStartVmInstance(String vmUuid, String judgerClassName, List<String> softAvoidHostUuids, Completion completion) {
        Tuple hostUuidTuple = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .findTuple();
        if (hostUuidTuple == null) {
            completion.success();
            return;
        }

        String hostUuid = hostUuidTuple.get(0, String.class);
        final String finalHostUuid = (hostUuid == null) ? hostUuidTuple.get(1, String.class) : hostUuid;
        if (finalHostUuid == null) {
            logger.debug("skip HA file sync: VM has no host/lastHost record");
            completion.success();
            return;
        }

        List<VmHostFileVO> vmHostFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .eq(VmHostFileVO_.hostUuid, finalHostUuid)
                .list();
        if (vmHostFiles.isEmpty()) {
            completion.success();
            return;
        }

        logger.info(String.format("try to sync VM host file[vmUuid=%s] from last host[uuid=%s] before HA start",
                vmUuid, finalHostUuid));
        SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
        syncMsg.setHostUuid(finalHostUuid);
        syncMsg.setVmUuid(vmUuid);
        syncMsg.setSyncReason(BeforeHaStart.reason());

        for (VmHostFileVO file : vmHostFiles) {
            if (file.getType() == NvRam) {
                syncMsg.setNvRamPath(file.getPath());
            } else if (file.getType() == VmHostFileType.TpmState) {
                syncMsg.setTpmStateFolder(file.getPath());
            } else {
                logger.warn(String.format(
                        "unsupported vm host file type: %s, skip syncing for VM[uuid:%s] from host[uuid:%s]",
                        file.getType(), vmUuid, finalHostUuid));
            }
        }

        bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
        bus.send(syncMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to sync VM host file[vmUuid=%s] from last host[uuid=%s] before HA start, but tolerated: %s",
                            vmUuid, finalHostUuid, reply.getError().getReadableDetails()));
                }
                completion.success();
            }
        });
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid, NoErrorCompletion completion) {
        String destHostUuid = inv.getHostUuid();
        String vmUuid = inv.getUuid();

        List<VmHostFileVO> vmHostFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .eq(VmHostFileVO_.hostUuid, srcHostUuid)
                .list();
        if (vmHostFiles.isEmpty()) {
            completion.done();
            return;
        }

        // clean up stale VmHostFileVO/VmHostFileContentVO on dest host
        // before sync creates new records
        List<VmHostFileVO> staleFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .eq(VmHostFileVO_.hostUuid, destHostUuid)
                .list();
        if (!staleFiles.isEmpty()) {
            List<String> staleUuids = staleFiles.stream()
                    .map(VmHostFileVO::getUuid)
                    .collect(java.util.stream.Collectors.toList());
            SQL.New(VmHostFileContentVO.class)
                    .in(VmHostFileContentVO_.uuid, staleUuids)
                    .delete();
            SQL.New(VmHostFileVO.class)
                    .in(VmHostFileVO_.uuid, staleUuids)
                    .delete();
            logger.debug(String.format("cleaned up %d stale VmHostFileVO/Content on dest host[uuid=%s] for VM[uuid=%s]",
                    staleFiles.size(), destHostUuid, vmUuid));
        }

        logger.info(String.format("sync VM host file[vmUuid=%s] from dest host[uuid=%s] after migration",
                vmUuid, destHostUuid));
        SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
        syncMsg.setHostUuid(destHostUuid);
        syncMsg.setVmUuid(vmUuid);
        syncMsg.setSyncReason(PostMigration.reason());

        for (VmHostFileVO file : vmHostFiles) {
            if (file.getType() == NvRam) {
                syncMsg.setNvRamPath(file.getPath());
            } else if (file.getType() == VmHostFileType.TpmState) {
                syncMsg.setTpmStateFolder(file.getPath());
            } else {
                logger.warn(String.format("unsupported vm host file type: %s, skip syncing for VM[uuid:%s]",
                        file.getType(), vmUuid));
            }
        }

        bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
        bus.send(syncMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to sync VM host file[vmUuid=%s] from host[uuid=%s] after migration, but tolerated: %s",
                            vmUuid, destHostUuid, reply.getError().getReadableDetails()));
                }
                completion.done();
            }
        });
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreatedOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg,
                                                             TakeVolumesSnapshotOnKvmReply treply,
                                                             Completion completion) {
        if (treply == null || !treply.isSuccess()) {
            completion.success();
            return;
        }

        if (!msg.isBackupHostFileIfNeeded()) {
            completion.success();
            return;
        }

        String vmUuid = msg.getLockedVmInstanceUuids().get(0);
        String hostUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hostUuid)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .findValue();

        List<VmHostFileVO> hostFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .eq(VmHostFileVO_.hostUuid, hostUuid)
                .list();

        if (hostFiles.isEmpty()) {
            completion.success();
            return;
        }

        String tempResourceUuid = Platform.getUuid();

        SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
        syncMsg.setVmUuid(vmUuid);
        syncMsg.setHostUuid(hostUuid);

        for (VmHostFileVO file : hostFiles) {
            if (file.getType() == NvRam) {
                syncMsg.setNvRamPath(buildNvramSnapshotBackupFilePath(vmUuid));
            } else if (file.getType() == VmHostFileType.TpmState) {
                syncMsg.setTpmStateFolder(buildTpmStateSnapshotBackupFilePath(vmUuid));
            }
        }

        syncMsg.setSyncReason(SnapshotGroupOnlineBackup.reason());
        syncMsg.setSyncToBackup(true);
        syncMsg.setBackupResourceUuid(tempResourceUuid);
        bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
        bus.send(syncMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    treply.setHostBackupTempResourceUuid(tempResourceUuid);
                    logger.debug(String.format("synced backup host files for vm[uuid:%s] to VmHostBackupFileVO[resourceUuid:%s]",
                            vmUuid, tempResourceUuid));
                } else {
                    logger.warn(String.format("failed to sync backup host files for vm[uuid:%s] during online snapshot, " +
                            "but tolerated: %s", vmUuid, reply.getError().getReadableDetails()));
                }
                completion.success();
            }
        });
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreationFailsOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg,
                                                                    TakeVolumesSnapshotOnKvmReply treply) {
        // No cleanup needed — backup files on agent side are ephemeral
    }

    @Override
    public void afterVolumeSnapshotGroupCreated(VolumeSnapshotGroupInventory snapshotGroup,
                                                 ConsistentType consistentType,
                                                 Completion completion) {
        completion.success();
    }

    @Override
    public void afterVolumeSnapshotCreated(VolumeSnapshotInventory snapshot, Completion completion) {
        completion.success();
    }

    @Override
    public void beforeConvertVmInstanceToTemplatedVm(String vmUuid, Completion completion) {
        boolean needRegisterNvRam = !vmHostFileFactory.vmHostFileTypeNeedRegisterForVm(vmUuid).isEmpty();
        if (!needRegisterNvRam) {
            completion.success();
            return;
        }

        Tuple tuple = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.state, VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                .findTuple();
        VmInstanceState vmState = tuple.get(0, VmInstanceState.class);
        String hostUuid = tuple.get(1, String.class);
        if (hostUuid == null) {
            hostUuid = tuple.get(2, String.class);
        }

        List<VmHostFileVO> fileList = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .eq(VmHostFileVO_.hostUuid, hostUuid)
                .list();
        if (fileList.isEmpty()) {
            completion.success();
            return;
        }

        if (vmState == VmInstanceState.Stopped && fileList.stream().allMatch(it -> it.getChangeDate() == null)) {
            completion.success();
            return;
        }

        SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
        syncMsg.setHostUuid(hostUuid);
        syncMsg.setVmUuid(vmUuid);
        syncMsg.setSyncReason(ConventToTemplatedVM.reason());

        for (VmHostFileVO file : fileList) {
            if (file.getType() == NvRam) {
                syncMsg.setNvRamPath(file.getPath());
            } else if (file.getType() == VmHostFileType.TpmState) {
                syncMsg.setTpmStateFolder(file.getPath());
            }
        }

        bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
        bus.send(syncMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                    return;
                }
                completion.fail(reply.getError());
            }
        });
    }
}
