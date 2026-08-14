package org.zstack.kvm.efi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmCanonicalEvents;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.additions.RestoreVmHostFileMsg;
import org.zstack.header.vm.additions.RestoreVmHostFileReply;
import org.zstack.header.vm.additions.VmHostBackupFileDeletionMsg;
import org.zstack.header.vm.additions.VmHostBackupFileDeletionReply;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileContentFormat;
import org.zstack.header.vm.additions.VmHostFileContentVO;
import org.zstack.header.vm.additions.VmHostFileContentVO_;
import org.zstack.header.vm.additions.VmHostFileDeletionMsg;
import org.zstack.header.vm.additions.VmHostFileDeletionReply;
import org.zstack.header.vm.additions.VmHostFileManager;
import org.zstack.header.vm.additions.VmHostFileOperation;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.KvmResponseWrapper;
import org.zstack.kvm.vmfiles.message.BackupVmHostFileMsg;
import org.zstack.kvm.vmfiles.message.BackupVmHostFileOnHypervisorMsg;
import org.zstack.kvm.vmfiles.message.BackupVmHostFileOnHypervisorReply;
import org.zstack.kvm.vmfiles.message.BackupVmHostFileReply;
import org.zstack.kvm.vmfiles.message.CloneVmHostFileMsg;
import org.zstack.kvm.vmfiles.message.CloneVmHostFileReply;
import org.zstack.kvm.vmfiles.message.SyncVmHostFilesFromHostMsg;
import org.zstack.kvm.vmfiles.message.SyncVmHostFilesFromHostReply;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static org.zstack.compute.vm.VmGlobalConfig.RESET_TPM_AFTER_VM_CLONE;
import static org.zstack.core.Platform.operr;
import static org.zstack.header.vm.additions.VmHostFileSyncReason.PostClone;
import static org.zstack.header.vm.additions.VmHostFileSyncReason.Restore;
import static org.zstack.header.vm.additions.VmHostFileSyncReason.VmShutdown;
import static org.zstack.kvm.KVMAgentCommands.*;
import static org.zstack.kvm.KVMConstant.*;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.*;

public class KvmSecureBootManager extends AbstractService implements VmHostFileManager {
    private static final CLogger logger = Utils.getLogger(KvmSecureBootManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade databaseFacade;
    @Autowired
    private EventFacadeImpl eventFacade;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;
    @Autowired
    private KvmVmHostFileFactory vmHostFileFactory;
    @Autowired
    private TimeHelper timeHelper;
    @Autowired
    private ResourceDestinationMaker resourceDestinationMaker;
    @Autowired
    private TpmEncryptedResourceKeyBackend resourceKeyBackend;

    @Override
    public boolean start() {
        setupCanonicalEvents();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @SuppressWarnings("rawtypes")
    private void setupCanonicalEvents() {
        eventFacade.on(VmCanonicalEvents.VM_LIBVIRT_REPORT_START, new EventCallback<Object>() {
            @Override
            protected void run(Map tokens, Object data) {
                String vmUuid = (String) data;
                boolean managedByMe = resourceDestinationMaker.isManagedByUs(vmUuid);
                if (!managedByMe) {
                    return;
                }
                markVmHostFilesChanged(vmUuid);
            }
        });

        eventFacade.on(VmCanonicalEvents.VM_LIBVIRT_REPORT_SHUTDOWN, new EventCallback<Object>() {
            @Override
            protected void run(Map tokens, Object data) {
                String vmUuid = (String) data;
                boolean managedByMe = resourceDestinationMaker.isManagedByUs(vmUuid);
                if (!managedByMe) {
                    return;
                }

                Tuple tuple = Q.New(VmInstanceVO.class)
                        .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                        .eq(VmInstanceVO_.uuid, vmUuid)
                        .findTuple();
                if (tuple == null) {
                    return;
                }

                String hostUuid = (String) tuple.get(0);
                if (hostUuid == null) {
                    hostUuid = (String) tuple.get(1);
                }
                markVmHostFilesChanged(vmUuid, hostUuid);

                List<VmHostFileVO> hostFiles = Q.New(VmHostFileVO.class)
                        .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                        .eq(VmHostFileVO_.hostUuid, hostUuid)
                        .in(VmHostFileVO_.type, list(VmHostFileType.NvRam, VmHostFileType.TpmState))
                        .list();
                if (hostFiles.isEmpty()) {
                    return;
                }

                VmHostFileVO nvRamFile = findOneOrNull(hostFiles, it -> it.getType() == VmHostFileType.NvRam);
                VmHostFileVO tpmStateFile = findOneOrNull(hostFiles, it -> it.getType() == VmHostFileType.TpmState);
                if (nvRamFile == null && tpmStateFile == null) {
                    return;
                }

                SyncVmHostFilesFromHostMsg innerMessage = new SyncVmHostFilesFromHostMsg();
                innerMessage.setHostUuid(hostUuid);
                innerMessage.setVmUuid(vmUuid);
                innerMessage.setNvRamPath(nvRamFile == null ? null : nvRamFile.getPath());
                innerMessage.setTpmStateFolder(tpmStateFile == null ? null : tpmStateFile.getPath());
                innerMessage.setSyncReason(VmShutdown.reason());
                bus.makeLocalServiceId(innerMessage, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                bus.send(innerMessage, new CloudBusCallBack(null) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            logger.info(String.format("success to read file content from host[uuid=%s]",
                                    innerMessage.getHostUuid()));
                        } else {
                            logger.warn(String.format("failed to read file content from host[uuid=%s]: %s",
                                    innerMessage.getHostUuid(), reply.getError().getReadableDetails()));
                        }
                    }
                });
            }
        });
    }

    /**
     * Preemptive judgment: when a VM with TPM (or enabled secure boot) starts or shuts down,
     * the NvRam/TpmState data must have changed, so mark the corresponding
     * VmHostFileVO.changeDate to current time.
     */
    private void markVmHostFilesChanged(String vmUuid) {
        Tuple tuple = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .findTuple();
        if (tuple == null) {
            return;
        }

        String hostUuid = (String) tuple.get(0);
        if (hostUuid == null) {
            hostUuid = (String) tuple.get(1);
        }
        if (hostUuid == null) {
            return;
        }

        markVmHostFilesChanged(vmUuid, hostUuid);
    }

    private void markVmHostFilesChanged(String vmUuid, String hostUuid) {
        if (hostUuid == null) {
            return;
        }

        final Set<VmHostFileType> types = vmHostFileFactory.vmHostFileTypeNeedRegisterForVm(vmUuid);
        if (types.isEmpty()) {
            return;
        }

        Timestamp now = new Timestamp(timeHelper.getCurrentTimeMillis());
        long updated = SQL.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .eq(VmHostFileVO_.hostUuid, hostUuid)
                .in(VmHostFileVO_.type, types)
                .set(VmHostFileVO_.changeDate, now)
                .update();

        if (updated > 0) {
            logger.debug(String.format("preemptively marked VmHostFiles as changed for VM[uuid:%s] on host[uuid:%s], %d records updated",
                    vmUuid, hostUuid, updated));
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof SyncVmHostFilesFromHostMsg) {
            handle((SyncVmHostFilesFromHostMsg) msg);
        } else if (msg instanceof CloneVmHostFileMsg) {
            handle((CloneVmHostFileMsg) msg);
        } else if (msg instanceof BackupVmHostFileMsg) {
            handle((BackupVmHostFileMsg) msg);
        } else if (msg instanceof RestoreVmHostFileMsg) {
            handle((RestoreVmHostFileMsg) msg);
        } else if (msg instanceof BackupVmHostFileOnHypervisorMsg) {
            handle((BackupVmHostFileOnHypervisorMsg) msg);
        } else if (msg instanceof VmHostFileDeletionMsg) {
            handle((VmHostFileDeletionMsg) msg);
        } else if (msg instanceof VmHostBackupFileDeletionMsg) {
            handle((VmHostBackupFileDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    static class CloneVmHostFileContext {
        List<VmHostFileType> typesNeedClone = new ArrayList<>();
        List<VmHostFileVO> files = new ArrayList<>();
        List<VmHostBackupFileVO> backupFiles = new ArrayList<>();
        List<SyncVmHostFilesFromHostMsg> syncContexts = new ArrayList<>();
    }

    private void handle(SyncVmHostFilesFromHostMsg msg) {
        KvmCommandSender sender = new KvmCommandSender(msg.getHostUuid())
                .disableHostStatusCheck();

        KVMAgentCommands.ReadVmHostFileContentCmd cmd = new KVMAgentCommands.ReadVmHostFileContentCmd();
        cmd.setHostFiles(new ArrayList<>());
        if (msg.getTpmStateFolder() != null) {
            KVMAgentCommands.VmHostFileTO to = new KVMAgentCommands.VmHostFileTO();
            to.setPath(msg.getTpmStateFolder());
            to.setType(VmHostFileType.TpmState.toString());
            cmd.getHostFiles().add(to);
        }
        if (msg.getNvRamPath() != null) {
            KVMAgentCommands.VmHostFileTO to = new KVMAgentCommands.VmHostFileTO();
            to.setPath(msg.getNvRamPath());
            to.setType(VmHostFileType.NvRam.toString());
            cmd.getHostFiles().add(to);
        }
        long now = timeHelper.getCurrentTimeMillis();

        SyncVmHostFilesFromHostReply reply = new SyncVmHostFilesFromHostReply();
        sender.send(cmd, READ_VM_HOST_FILE_PATH, wrapper -> {
            KVMAgentCommands.ReadVmHostFileContentResponse readRsp = wrapper.getResponse(KVMAgentCommands.ReadVmHostFileContentResponse.class);
            return readRsp.isSuccess() ? null :
                    operr("failed to read file content response").withException(readRsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(msg) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                KVMAgentCommands.ReadVmHostFileContentResponse readRsp = wrapper.getResponse(KVMAgentCommands.ReadVmHostFileContentResponse.class);
                if (!readRsp.isSuccess()) {
                    reply.setError(operr("failed to read file content response").withException(readRsp.getError()));
                    bus.reply(msg, reply);
                    return;
                }

                ErrorCode error;
                if (msg.isSyncToBackup()) {
                    error = syncToBackupFiles(msg, readRsp);
                } else {
                    error = syncToHostFiles(msg, cmd, readRsp, now);
                }

                if (error != null) {
                    reply.setError(error);
                }
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private ErrorCode syncToHostFiles(SyncVmHostFilesFromHostMsg msg,
                                      KVMAgentCommands.ReadVmHostFileContentCmd cmd,
                                      KVMAgentCommands.ReadVmHostFileContentResponse readRsp,
                                      long timeBeforeSync) {
        final List<VmHostFileVO> existsFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, msg.getVmUuid())
                .eq(VmHostFileVO_.hostUuid, msg.getHostUuid())
                .in(VmHostFileVO_.path, cmd.getPaths())
                .list();
        final List<String> existsContentUuid;
        if (!existsFiles.isEmpty()) {
            existsContentUuid = Q.New(VmHostFileContentVO.class)
                    .in(VmHostFileContentVO_.uuid, transform(existsFiles, VmHostFileVO::getUuid))
                    .select(VmHostFileContentVO_.uuid)
                    .listValues();
        } else {
            existsContentUuid = Collections.emptyList();
        }

        Timestamp syncTime = new Timestamp(timeBeforeSync);
        List<ErrorCode> errors = new ArrayList<>();
        for (String path : cmd.getPaths()) {
            KVMAgentCommands.VmHostFileTO to = findOneOrNull(readRsp.getHostFiles(), item -> item.getPath().equals(path));
            if (to == null) {
                continue;
            }
            if (to.getError() != null) {
                errors.add(operr("failed to read file %s", path)
                        .withOpaque("path", path)
                        .withException(to.getError()));
                continue;
            }

            VmHostFileType type = Objects.equals(path, msg.getNvRamPath()) ?
                    VmHostFileType.NvRam : VmHostFileType.TpmState;

            VmHostFileVO file = findOneOrNull(existsFiles, item -> item.getPath().equals(path));
            boolean fileExists = file != null;

            if (fileExists) {
                String fileUuid = file.getUuid();
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        sql(VmHostFileVO.class)
                                .eq(VmHostFileVO_.uuid, fileUuid)
                                .set(VmHostFileVO_.lastSyncReason, msg.getSyncReason())
                                .set(VmHostFileVO_.lastSyncDate, syncTime)
                                .set(VmHostFileVO_.lastOpDate, syncTime)
                                .update();
                        sql(VmHostFileVO.class)
                                .eq(VmHostFileVO_.uuid, fileUuid)
                                .lt(VmHostFileVO_.changeDate, syncTime)
                                .set(VmHostFileVO_.changeDate, null) // CAS update
                                .update();
                    }
                }.execute();

            } else {
                file = new VmHostFileVO();
                file.setUuid(Platform.getUuid());
                file.setHostUuid(msg.getHostUuid());
                file.setVmInstanceUuid(msg.getVmUuid());
                file.setPath(path);
                file.setType(type);
                file.setLastSyncReason(msg.getSyncReason());
                file.setLastSyncDate(syncTime);
                file.setChangeDate(null);
                file.setLastOpDate(syncTime);
                file.setCreateDate(syncTime);
                file.setResourceName(String.format("%s file for %s", type, msg.getVmUuid()));
                databaseFacade.persist(file);
            }

            byte[] bytes = Base64.getDecoder().decode(to.getContentBase64());
            if (existsContentUuid.contains(file.getUuid())) {
                SQL.New(VmHostFileContentVO.class)
                        .eq(VmHostFileContentVO_.uuid, file.getUuid())
                        .set(VmHostFileContentVO_.content, bytes)
                        .set(VmHostFileContentVO_.format, VmHostFileContentFormat.valueOf(to.getFileFormat()))
                        .set(VmHostFileContentVO_.lastOpDate, syncTime)
                        .update();
            } else {
                VmHostFileContentVO content = new VmHostFileContentVO();
                content.setUuid(file.getUuid());
                content.setContent(bytes);
                content.setFormat(VmHostFileContentFormat.valueOf(to.getFileFormat()));
                content.setCreateDate(syncTime);
                content.setLastOpDate(syncTime);
                databaseFacade.persist(content);
            }

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("persist/update VmHostFileContentVO [uuid=%s]", file.getUuid()));
            }
        }

        if (errors.isEmpty()) {
            return null;
        }

        return operr("failed to read file content from host[uuid=%s]", msg.getHostUuid())
                .withCause(errors);
    }

    private ErrorCode syncToBackupFiles(SyncVmHostFilesFromHostMsg msg,
                                        KVMAgentCommands.ReadVmHostFileContentResponse readRsp) {
        if (msg.getBackupResourceUuid() == null || msg.getBackupResourceUuid().isEmpty()) {
            return operr("backupResourceUuid is required when syncToBackup is true");
        }

        // Query the source VmHostFileVO records for afterBackup callback
        final List<VmHostFileVO> sourceHostFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, msg.getVmUuid())
                .eq(VmHostFileVO_.hostUuid, msg.getHostUuid())
                .list();

        List<VmHostBackupFileVO> backupFilesToPersist = new ArrayList<>();
        List<VmHostFileContentVO> contentsToPersist = new ArrayList<>();
        Map<VmHostBackupFileVO, VmHostFileVO> backupFromMap = new HashMap<>();

        List<ErrorCode> errors = new ArrayList<>();
        Timestamp now = Timestamp.from(Instant.now());

        for (KVMAgentCommands.VmHostFileTO to : readRsp.getHostFiles()) {
            if (to == null) {
                continue;
            }
            if (to.getError() != null) {
                errors.add(operr("failed to read backup file %s", to.getPath())
                        .withOpaque("path", to.getPath())
                        .withException(to.getError()));
                continue;
            }
            if (to.getContentBase64() == null) {
                errors.add(operr("backup file %s returns empty content", to.getPath())
                        .withOpaque("path", to.getPath()));
                continue;
            }

            VmHostFileType type = VmHostFileType.valueOf(to.getType());

            VmHostBackupFileVO backupFile = new VmHostBackupFileVO();
            backupFile.setUuid(Platform.getUuid());
            backupFile.setResourceUuid(msg.getBackupResourceUuid());
            backupFile.setType(type);
            backupFile.setCreateDate(now);
            backupFile.setLastOpDate(now);
            backupFilesToPersist.add(backupFile);

            VmHostFileContentVO content = new VmHostFileContentVO();
            content.setUuid(backupFile.getUuid());
            content.setContent(Base64.getDecoder().decode(to.getContentBase64()));
            content.setFormat(VmHostFileContentFormat.valueOf(to.getFileFormat()));
            content.setCreateDate(now);
            content.setLastOpDate(now);
            contentsToPersist.add(content);

            VmHostFileVO sourceFile = findOneOrNull(sourceHostFiles, item -> item.getType() == type);
            if (sourceFile != null) {
                backupFromMap.put(backupFile, sourceFile);
            }
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                Set<String> backupUuidSet = new HashSet<>();
                for (VmHostBackupFileVO bf : backupFilesToPersist) {
                    backupUuidSet.addAll(q(VmHostBackupFileVO.class)
                            .eq(VmHostBackupFileVO_.resourceUuid, bf.getResourceUuid())
                            .eq(VmHostBackupFileVO_.type, bf.getType())
                            .select(VmHostBackupFileVO_.uuid)
                            .listValues());
                }

                if (!backupUuidSet.isEmpty()) {
                    // TODO: need refactor -> use VmHostBackupFileDeletionMsg
                    safeForEach(backupUuidSet, resourceKeyBackend::cleanEncryptedResourceKey);
                    sql(VmHostBackupFileVO.class)
                            .in(VmHostBackupFileVO_.uuid, backupUuidSet)
                            .delete();
                }

                if (!backupFilesToPersist.isEmpty()) {
                    databaseFacade.persistCollection(backupFilesToPersist);
                }
                if (!contentsToPersist.isEmpty()) {
                    databaseFacade.persistCollection(contentsToPersist);
                }
            }
        }.execute();

        for (VmHostBackupFileVO backup : backupFilesToPersist) {
            VmHostFileVO source = backupFromMap.get(backup);
            if (source != null) {
                try {
                    vmHostFileFactory.createBackupBase(backup).afterBackup(source);
                } catch (Exception e) {
                    logger.warn(String.format("failed to execute afterBackup hook for VmHostBackupFileVO[uuid:%s, type:%s]: %s",
                            backup.getUuid(), backup.getType(), e.getMessage()), e);
                }
            }
        }

        if (errors.isEmpty()) {
            return null;
        }

        return operr("failed to read backup file content from host[uuid=%s]", msg.getHostUuid())
                .withCause(errors);
    }

    @SuppressWarnings("rawtypes")
    private void handle(CloneVmHostFileMsg msg) {
        CloneVmHostFileReply reply = new CloneVmHostFileReply();

        final Set<VmHostFileType> types = vmHostFileFactory.vmHostFileTypeNeedRegisterForVm(msg.getSrcVmUuid());
        if (types.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        CloneVmHostFileContext context = new CloneVmHostFileContext();
        if (types.contains(VmHostFileType.NvRam)) {
            context.typesNeedClone.add(VmHostFileType.NvRam);
        }

        if (types.contains(VmHostFileType.TpmState)) {
            boolean resetTpm;
            if (msg.getResetTpm() == null) {
                ResourceConfig resourceConfig = resourceConfigFacade.getResourceConfig(RESET_TPM_AFTER_VM_CLONE.getIdentity());
                resetTpm = resourceConfig.getResourceConfigValue(msg.getSrcVmUuid(), Boolean.class);
            } else {
                resetTpm = msg.getResetTpm();
            }
            if (!resetTpm) {
                context.typesNeedClone.add(VmHostFileType.TpmState);
            }
        }
        logger.debug(String.format("clone VM[uuid=%s] host files for types: %s", msg.getSrcVmUuid(), context.typesNeedClone));

        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setName("clone-vm-host-file");
        chain.then(new NoRollbackFlow() {
            String __name__ = "prepare-sync-vm-host-file-context-list";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                for (VmHostFileType type : context.typesNeedClone) {
                    VmHostFileVO file = Q.New(VmHostFileVO.class)
                            .eq(VmHostFileVO_.vmInstanceUuid, msg.getSrcVmUuid())
                            .eq(VmHostFileVO_.type, type)
                            .orderByDesc(VmHostFileVO_.lastSyncDate)
                            .limit(1)
                            .find();
                    if (file == null) {
                        logger.debug(String.format("skip to read/write %s host file for VM[vmUuid=%s]: file is not registered in MN",
                                type, msg.getSrcVmUuid()));
                        continue;
                    }
                    context.files.add(file);
                }

                if (context.files.isEmpty()) {
                    trigger.next();
                    return;
                }

                VmInstanceState vmState = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, msg.getSrcVmUuid())
                        .select(VmInstanceVO_.state)
                        .findValue();
                if (vmState == VmInstanceState.Stopped) {
                    boolean anyChange = context.files.stream()
                            .anyMatch(it -> it.getChangeDate() != null);
                    if (!anyChange) {
                        trigger.next();
                        return;
                    }
                }

                Map<String, SyncVmHostFilesFromHostMsg> contextMap = new HashMap<>();
                for (VmHostFileVO file : context.files) {
                    contextMap.computeIfAbsent(file.getHostUuid(), hostUuid -> {
                        SyncVmHostFilesFromHostMsg syncContext = new SyncVmHostFilesFromHostMsg();
                        syncContext.setHostUuid(hostUuid);
                        syncContext.setVmUuid(msg.getSrcVmUuid());
                        syncContext.setSyncReason(PostClone.reason());
                        return syncContext;
                    });
                }
                context.syncContexts.addAll(contextMap.values());

                for (VmHostFileVO file : context.files) {
                    SyncVmHostFilesFromHostMsg syncContext = contextMap.get(file.getHostUuid());
                    if (file.getType() == VmHostFileType.NvRam) {
                        syncContext.setNvRamPath(file.getPath());
                    } else if (file.getType() == VmHostFileType.TpmState) {
                        syncContext.setTpmStateFolder(file.getPath());
                    } else {
                        throw new CloudRuntimeException("unsupported vm host file type: " + file.getType());
                    }
                }

                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "read-vm-host-file-from-origin-host";

            @Override
            public boolean skip(Map data) {
                return context.syncContexts.isEmpty();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(context.syncContexts).each((syncContext, whileContext) -> {
                    bus.makeLocalServiceId(syncContext, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                    bus.send(syncContext, new CloudBusCallBack(whileContext) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                whileContext.addError(reply.getError());
                            }
                            whileContext.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList == null || errorCodeList.isEmpty()) {
                            trigger.next();
                            return;
                        }
                        String details = String.join("\n", transform(errorCodeList.getCauses(), ErrorCode::getReadableDetails));
                        if (msg.isIgnoreSyncError()) {
                            logger.warn(String.format(
                                    "failed to sync host file for VM[uuid=%s] but ignoreSyncError=true, continue:\n%s",
                                    msg.getSrcVmUuid(), details));
                            trigger.next();
                        } else {
                            trigger.fail(operr("failed to sync host file for VM[uuid=%s]", msg.getSrcVmUuid())
                                    .withCause(errorCodeList));
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "determine-content-uuid";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<VmHostFileType> missingTypes = new ArrayList<>(context.typesNeedClone);
                missingTypes.removeAll(transform(context.files, VmHostFileVO::getType));
                if (missingTypes.isEmpty()) {
                    trigger.next();
                    return;
                }

                context.backupFiles.addAll(Q.New(VmHostBackupFileVO.class)
                        .eq(VmHostBackupFileVO_.resourceUuid, msg.getSrcVmUuid())
                        .in(VmHostBackupFileVO_.type, missingTypes)
                        .list());
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "copy-host-content-database";

            @Override
            public boolean skip(Map data) {
                return context.files.isEmpty() && context.backupFiles.isEmpty();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> uuidList = transform(context.files, VmHostFileVO::getUuid);
                List<VmHostFileVO> filesAfterSyncing = Q.New(VmHostFileVO.class)
                        .in(VmHostFileVO_.uuid, uuidList)
                        .list();
                backupVmHostFile(filesAfterSyncing, context.backupFiles, msg.getDstVmUuidList());
                trigger.next();
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void handle(BackupVmHostFileMsg msg) {
        BackupVmHostFileReply reply = new BackupVmHostFileReply();
        List<VmHostBackupFileVO> filesNeedPersists = backupVmHostFile(
                msg.getVmUuid(), msg.getHostUuid(), msg.getToResourceUuidList());
        reply.setBackupFileUuidList(transform(filesNeedPersists, VmHostBackupFileVO::getUuid));
        bus.reply(msg, reply);
    }

    private List<VmHostBackupFileVO> backupVmHostFile(String fromVmUuid, String hostUuid, List<String> toResourceList) {
        List<VmHostFileVO> hostFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, fromVmUuid)
                .eq(VmHostFileVO_.hostUuid, hostUuid)
                .list();

        if (hostFiles.isEmpty()) {
            return new ArrayList<>();
        }
        return backupVmHostFile(hostFiles, new ArrayList<>(), toResourceList);
    }

    private List<VmHostBackupFileVO> backupVmHostFile(List<VmHostFileVO> fileList, List<VmHostBackupFileVO> backupFiles, List<String> toResourceList) {
        List<String> uuidList = transform(fileList, VmHostFileVO::getUuid);
        uuidList.addAll(transform(backupFiles, VmHostBackupFileVO::getUuid));
        List<VmHostFileContentVO> contents = Q.New(VmHostFileContentVO.class)
                .in(VmHostFileContentVO_.uuid, uuidList)
                .list();

        List<VmHostBackupFileVO> filesNeedPersists = new ArrayList<>();
        List<VmHostFileContentVO> contentsNeedPersists = new ArrayList<>();
        // value is VmHostBackupFileVO or VmHostFileVO
        Map<VmHostBackupFileVO, Object> backupFromMap = new HashMap<>();

        Timestamp now = Timestamp.from(Instant.now());
        for (String resourceUuid : toResourceList) {
            for (String uuid : uuidList) {
                VmHostFileContentVO srcContent = findOneOrNull(contents,
                        item -> item.getUuid().equals(uuid));
                if (srcContent == null) {
                    continue;
                }

                VmHostFileVO vmHostFile = findOneOrNull(fileList,
                        item -> item.getUuid().equals(uuid));
                VmHostBackupFileVO vmHostBackupFile = vmHostFile == null ?
                        findOneOrNull(backupFiles, item -> item.getUuid().equals(uuid)) : null;
                DebugUtils.Assert(vmHostFile != null || vmHostBackupFile != null,
                        "vmHostFile or vmHostBackupFile cannot be null");

                VmHostBackupFileVO file = new VmHostBackupFileVO();
                file.setUuid(Platform.getUuid());
                file.setResourceUuid(resourceUuid);
                file.setType(vmHostFile == null ? vmHostBackupFile.getType() : vmHostFile.getType());
                file.setCreateDate(now);
                file.setLastOpDate(now);
                filesNeedPersists.add(file);
                backupFromMap.put(file, vmHostFile == null ? vmHostBackupFile : vmHostFile);

                VmHostFileContentVO content = new VmHostFileContentVO();
                content.setUuid(file.getUuid());
                content.setContent(srcContent.getContent());
                content.setFormat(srcContent.getFormat());
                content.setCreateDate(now);
                content.setLastOpDate(now);
                contentsNeedPersists.add(content);
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("persist VmHostFileContentVO [uuid=%s]",
                    transform(contentsNeedPersists, VmHostFileContentVO::getUuid)));
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                Set<String> backupUuidSet = new HashSet<>();
                for (VmHostBackupFileVO bf : filesNeedPersists) {
                    backupUuidSet.addAll(q(VmHostBackupFileVO.class)
                            .eq(VmHostBackupFileVO_.resourceUuid, bf.getResourceUuid())
                            .eq(VmHostBackupFileVO_.type, bf.getType())
                            .select(VmHostBackupFileVO_.uuid)
                            .listValues());
                }

                if (!backupUuidSet.isEmpty()) {
                    // TODO: need refactor -> use VmHostBackupFileDeletionMsg
                    safeForEach(backupUuidSet, resourceKeyBackend::cleanEncryptedResourceKey);
                    sql(VmHostBackupFileVO.class)
                            .in(VmHostBackupFileVO_.uuid, backupUuidSet)
                            .delete();
                }

                if (!filesNeedPersists.isEmpty()) {
                    databaseFacade.persistCollection(filesNeedPersists);
                }
                if (!contentsNeedPersists.isEmpty()) {
                    databaseFacade.persistCollection(contentsNeedPersists);
                }
            }
        }.execute();

        for (VmHostBackupFileVO backup : filesNeedPersists) {
            final Object backupFrom = backupFromMap.get(backup);
            try {
                if (backupFrom instanceof VmHostBackupFileVO) {
                    vmHostFileFactory.createBackupBase(backup).afterBackup((VmHostBackupFileVO) backupFrom);
                } else if (backupFrom instanceof VmHostFileVO) {
                    vmHostFileFactory.createBackupBase(backup).afterBackup((VmHostFileVO) backupFrom);
                }
            } catch (Exception e) {
                logger.warn(String.format("failed to execute afterBackup hook for VmHostBackupFileVO[uuid:%s, type:%s]: %s",
                        backup.getUuid(), backup.getType(), e.getMessage()), e);
            }
        }

        return filesNeedPersists;
    }

    private void handle(RestoreVmHostFileMsg msg) {
        RestoreVmHostFileReply reply = new RestoreVmHostFileReply();

        List<VmHostBackupFileVO> backupFiles = Q.New(VmHostBackupFileVO.class)
                .eq(VmHostBackupFileVO_.resourceUuid, msg.getSnapshotGroupUuid())
                .list();

        Tuple tuple = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .findTuple();
        if (tuple == null) {
            reply.setError(operr("VM instance [uuid:%s] not found", msg.getVmInstanceUuid()));
            bus.reply(msg, reply);
            return;
        }

        String hostUuid = tuple.get(0, String.class);
        if (hostUuid == null) {
            hostUuid = tuple.get(1, String.class);
        }
        if (hostUuid == null) {
            reply.setError(operr("VM instance [uuid:%s] has no host", msg.getVmInstanceUuid()));
            bus.reply(msg, reply);
            return;
        }

        List<VmHostFileVO> currentHostFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .eq(VmHostFileVO_.hostUuid, hostUuid)
                .list();

        Map<VmHostFileType, VmHostFileVO> currentFilesByType = new HashMap<>();
        for (VmHostFileVO file : currentHostFiles) {
            currentFilesByType.put(file.getType(), file);
        }

        Map<VmHostFileType, VmHostBackupFileVO> backupFilesByType = new HashMap<>();
        for (VmHostBackupFileVO file : backupFiles) {
            backupFilesByType.put(file.getType(), file);
        }

        Set<VmHostFileType> allTypes = new HashSet<>();
        allTypes.addAll(currentFilesByType.keySet());
        allTypes.addAll(backupFilesByType.keySet());

        if (allTypes.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        // Batch query all backup file content before loop
        List<String> backupUuids = transform(backupFiles, VmHostBackupFileVO::getUuid);
        Map<String, VmHostFileContentVO> backupContentMap = new HashMap<>();
        if (!backupUuids.isEmpty()) {
            List<VmHostFileContentVO> backupContents = Q.New(VmHostFileContentVO.class)
                    .in(VmHostFileContentVO_.uuid, backupUuids)
                    .list();
            backupContentMap.putAll(toMap(backupContents, VmHostFileContentVO::getUuid, Function.identity()));
        }

        List<VmHostFileTO> fileList = new ArrayList<>();
        for (VmHostFileType type : allTypes) {
            VmHostFileTO to = new VmHostFileTO();
            to.setType(type.toString());

            boolean hasCurrentFile = currentFilesByType.containsKey(type);
            boolean hasBackupFile = backupFilesByType.containsKey(type);

            if (hasBackupFile) {
                // Write operation
                VmHostBackupFileVO backupFile = backupFilesByType.get(type);
                VmHostFileContentVO content = backupContentMap.get(backupFile.getUuid());
                if (content == null) {
                    logger.warn(String.format("backup file content [uuid:%s] not found for type %s",
                            backupFile.getUuid(), type));
                    continue;
                }

                to.setPath(buildPathForVmHostFileType(type, msg.getVmInstanceUuid()));
                to.setFileFormat(content.getFormat().toString());
                to.setOperation(VmHostFileOperation.Write.toString());
                String contentBase64 = Base64.getEncoder().encodeToString(content.getContent());
                to.setContentBase64(contentBase64);

                fileList.add(to);
            } else if (hasCurrentFile) {
                // Delete operation
                VmHostFileVO currentFile = currentFilesByType.get(type);
                to.setPath(currentFile.getPath());
                to.setOperation(VmHostFileOperation.Delete.toString());

                fileList.add(to);
            }
        }

        if (fileList.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        final String finalHostUuid = hostUuid;
        SimpleFlowChain.of("restore-vm-host-file")
            .then(Flow.of("send-cmd")
                .handle(trigger -> {
                    KVMAgentCommands.WriteVmHostFileContentCmd cmd = new KVMAgentCommands.WriteVmHostFileContentCmd();
                    cmd.setHostFiles(fileList);

                    KvmCommandSender sender = new KvmCommandSender(finalHostUuid);
                    sender.send(cmd, WRITE_VM_HOST_FILE_PATH, wrapper -> {
                        KVMAgentCommands.WriteVmHostFileContentResponse writeRsp = wrapper.getResponse(KVMAgentCommands.WriteVmHostFileContentResponse.class);
                        return writeRsp.isSuccess() ? null :
                                operr("failed to write/delete host file response").withException(writeRsp.getError());
                    }, new ReturnValueCompletion<KvmResponseWrapper>(trigger) {
                        @Override
                        public void success(KvmResponseWrapper wrapper) {
                            logger.info(String.format("success to restore host files for VM[uuid:%s] from snapshot group[uuid:%s]",
                                    msg.getVmInstanceUuid(), msg.getSnapshotGroupUuid()));
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(operr("failed to restore host files for VM[uuid:%s]", msg.getVmInstanceUuid())
                                    .withCause(errorCode));
                        }
                    });
                })
                .build())
            .then(Flow.of("persist-content-in-db")
                .handle(trigger -> {
                    Timestamp now = Timestamp.from(Instant.now());

                    List<String> allUuids = new ArrayList<>();
                    allUuids.addAll(transform(backupFilesByType.values(), VmHostBackupFileVO::getUuid));
                    allUuids.addAll(transform(currentFilesByType.values(), VmHostFileVO::getUuid));

                    Map<String, VmHostFileContentVO> contentMap = new HashMap<>();
                    if (!allUuids.isEmpty()) {
                        List<VmHostFileContentVO> contents = Q.New(VmHostFileContentVO.class)
                                .in(VmHostFileContentVO_.uuid, allUuids)
                                .list();
                        contentMap.putAll(toMap(contents, VmHostFileContentVO::getUuid, Function.identity()));
                    }

                    for (VmHostFileType type : allTypes) {
                        boolean hasCurrentFile = currentFilesByType.containsKey(type);
                        boolean hasBackupFile = backupFilesByType.containsKey(type);

                        if (hasBackupFile) {
                            VmHostBackupFileVO backupFile = backupFilesByType.get(type);
                            VmHostFileContentVO backupContent = contentMap.get(backupFile.getUuid());
                            if (backupContent == null) {
                                continue;
                            }

                            if (hasCurrentFile) {
                                // update existing VmHostFileVO and VmHostFileContentVO
                                VmHostFileVO currentFile = currentFilesByType.get(type);
                                SQL.New(VmHostFileVO.class)
                                        .eq(VmHostFileVO_.uuid, currentFile.getUuid())
                                        .set(VmHostFileVO_.lastSyncReason, Restore.reason(msg.getSyncReason()))
                                        .set(VmHostFileVO_.lastOpDate, now)
                                        .set(VmHostFileVO_.lastSyncDate, now)
                                        .update();

                                VmHostFileContentVO existingContent = contentMap.get(currentFile.getUuid());
                                if (existingContent != null) {
                                    SQL.New(VmHostFileContentVO.class)
                                            .eq(VmHostFileContentVO_.uuid, currentFile.getUuid())
                                            .set(VmHostFileContentVO_.content, backupContent.getContent())
                                            .set(VmHostFileContentVO_.format, backupContent.getFormat())
                                            .set(VmHostFileContentVO_.lastOpDate, now)
                                            .update();
                                } else {
                                    VmHostFileContentVO newContent = new VmHostFileContentVO();
                                    newContent.setUuid(currentFile.getUuid());
                                    newContent.setContent(backupContent.getContent());
                                    newContent.setFormat(backupContent.getFormat());
                                    newContent.setCreateDate(now);
                                    newContent.setLastOpDate(now);
                                    databaseFacade.persist(newContent);
                                }
                            } else {
                                // create new VmHostFileVO and VmHostFileContentVO
                                VmHostFileVO newFile = new VmHostFileVO();
                                newFile.setUuid(Platform.getUuid());
                                newFile.setResourceName(String.format("%s file for %s", type, msg.getVmInstanceUuid()));
                                newFile.setVmInstanceUuid(msg.getVmInstanceUuid());
                                newFile.setHostUuid(finalHostUuid);
                                newFile.setType(type);
                                newFile.setPath(buildPathForVmHostFileType(type, msg.getVmInstanceUuid()));
                                newFile.setLastSyncReason(Restore.reason(msg.getSyncReason()));
                                newFile.setLastSyncDate(now);
                                newFile.setCreateDate(now);
                                newFile.setLastOpDate(now);
                                databaseFacade.persist(newFile);

                                VmHostFileContentVO newContent = new VmHostFileContentVO();
                                newContent.setUuid(newFile.getUuid());
                                newContent.setContent(backupContent.getContent());
                                newContent.setFormat(backupContent.getFormat());
                                newContent.setCreateDate(now);
                                newContent.setLastOpDate(now);
                                databaseFacade.persist(newContent);
                            }
                        } else if (hasCurrentFile) {
                            // delete VmHostFileVO and VmHostFileContentVO
                            VmHostFileVO currentFile = currentFilesByType.get(type);
                            SQL.New(VmHostFileContentVO.class)
                                    .eq(VmHostFileContentVO_.uuid, currentFile.getUuid())
                                    .delete();
                            SQL.New(VmHostFileVO.class)
                                    .eq(VmHostFileVO_.uuid, currentFile.getUuid())
                                    .delete();
                        }
                    }

                    trigger.next();
                })
                .build())
            .propagateExceptionTo(msg)
            .done(() -> bus.reply(msg, reply))
            .error(errorCode -> {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            })
            .start();
    }

    private void handle(BackupVmHostFileOnHypervisorMsg msg) {
        KvmCommandSender sender = new KvmCommandSender(msg.getHostUuid());

        KVMAgentCommands.BackupVmHostFileCmd cmd = new KVMAgentCommands.BackupVmHostFileCmd();
        cmd.setVmHostFileBackupJobs(msg.getVmHostFileBackupJobs());

        BackupVmHostFileOnHypervisorReply reply = new BackupVmHostFileOnHypervisorReply();
        sender.send(cmd, BACKUP_VM_HOST_FILE_PATH, wrapper -> {
            KVMAgentCommands.BackupVmHostFileResponse rsp = wrapper.getResponse(KVMAgentCommands.BackupVmHostFileResponse.class);
            return rsp.isSuccess() ? null :
                    operr("failed to backup vm host file on hypervisor[hostUuid=%s]", msg.getHostUuid())
                            .withOpaque("host.uuid", msg.getHostUuid())
                            .withException(rsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(msg) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public void cleanVmHostBackupFile(String resourceUuid) {
        List<VmHostBackupFileVO> backups = Q.New(VmHostBackupFileVO.class)
                .eq(VmHostBackupFileVO_.resourceUuid, resourceUuid)
                .list();

        for (VmHostBackupFileVO backup : backups) {
            try {
                vmHostFileFactory.createBackupBase(backup).clean();
            } catch (Exception e) {
                logger.warn(String.format("failed to delete VmHostBackupFileVO[uuid:%s, type:%s]: %s",
                        backup.getUuid(), backup.getType(), e.getMessage()), e);
            }
        }
    }

    private void handle(VmHostFileDeletionMsg msg) {
        VmHostFileDeletionReply reply = new VmHostFileDeletionReply();
        databaseFacade.removeByPrimaryKey(msg.getUuid(), VmHostFileVO.class);
        // VmHostFileContentVO will be deleted cascade
        bus.reply(msg, reply);
    }

    private void handle(VmHostBackupFileDeletionMsg msg) {
        VmHostBackupFileDeletionReply reply = new VmHostBackupFileDeletionReply();
        VmHostBackupFileVO backup = databaseFacade.findByUuid(msg.getUuid(), VmHostBackupFileVO.class);
        if (backup != null) {
            try {
                vmHostFileFactory.createBackupBase(backup).clean();
            } catch (Exception e) {
                if (msg.isForceDelete()) {
                    logger.warn(String.format("force delete VmHostBackupFile[uuid:%s] ignored cleanup error: %s",
                            backup.getUuid(), e.getMessage()), e);
                } else {
                    reply.setError(operr("failed to delete VmHostBackupFile[uuid:%s]", backup.getUuid())
                            .withException(e));
                }
            }
        }
        bus.reply(msg, reply);
    }
}
