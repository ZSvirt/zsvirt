package org.zstack.kvm.tpm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager;
import org.zstack.header.secret.SecretHostDeleteMsg;
import org.zstack.header.tpm.api.APIAddTpmEvent;
import org.zstack.header.tpm.api.APIAddTpmMsg;
import org.zstack.header.tpm.api.APIGetTpmCapabilityMsg;
import org.zstack.header.tpm.api.APIGetTpmCapabilityReply;
import org.zstack.header.tpm.api.APIRemoveTpmEvent;
import org.zstack.header.tpm.api.APIRemoveTpmMsg;
import org.zstack.header.tpm.api.APIUpdateTpmMsg;
import org.zstack.header.tpm.entity.TpmCapabilityView;
import org.zstack.header.tpm.entity.TpmInventory;
import org.zstack.header.tpm.entity.TpmKeyBackupVO;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.tpm.message.AddTpmMsg;
import org.zstack.header.tpm.message.AddTpmReply;
import org.zstack.header.tpm.message.DeleteTpmKeyBackupMsg;
import org.zstack.header.tpm.message.DeleteTpmKeyBackupReply;
import org.zstack.header.tpm.message.RestoreTpmEncryptionKeyMsg;
import org.zstack.header.tpm.message.RestoreTpmEncryptionKeyReply;
import org.zstack.header.tpm.message.TpmDeletionMsg;
import org.zstack.header.tpm.message.TpmDeletionReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.additions.ResetVmTpmMsg;
import org.zstack.header.vm.additions.ResetVmTpmReply;
import org.zstack.header.vm.additions.VmHostBackupFileDeletionMsg;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileDeletionMsg;
import org.zstack.header.vm.additions.VmHostFileInventory;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.header.vm.additions.VmHostFileOperation;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.KvmResponseWrapper;
import org.zstack.kvm.efi.KvmSecureBootExtensions;
import org.zstack.header.tpm.message.BackupTpmEncryptionKeyMsg;
import org.zstack.header.tpm.message.BackupTpmEncryptionKeyReply;
import org.zstack.kvm.tpm.message.CloneVmTpmMsg;
import org.zstack.kvm.tpm.message.CloneVmTpmReply;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.compute.vm.VmGlobalConfig.RESET_TPM_AFTER_VM_CLONE;
import static org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend.*;
import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.header.errorcode.SysErrors.NOT_SUPPORTED;
import static org.zstack.header.tpm.TpmConstants.*;
import static org.zstack.header.tpm.TpmErrors.VM_STATE_ERROR;
import static org.zstack.kvm.KVMSystemTags.EDK_RPM_TOKEN;
import static org.zstack.kvm.KVMSystemTags.SWTPM_VERSION;
import static org.zstack.kvm.KVMSystemTags.SWTPM_VERSION_TOKEN;
import static org.zstack.kvm.KVMSystemTags.VM_EDK;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.isEmpty;
import static org.zstack.utils.CollectionUtils.transform;

public class KvmTpmManager extends AbstractService {
    private static final CLogger logger = Utils.getLogger(KvmTpmManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade threadFacade;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;
    @Autowired
    private VmTpmManager vmTpmManager;
    @Autowired
    private TpmEncryptedResourceKeyBackend tpmKeyBackend;
    @Autowired
    private EncryptedResourceKeyManager resourceKeyManager;
    @Autowired
    private KvmSecureBootExtensions secureBootExtensions;
    @Autowired
    private DatabaseFacade databaseFacade;
    @Autowired
    private TimeHelper timeHelper;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    private String tpmQueueSyncSignature(String vmUuid) {
        return String.format("tpm-queue-sync-%s", vmUuid);
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AddTpmMsg) {
            handle((AddTpmMsg) msg);
        } else if (msg instanceof TpmDeletionMsg) {
            handle((TpmDeletionMsg) msg);
        } else if (msg instanceof CloneVmTpmMsg) {
            handle((CloneVmTpmMsg) msg);
        } else if (msg instanceof BackupTpmEncryptionKeyMsg) {
            handle((BackupTpmEncryptionKeyMsg) msg);
        } else if (msg instanceof RestoreTpmEncryptionKeyMsg) {
            handle((RestoreTpmEncryptionKeyMsg) msg);
        } else if (msg instanceof DeleteTpmKeyBackupMsg) {
            handle((DeleteTpmKeyBackupMsg) msg);
        } else if (msg instanceof ResetVmTpmMsg) {
            handle((ResetVmTpmMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetTpmCapabilityMsg) {
            handle((APIGetTpmCapabilityMsg) msg);
        } else if (msg instanceof APIAddTpmMsg) {
            handle((APIAddTpmMsg) msg);
        } else if (msg instanceof APIRemoveTpmMsg) {
            handle((APIRemoveTpmMsg) msg);
        } else if (msg instanceof APIUpdateTpmMsg) {
            handle((APIUpdateTpmMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(AddTpmMsg msg) {
        if (msg.getKeyProviderUuid() != null && msg.getResourceUuidKeyFrom() != null) {
            throw operr("keyProviderUuid and resourceUuidKeyFrom cannot be set at the same time").toException();
        }

        AddTpmReply reply = new AddTpmReply();
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                AddTpmToVmContext context = AddTpmToVmContext.valueOf(msg);
                addTpmToVm(context, new Completion(chain, msg) {
                    @Override
                    public void success() {
                        chain.next();
                        TpmVO vo = Q.New(TpmVO.class)
                                .eq(TpmVO_.uuid, msg.getTpmUuid())
                                .find();
                        reply.setInventory(TpmInventory.valueOf(vo));
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return tpmQueueSyncSignature(msg.getVmInstanceUuid());
            }

            @Override
            public String getName() {
                return "queue-of-add-tpm-to-vm-" + msg.getVmInstanceUuid();
            }
        });
    }

    static class AddTpmToVmContext {
        String keyProviderUuid; // create new key with the provider uuid
        String resourceUuidKeyFrom; // copy key from the resource uuid
        String vmInstanceUuid;
        String tpmUuid;

        boolean tpmCreated;
        boolean keyProviderAttached;
        String createdTpmUuid;

        static AddTpmToVmContext valueOf(AddTpmMsg msg) {
            AddTpmToVmContext context = new AddTpmToVmContext();
            context.keyProviderUuid = msg.getKeyProviderUuid();
            context.resourceUuidKeyFrom = msg.getResourceUuidKeyFrom();
            context.vmInstanceUuid = msg.getVmInstanceUuid();
            context.tpmUuid = msg.getTpmUuid();
            return context;
        }
    }

    private void addTpmToVm(AddTpmToVmContext context, Completion completion) {
        SimpleFlowChain.of("add-tpm-to-vm-" + context.vmInstanceUuid)
            .then(Flow.of("check-vm-status")
                .handle(trigger -> {
                    VmInstanceVO vm = Q.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, context.vmInstanceUuid)
                            .find();

                    if (!SUPPORT_VM_STATES_FOR_TPM_OPERATION.contains(vm.getState())) {
                        trigger.fail(err(VM_STATE_ERROR,
                                "The current VM state does not support adding TPM operations")
                                .withOpaque("support.vm.state", SUPPORT_VM_STATES_FOR_TPM_OPERATION));
                        return;
                    }
                    trigger.next();
                })
                .build())
            .then(Flow.of("create-tpm-db-records")
                .handle(trigger -> {
                    TpmVO tpm = vmTpmManager.persistTpmVO(context.tpmUuid, context.vmInstanceUuid);
                    context.createdTpmUuid = tpm.getUuid();
                    context.tpmCreated = true;
                    trigger.next();
                })
                .rollback(trigger -> {
                    if (context.tpmCreated && context.createdTpmUuid != null) {
                        vmTpmManager.deleteTpmVO(context.createdTpmUuid);
                    }
                    trigger.rollback();
                })
                .build())
            .then(Flow.of("attach-key-provider-to-tpm")
                .skipIf(data -> VmGlobalConfig.ALLOWED_TPM_VM_WITHOUT_KMS.value(Boolean.class) || context.keyProviderUuid == null)
                .handle(trigger -> {
                    tpmKeyBackend.attachKeyProviderToTpm(context.createdTpmUuid, context.keyProviderUuid);
                    context.keyProviderAttached = true;

                    EncryptedResourceKeyManager.GetOrCreateResourceKeyContext keyCtx =
                            new EncryptedResourceKeyManager.GetOrCreateResourceKeyContext();
                    keyCtx.setResourceUuid(context.createdTpmUuid);
                    keyCtx.setResourceType(TpmVO.class.getSimpleName());
                    keyCtx.setKeyProviderUuid(context.keyProviderUuid);
                    keyCtx.setPurpose("vtpm");
                    resourceKeyManager.getOrCreateKey(keyCtx, new ReturnValueCompletion<EncryptedResourceKeyManager.ResourceKeyResult>(trigger) {
                        @Override
                        public void success(EncryptedResourceKeyManager.ResourceKeyResult returnValue) {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                })
                .rollback(trigger -> {
                    if (context.keyProviderAttached && context.createdTpmUuid != null) {
                        tpmKeyBackend.detachKeyProviderFromTpm(context.createdTpmUuid);
                    }
                    trigger.rollback();
                })
                .build())
            .then(Flow.of("copy-key-to-tpm")
                .skipIf(data -> VmGlobalConfig.ALLOWED_TPM_VM_WITHOUT_KMS.value(Boolean.class) || context.resourceUuidKeyFrom == null)
                .handle(trigger -> {
                    RestoreEncryptedResourceKeyContext restoreContext = new RestoreEncryptedResourceKeyContext();
                    restoreContext.srcResourceUuid = context.resourceUuidKeyFrom;
                    restoreContext.dstResourceUuid = context.createdTpmUuid;
                    tpmKeyBackend.restoreEncryptedResourceKey(restoreContext);
                    trigger.next();
                })
                .rollback(trigger -> {
                    if (context.createdTpmUuid != null) {
                        tpmKeyBackend.detachKeyProviderFromTpm(context.createdTpmUuid);
                    }
                    trigger.rollback();
                })
                .build())
            .propagateExceptionTo(completion)
            .done(completion::success)
            .error(completion::fail)
            .start();
    }

    private void handle(TpmDeletionMsg msg) {
        TpmDeletionReply reply = new TpmDeletionReply();
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                RemoveTpmFromVmContext context = RemoveTpmFromVmContext.valueOf(msg);
                removeTpmFromVm(context, new Completion(chain, msg) {
                    @Override
                    public void success() {
                        chain.next();
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return tpmQueueSyncSignature(msg.getVmInstanceUuid());
            }

            @Override
            public String getName() {
                return "queue-of-remove-tpm-from-vm-" + msg.getVmInstanceUuid();
            }
        });
    }

    static class RemoveTpmFromVmContext {
        String vmInstanceUuid;
        String tpmUuid;
        Integer keyVersion;

        // enable when TPM delete/VM delete operation
        boolean force;

        List<VmHostFileVO> hostFiles;

        static RemoveTpmFromVmContext valueOf(TpmDeletionMsg msg) {
            RemoveTpmFromVmContext context = new RemoveTpmFromVmContext();
            context.vmInstanceUuid = msg.getVmInstanceUuid();
            context.tpmUuid = msg.getTpmUuid();
            context.force = msg.isForceDelete();
            return context;
        }
    }

    private void removeTpmFromVm(RemoveTpmFromVmContext context, Completion completion) {
        SimpleFlowChain.of("remove-tpm-from-vm-" + context.vmInstanceUuid)
            .then(Flow.of("check-vm-status")
                .skipIf(data -> context.force)
                .handle(trigger -> {
                    VmInstanceVO vm = Q.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, context.vmInstanceUuid)
                            .find();

                    if (!SUPPORT_VM_STATES_FOR_TPM_OPERATION.contains(vm.getState())) {
                        trigger.fail(err(VM_STATE_ERROR,
                                "The current VM state does not support removing TPM operations")
                                .withOpaque("support.vm.state", SUPPORT_VM_STATES_FOR_TPM_OPERATION));
                        return;
                    }
                    trigger.next();
                })
                .build())
            .then(Flow.of("collect-vm-host-files")
                .handle(trigger -> {
                    context.keyVersion = tpmKeyBackend.findKeyVersionByTpm(context.tpmUuid);
                    // DO NOT delete NvRam type VmHostFile: Maybe secure boot or other component related.
                    context.hostFiles = Q.New(VmHostFileVO.class)
                            .eq(VmHostFileVO_.vmInstanceUuid, context.vmInstanceUuid)
                            .eq(VmHostFileVO_.type, VmHostFileType.TpmState)
                            .list();
                    trigger.next();
                })
                .build())
            .then(Flow.of("send-delete-commands-to-hosts")
                .skipIf(data -> context.hostFiles.isEmpty())
                .handle(trigger -> {
                    Map<String, List<VmHostFileVO>> filesByHost = new HashMap<>();
                    for (VmHostFileVO file : context.hostFiles) {
                        filesByHost.computeIfAbsent(file.getHostUuid(), k -> new ArrayList<>()).add(file);
                    }

                    new While<>(filesByHost.entrySet()).each((entry, whileCompletion) -> {
                        String hostUuid = entry.getKey();
                        List<VmHostFileVO> files = entry.getValue();

                        KVMAgentCommands.WriteVmHostFileContentCmd cmd = new KVMAgentCommands.WriteVmHostFileContentCmd();
                        List<KVMAgentCommands.VmHostFileTO> fileTOs = new ArrayList<>();
                        for (VmHostFileVO file : files) {
                            KVMAgentCommands.VmHostFileTO to = new KVMAgentCommands.VmHostFileTO();
                            to.setPath(file.getPath());
                            to.setType(file.getType().toString());
                            to.setOperation(VmHostFileOperation.Delete.toString());
                            fileTOs.add(to);
                        }
                        cmd.setHostFiles(fileTOs);

                        new KvmCommandSender(hostUuid).send(cmd, KVMConstant.WRITE_VM_HOST_FILE_PATH, wrapper -> {
                            KVMAgentCommands.WriteVmHostFileContentResponse rsp =
                                    wrapper.getResponse(KVMAgentCommands.WriteVmHostFileContentResponse.class);
                            return rsp.isSuccess() ? null : operr("failed to delete host files on host[uuid=%s]", hostUuid);
                        }, new ReturnValueCompletion<KvmResponseWrapper>(whileCompletion) {
                            @Override
                            public void success(KvmResponseWrapper wrapper) {
                                whileCompletion.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(String.format("failed to delete host files on host[uuid=%s], but continuing with DB cleanup: %s",
                                        hostUuid, errorCode.getDetails()));
                                whileCompletion.done();
                            }
                        });
                    }).run(new WhileDoneCompletion(trigger) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            trigger.next();
                        }
                    });
                })
                .build())
            .then(Flow.of("delete-host-secret")
                .skipIf(data -> context.keyVersion == null)
                .handle(trigger -> {
                    Set<String> hostUuids = new HashSet<>();
                    for (VmHostFileVO file : context.hostFiles) {
                        hostUuids.add(file.getHostUuid());
                    }
                    if (hostUuids.isEmpty()) {
                        addVmCurrentAndLastHostUuidsForSecretDelete(hostUuids, context.vmInstanceUuid);
                    }
                    if (hostUuids.isEmpty()) {
                        trigger.next();
                        return;
                    }

                    new While<>(new ArrayList<>(hostUuids)).each((hostUuid, whileCompletion) -> {
                        SecretHostDeleteMsg dmsg = new SecretHostDeleteMsg();
                        dmsg.setHostUuid(hostUuid);
                        dmsg.setVmUuid(context.vmInstanceUuid);
                        dmsg.setPurpose("vtpm");
                        dmsg.setKeyVersion(context.keyVersion);
                        dmsg.setUsageInstance(KVMConstant.HOST_SECRET_USAGE_INSTANCE_VTPM);
                        bus.makeTargetServiceIdByResourceUuid(dmsg, HostConstant.SERVICE_ID, hostUuid);
                        bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    ErrorCode err = reply.getError();
                                    String errMsg = err != null && err.getDetails() != null ? err.getDetails() : "unknown error";
                                    logger.warn(String.format("failed to delete host secret on host[uuid:%s] for vm[uuid:%s], continue cleanup: %s",
                                            hostUuid, context.vmInstanceUuid, errMsg));
                                }
                                whileCompletion.done();
                            }
                        });
                    }).run(new WhileDoneCompletion(trigger) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            trigger.next();
                        }
                    });
                })
                .build())
            .then(Flow.of("detach-resource-key")
                .handle(trigger -> {
                    tpmKeyBackend.detachKeyProviderFromTpm(context.tpmUuid);
                    List<String> backupUuidList = Q.New(VmHostBackupFileVO.class)
                            .eq(VmHostBackupFileVO_.resourceUuid, context.vmInstanceUuid)
                            .eq(VmHostBackupFileVO_.type, VmHostFileType.TpmState)
                            .select(VmHostBackupFileVO_.uuid)
                            .listValues();
                    for (String backupUuid : backupUuidList) {
                        tpmKeyBackend.cleanEncryptedResourceKey(backupUuid);
                    }
                    trigger.next();
                })
                .build())
            .then(Flow.of("remove-db-records")
                .handle(trigger -> {
                    new SQLBatch() {
                        @Override
                        protected void scripts() {
                            sql(TpmVO.class)
                                    .eq(TpmVO_.uuid, context.tpmUuid)
                                    .delete();
                            sql(VmHostFileVO.class)
                                    .eq(VmHostFileVO_.vmInstanceUuid, context.vmInstanceUuid)
                                    .eq(VmHostFileVO_.type, VmHostFileType.TpmState)
                                    .delete();
                            sql(VmHostBackupFileVO.class)
                                    .eq(VmHostBackupFileVO_.resourceUuid, context.vmInstanceUuid)
                                    .eq(VmHostBackupFileVO_.type, VmHostFileType.TpmState)
                                    .delete();
                        }
                    }.execute();
                    trigger.next();
                })
                .build())
            .propagateExceptionTo(completion)
            .done(completion::success)
            .error(completion::fail)
            .start();
    }

    private void handle(CloneVmTpmMsg msg) {
        CloneVmTpmReply reply = new CloneVmTpmReply();

        String originTpmUuid = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, msg.getSrcVmUuid())
                .select(TpmVO_.uuid)
                .findValue();
        if (originTpmUuid == null) {
            bus.reply(msg, reply);
            return;
        }

        SimpleFlowChain.of("clone-VM-TPM")
        .then(Flow.of("persist-TPM-VO")
            .handle(trigger -> {
                reply.setInventories(new ArrayList<>());
                for (String dstVmUuid : msg.getDstVmUuidList()) {
                    TpmVO dstTpm = vmTpmManager.persistTpmVO(null, dstVmUuid);
                    reply.getInventories().add(TpmInventory.valueOf(dstTpm));
                }
                trigger.next();
            })
            .rollback(trigger -> {
                if (CollectionUtils.isEmpty(reply.getInventories())) {
                    trigger.rollback();
                    return;
                }

                new While<>(reply.getInventories()).each((tpm, whileCompletion) -> {
                    RemoveTpmFromVmContext removeContext = new RemoveTpmFromVmContext();
                    removeContext.vmInstanceUuid = tpm.getVmInstanceUuid();
                    removeContext.tpmUuid = tpm.getUuid();
                    removeContext.force = true;
                    removeTpmFromVm(removeContext, new Completion(whileCompletion) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("failed to delete tpm for VM[%s] but still continue: %s",
                                    tpm.getVmInstanceUuid(), errorCode.getReadableDetails()));
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.rollback();
                    }
                });
            })
            .build())
        .then(Flow.of("clone-encrypted-resource-key-if-needed")
            .handle(trigger -> {
                boolean resetTpm;
                if (msg.getResetTpm() == null) {
                    ResourceConfig resourceConfig = resourceConfigFacade.getResourceConfig(RESET_TPM_AFTER_VM_CLONE.getIdentity());
                    resetTpm = resourceConfig.getResourceConfigValue(msg.getSrcVmUuid(), Boolean.class);
                } else {
                    resetTpm = msg.getResetTpm();
                }

                new While<>(reply.getInventories()).each((inventory, whileCompletion) -> {
                    TpmEncryptedResourceKeyBackend.CloneEncryptedResourceKeyContext context =
                            new TpmEncryptedResourceKeyBackend.CloneEncryptedResourceKeyContext();
                    context.srcTpmUuid = originTpmUuid;
                    context.dstTpmUuid = inventory.getUuid();
                    context.resetTpm = resetTpm;
                    tpmKeyBackend.cloneEncryptedResourceKey(context, new Completion(whileCompletion) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            whileCompletion.addError(errorCode);
                            whileCompletion.allDone();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.isEmpty()) {
                            trigger.next();
                            return;
                        }
                        trigger.fail(operr("Failed to clone encrypted resource key")
                                .withOpaque("src.tpm.uuid", originTpmUuid)
                                .withCause(errorCodeList));
                    }
                });
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

    private void handle(BackupTpmEncryptionKeyMsg msg) {
        BackupEncryptedResourceKeyContext content = new BackupEncryptedResourceKeyContext();
        content.srcResourceUuid = msg.getSrcResourceUuid();
        content.dstResourceUuid = msg.getDstResourceUuid();
        tpmKeyBackend.backupEncryptedResourceKey(content);
        bus.reply(msg, new BackupTpmEncryptionKeyReply());
    }

    private void handle(RestoreTpmEncryptionKeyMsg msg) {
        RestoreTpmEncryptionKeyReply reply = new RestoreTpmEncryptionKeyReply();
        String tpmKeyBackupUuid = null;
        try {
            if (msg.isBackupCurrentKey()
                    && msg.getDstResourceUuid() != null
                    && tpmKeyBackend.checkTpmKeyProviderAttached(msg.getDstResourceUuid())) {
                tpmKeyBackupUuid = Platform.getUuid();
                TpmKeyBackupVO backupVo = new TpmKeyBackupVO();
                backupVo.setUuid(tpmKeyBackupUuid);
                Timestamp now = new Timestamp(timeHelper.getCurrentTimeMillis());
                backupVo.setCreateDate(now);
                backupVo.setLastOpDate(now);
                databaseFacade.persist(backupVo);
                BackupEncryptedResourceKeyContext backupCtx = new BackupEncryptedResourceKeyContext();
                backupCtx.srcResourceUuid = msg.getDstResourceUuid();
                backupCtx.dstResourceUuid = tpmKeyBackupUuid;
                tpmKeyBackend.backupEncryptedResourceKey(backupCtx);
                reply.setTpmKeyBackupUuid(tpmKeyBackupUuid);
            }

            RestoreEncryptedResourceKeyContext content = new RestoreEncryptedResourceKeyContext();
            content.srcResourceUuid = msg.getSrcResourceUuid();
            content.dstResourceUuid = msg.getDstResourceUuid();
            tpmKeyBackend.restoreEncryptedResourceKey(content);
            bus.reply(msg, reply);
        } catch (Exception t) {
            if (tpmKeyBackupUuid != null) {
                tpmKeyBackend.cleanTpmKeyBackupEncryptedResourceKey(tpmKeyBackupUuid);
                databaseFacade.removeByPrimaryKey(tpmKeyBackupUuid, TpmKeyBackupVO.class);
            }
            throw t;
        }
    }

    private void handle(DeleteTpmKeyBackupMsg msg) {
        DeleteTpmKeyBackupReply reply = new DeleteTpmKeyBackupReply();
        if (msg.getTpmKeyBackupUuid() != null) {
            tpmKeyBackend.cleanTpmKeyBackupEncryptedResourceKey(msg.getTpmKeyBackupUuid());
            databaseFacade.removeByPrimaryKey(msg.getTpmKeyBackupUuid(), TpmKeyBackupVO.class);
        }
        bus.reply(msg, reply);
    }

    static class ResetVmTpmContext {
        String vmInstanceUuid;
        Integer keyVersion;

        List<VmHostFileVO> hostFiles;
        VmHostFileVO hostFileToDeleteLast;
        List<String> hostFileUuidListDeleteSuccessfully = new ArrayList<>();
        ErrorCodeList errorsOnSendCmd = new ErrorCodeList();

        static ResetVmTpmContext valueOf(ResetVmTpmMsg msg) {
            ResetVmTpmContext context = new ResetVmTpmContext();
            context.vmInstanceUuid = msg.getVmInstanceUuid();
            return context;
        }
    }

    private void handle(ResetVmTpmMsg msg) {
        ResetVmTpmReply reply = new ResetVmTpmReply();
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                ResetVmTpmContext context = ResetVmTpmContext.valueOf(msg);
                resetVmTpm(context, new Completion(chain, msg) {
                    @Override
                    public void success() {
                        chain.next();
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return tpmQueueSyncSignature(msg.getVmInstanceUuid());
            }

            @Override
            public String getName() {
                return "queue-of-reset-tpm-from-vm-" + msg.getVmInstanceUuid();
            }
        });
    }

    private void resetVmTpm(ResetVmTpmContext context, Completion completion) {
        String vmUuid = context.vmInstanceUuid;
        String tpmUuid = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, vmUuid)
                .select(TpmVO_.uuid)
                .findValue();
        if (tpmUuid != null) {
            context.keyVersion = tpmKeyBackend.findKeyVersionByTpm(tpmUuid);
        }

        SimpleFlowChain.of("reset-vm-tpm-" + vmUuid)
            .then(Flow.of("collect-vm-host-files")
                .handle(trigger -> {
                    context.hostFiles = Q.New(VmHostFileVO.class)
                            .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                            .eq(VmHostFileVO_.type, VmHostFileType.TpmState)
                            .orderByAsc(VmHostFileVO_.lastOpDate)
                            .list();
                    if (!context.hostFiles.isEmpty()) {
                        // We should delete it in last turn:
                        context.hostFileToDeleteLast = context.hostFiles.get(context.hostFiles.size() - 1);
                        context.hostFiles.remove(context.hostFiles.size() - 1);
                    }
                    trigger.next();
                })
                .build())
            .then(Flow.of("send-delete-commands-to-hosts-exclude-last-modified")
                .skipIf(data -> context.hostFiles.isEmpty())
                .handle(trigger -> {
                    Map<String, List<VmHostFileVO>> filesByHost = new HashMap<>();
                    for (VmHostFileVO file : context.hostFiles) {
                        filesByHost.computeIfAbsent(file.getHostUuid(), k -> new ArrayList<>()).add(file);
                    }

                    new While<>(filesByHost.entrySet()).each((entry, whileCompletion) -> {
                        List<KVMAgentCommands.VmHostFileTO> fileTOs = new ArrayList<>();
                        for (VmHostFileVO file : entry.getValue()) {
                            KVMAgentCommands.VmHostFileTO to = new KVMAgentCommands.VmHostFileTO();
                            to.setPath(file.getPath());
                            to.setType(file.getType().toString());
                            to.setOperation(VmHostFileOperation.Delete.toString());
                            fileTOs.add(to);
                        }

                        KvmSecureBootExtensions.RewriteVmHostFilesContext ctx =
                                new KvmSecureBootExtensions.RewriteVmHostFilesContext();
                        ctx.hostUuid = entry.getKey();
                        ctx.hostFiles = fileTOs;

                        secureBootExtensions.rewriteVmHostFiles(ctx, new Completion(whileCompletion) {
                            @Override
                            public void success() {
                                context.hostFileUuidListDeleteSuccessfully.addAll(
                                        transform(entry.getValue(), VmHostFileVO::getUuid));
                                whileCompletion.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                context.errorsOnSendCmd.add(errorCode.withOpaque("host.uuid", entry.getKey()));
                                whileCompletion.done();
                            }
                        });
                    }).run(new WhileDoneCompletion(trigger) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            trigger.next();
                        }
                    });
                })
                .build())
            .then(Flow.of("remove-db-records")
                .skipIf(data -> context.hostFileUuidListDeleteSuccessfully.isEmpty())
                .handle(trigger -> {
                    SQL.New(VmHostFileVO.class)
                            .in(VmHostFileVO_.uuid, context.hostFileUuidListDeleteSuccessfully)
                            .delete();
                    trigger.next();
                })
                .build())
            .then(Flow.of("check-if-any-error-in-command-sending")
                .handle(trigger -> {
                    // If any host failed to delete, abort the chain to preserve
                    // the last-modified TPM record as a recovery point.
                    if (context.errorsOnSendCmd.hasError()) {
                        if (context.errorsOnSendCmd.size() == 1) {
                            trigger.fail(context.errorsOnSendCmd.getCauses().get(0));
                        } else {
                            trigger.fail(operr("failed to delete TPM files on multiple hosts")
                                    .withOpaque("vm.uuid", vmUuid)
                                    .withCause(context.errorsOnSendCmd.getCauses()));
                        }
                        return;
                    }
                    trigger.next();
                })
                .build())
            .then(Flow.of("send-delete-commands-to-hosts-for-last-modified")
                .skipIf(data -> context.hostFileToDeleteLast == null)
                .handle(trigger -> {
                    KVMAgentCommands.VmHostFileTO to = new KVMAgentCommands.VmHostFileTO();
                    to.setPath(context.hostFileToDeleteLast.getPath());
                    to.setType(context.hostFileToDeleteLast.getType().toString());
                    to.setOperation(VmHostFileOperation.Delete.toString());

                    KvmSecureBootExtensions.RewriteVmHostFilesContext ctx =
                            new KvmSecureBootExtensions.RewriteVmHostFilesContext();
                    ctx.hostUuid = context.hostFileToDeleteLast.getHostUuid();
                    ctx.hostFiles = list(to);

                    secureBootExtensions.rewriteVmHostFiles(ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode.withOpaque("host.uuid", ctx.hostUuid));
                        }
                    });
                })
                .build())
            .then(Flow.of("remove-host-file-db-records-for-remains")
                .skipIf(data -> context.hostFileToDeleteLast == null)
                .handle(trigger -> {
                    VmHostFileDeletionMsg deletionMsg = new VmHostFileDeletionMsg();
                    deletionMsg.setUuid(context.hostFileToDeleteLast.getUuid());
                    deletionMsg.setForceDelete(false);
                    bus.makeLocalServiceId(deletionMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                    bus.send(deletionMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                trigger.next();
                            } else {
                                trigger.fail(reply.getError());
                            }
                        }
                    });
                })
                .build())
            .then(Flow.of("remove-backups-db-records-for-remains")
                .handle(trigger -> {
                    List<String> backupUuidList = Q.New(VmHostBackupFileVO.class)
                            .eq(VmHostBackupFileVO_.resourceUuid, vmUuid)
                            .eq(VmHostBackupFileVO_.type, VmHostFileType.TpmState)
                            .select(VmHostBackupFileVO_.uuid)
                            .listValues();
                    if (isEmpty(backupUuidList)) {
                        trigger.next();
                        return;
                    }

                    new While<>(backupUuidList).each((uuid, whileCompletion) -> {
                        VmHostBackupFileDeletionMsg deletionMsg = new VmHostBackupFileDeletionMsg();
                        deletionMsg.setUuid(uuid);
                        // VmHostFileVO has been deleted in the previous step, so force delete here is safe
                        deletionMsg.setForceDelete(true);
                        bus.makeLocalServiceId(deletionMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                        bus.send(deletionMsg, new CloudBusCallBack(whileCompletion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    whileCompletion.addError(reply.getError());
                                }
                                whileCompletion.done();
                            }
                        });
                    }).run(new WhileDoneCompletion(trigger) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            if (errorCodeList.hasError()) {
                                String details = String.join("\n", transform(errorCodeList.getCauses(), ErrorCode::getReadableDetails));
                                logger.warn("failed to clean backup files but still continue:\n" + details);
                            }
                            trigger.next();
                        }
                    });
                })
                .build())
            .then(Flow.of("delete-host-secret")
                .handle(trigger -> {
                    if (context.keyVersion == null) {
                        trigger.next();
                        return;
                    }
                    Set<String> hostUuids = new HashSet<>();
                    for (VmHostFileVO file : context.hostFiles) {
                        hostUuids.add(file.getHostUuid());
                    }
                    if (context.hostFileToDeleteLast != null) {
                        hostUuids.add(context.hostFileToDeleteLast.getHostUuid());
                    }
                    if (hostUuids.isEmpty()) {
                        addVmCurrentAndLastHostUuidsForSecretDelete(hostUuids, vmUuid);
                    }
                    if (hostUuids.isEmpty()) {
                        trigger.next();
                        return;
                    }

                    new While<>(new ArrayList<>(hostUuids)).each((hostUuid, whileCompletion) -> {
                        SecretHostDeleteMsg dmsg = new SecretHostDeleteMsg();
                        dmsg.setHostUuid(hostUuid);
                        dmsg.setVmUuid(vmUuid);
                        dmsg.setPurpose("vtpm");
                        dmsg.setKeyVersion(context.keyVersion);
                        dmsg.setUsageInstance(KVMConstant.HOST_SECRET_USAGE_INSTANCE_VTPM);
                        bus.makeTargetServiceIdByResourceUuid(dmsg, HostConstant.SERVICE_ID, hostUuid);
                        bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    ErrorCode err = reply.getError();
                                    String errMsg = err != null && err.getDetails() != null ? err.getDetails() : "unknown error";
                                    logger.warn(String.format("failed to delete host secret on host[uuid:%s] for vm[uuid:%s], continue reset: %s",
                                            hostUuid, vmUuid, errMsg));
                                }
                                whileCompletion.done();
                            }
                        });
                    }).run(new WhileDoneCompletion(trigger) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            trigger.next();
                        }
                    });
                })
                .build())
            .propagateExceptionTo(completion)
            .done(completion::success)
            .error(completion::fail)
            .start();
    }

    private static void addVmCurrentAndLastHostUuidsForSecretDelete(Set<String> hostUuids, String vmInstanceUuid) {
        if (vmInstanceUuid == null) {
            return;
        }
        VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                .find();
        if (vm == null) {
            return;
        }
        if (vm.getHostUuid() != null) {
            hostUuids.add(vm.getHostUuid());
        }
        if (vm.getLastHostUuid() != null) {
            hostUuids.add(vm.getLastHostUuid());
        }
    }

    private void handle(APIGetTpmCapabilityMsg msg) {
        TpmCapabilityView view = new TpmCapabilityView();

        final TpmVO tpm = Q.New(TpmVO.class)
                .eq(TpmVO_.uuid, msg.getTpmUuid())
                .find();
        final VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, tpm.getVmInstanceUuid())
                .find();
        view.setTpmInventory(TpmInventory.valueOf(tpm));

        List<VmHostFileVO> files = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vm.getUuid())
                .in(VmHostFileVO_.type, list(VmHostFileType.TpmState, VmHostFileType.NvRam))
                .list();
        view.setFileRefs(VmHostFileInventory.valueOf(files));

        view.setEdkVersion(VM_EDK.getTokenByResourceUuid(vm.getUuid(), EDK_RPM_TOKEN));

        if (vm.getHostUuid() != null) {
            view.setSwtpmVersion(SWTPM_VERSION.getTokenByResourceUuid(vm.getHostUuid(), SWTPM_VERSION_TOKEN));
        } else if (vm.getLastHostUuid() != null) {
            view.setSwtpmVersion(SWTPM_VERSION.getTokenByResourceUuid(vm.getLastHostUuid(), SWTPM_VERSION_TOKEN));
        }

        ResourceConfig resourceConfig = resourceConfigFacade.getResourceConfig(RESET_TPM_AFTER_VM_CLONE.getIdentity());
        view.setResetTpmAfterVmCloneConfig(resourceConfig.getResourceConfigValue(vm.getUuid(), Boolean.class));

        APIGetTpmCapabilityReply reply = new APIGetTpmCapabilityReply();
        reply.setInventory(view);
        bus.reply(msg, reply);
    }

    private void handle(APIAddTpmMsg msg) {
        APIAddTpmEvent event = new APIAddTpmEvent(msg.getId());

        AddTpmMsg inner = AddTpmMsg.valueOf(msg);
        bus.makeTargetServiceIdByResourceUuid(inner, SERVICE_ID, msg.getResourceUuid());
        bus.send(inner, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    event.setInventory(((AddTpmReply) reply.castReply()).getInventory());
                } else {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIRemoveTpmMsg msg) {
        APIRemoveTpmEvent event = new APIRemoveTpmEvent(msg.getId());

        TpmDeletionMsg inner = TpmDeletionMsg.valueOf(msg);
        bus.makeTargetServiceIdByResourceUuid(inner, SERVICE_ID, msg.getTpmUuid());
        bus.send(inner, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIUpdateTpmMsg msg) {
        throw err(NOT_SUPPORTED, "UpdateTpm is not supported in current version").toException();
    }
}
