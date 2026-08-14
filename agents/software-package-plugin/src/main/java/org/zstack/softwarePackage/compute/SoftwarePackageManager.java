package org.zstack.softwarePackage.compute;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.GetFileDownloadProgressMsg;
import org.zstack.header.host.GetFileDownloadProgressReply;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.UploadFileToHostMsg;
import org.zstack.header.host.UploadFileToHostReply;
import org.zstack.header.host.UploadFileToVmMsg;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.backup.DeleteFilesOnBackupStorageHostMsg;
import org.zstack.header.storage.backup.UnzipFileOnBackupStorageHostMsg;
import org.zstack.header.storage.backup.UnzipFileOnBackupStorageHostReply;
import org.zstack.header.storage.backup.UploadFileToBackupStorageHostMsg;
import org.zstack.header.storage.backup.UploadFileToBackupStorageHostReply;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.softwarePackage.compute.client.ShellCommandUtils;
import org.zstack.softwarePackage.compute.client.SoftwarePackageBackupStorageUtils;
import org.zstack.softwarePackage.entity.SoftwarePackageStatus;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.softwarePackage.entity.SoftwarePackageVO_;
import org.zstack.softwarePackage.entity.UpgradeType;
import org.zstack.softwarePackage.entity.UploadSoftwarePackageLongJobData;
import org.zstack.softwarePackage.entity.UploadSoftwarePackageToBackupStorageLongJobData;
import org.zstack.softwarePackage.header.*;
import org.zstack.softwarePackage.message.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Tuple;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import static org.zstack.core.Platform.err;
import static org.zstack.longjob.LongJobUtils.jobCanceled;
import static org.zstack.softwarePackage.SoftwarePackageConstant.*;
import static org.zstack.softwarePackage.SoftwarePackagePluginErrors.*;
import static org.zstack.softwarePackage.compute.SoftwarePackageSystemTags.*;
import static org.zstack.softwarePackage.entity.SoftwarePackageStatus.*;
import static org.zstack.softwarePackage.header.APIInstallSoftwarePackageMsg.buildAPIInstallSoftwarePackageMsg;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class SoftwarePackageManager extends AbstractService {
    private static final CLogger logger = Utils.getLogger(SoftwarePackageManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    protected DatabaseFacade databases;
    @Autowired
    private ThreadFacade threads;
    @Autowired
    private PluginRegistry plugins;

    @Override
    public boolean start() {
        plugins.saveExtensionAsMap(SoftwarePackageExtensionPoint.class,
                SoftwarePackageExtensionPoint::getSoftwarePackageType);
        plugins.saveExtensionAsMap(UploadSoftwarePackageToBackupStorageExtensionPoint.class,
                UploadSoftwarePackageToBackupStorageExtensionPoint::getSoftwarePackageType);
        plugins.saveExtensionAsMap(EstimatedImageSizeExtensionPoint.class,
                EstimatedImageSizeExtensionPoint::getSoftwarePackageType);
        plugins.saveExtensionAsMap(UploadSoftwarePackageToVmBackend.class,
                UploadSoftwarePackageToVmBackend::getType);
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            if (msg instanceof APIUploadSoftwarePackageMsg) {
                handle((APIUploadSoftwarePackageMsg) msg);
            } else if (msg instanceof APIUploadSoftwarePackageToVmMsg) {
                handle((APIUploadSoftwarePackageToVmMsg) msg);
            } else if (msg instanceof APIUploadSoftwarePackageToBackupStorageMsg) {
                handle((APIUploadSoftwarePackageToBackupStorageMsg) msg);
            } else if (msg instanceof APICleanSoftwarePackageMsg) {
                handle((APICleanSoftwarePackageMsg) msg);
            } else if (msg instanceof APIGetUploadSoftwarePackageJobDetailsMsg) {
                handle((APIGetUploadSoftwarePackageJobDetailsMsg) msg);
            } else if (msg instanceof APIGetDirectoryUsageMsg) {
                handle((APIGetDirectoryUsageMsg) msg);
            } else if (msg instanceof APIInstallSoftwarePackageMsg) {
                handle((APIInstallSoftwarePackageMsg) msg);
            } else if (msg instanceof APIUninstallSoftwarePackageMsg) {
                handle((APIUninstallSoftwarePackageMsg) msg);
            } else if (msg instanceof APIUploadAndExecuteSoftwareUpgradePackageMsg) {
                handle((APIUploadAndExecuteSoftwareUpgradePackageMsg) msg);
            } else if (msg instanceof APICleanUpgradeSoftwarePackageMsg) {
                handle((APICleanUpgradeSoftwarePackageMsg) msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        } else {
            if (msg instanceof UploadSoftwarePackageMsg) {
                handle((UploadSoftwarePackageMsg) msg);
            } else if (msg instanceof CleanSoftwarePackageMsg) {
                handle((CleanSoftwarePackageMsg) msg);
            } else if (msg instanceof CleanUpgradeSoftwarePackageMsg) {
                handle((CleanUpgradeSoftwarePackageMsg) msg);
            } else if (msg instanceof UploadSoftwarePackageToBackupStorageMsg) {
                handle((UploadSoftwarePackageToBackupStorageMsg) msg);
            } else if (msg instanceof UploadAndExecuteSoftwareUpgradePackageMsg) {
                handle((UploadAndExecuteSoftwareUpgradePackageMsg) msg);
            } else if (msg instanceof UploadSoftwarePackageToVmMsg) {
                handle((UploadSoftwarePackageToVmMsg) msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    // ------------------------------------- Message Handers ------------------------------------

    private void handle(APIUploadSoftwarePackageMsg msg) {
        threads.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "upload-software-package";
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUploadSoftwarePackageEvent event = new APIUploadSoftwarePackageEvent(msg.getId());
                uploadSoftwarePackage(UploadSoftwarePackageLongJobData.buildFileLongJobDataFromApiMsg(msg), new ReturnValueCompletion<SoftwarePackageInventory>(chain, msg) {
                    @Override
                    public void success(SoftwarePackageInventory inv) {
                        event.setInventory(inv);
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "upload-software-package";
            }
        });
    }

    private void handle(UploadSoftwarePackageMsg msg) {
        UploadSoftwarePackageReply reply = new UploadSoftwarePackageReply();
        uploadSoftwarePackage(UploadSoftwarePackageLongJobData.buildFileLongJobDataFromMsg(msg), new ReturnValueCompletion<SoftwarePackageInventory>(msg) {
            @Override
            public void success(SoftwarePackageInventory inv) {
                reply.setInventory(inv);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APICleanSoftwarePackageMsg msg) {
        APICleanSoftwarePackageEvent event = new APICleanSoftwarePackageEvent(msg.getId());
        SoftwarePackageVO softwarePackageVO = msg.getSoftwarePackageVO();
        cleanupSoftwarePackage(softwarePackageVO, new Completion(msg) {
            @Override
            public void success() {
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void handle(CleanSoftwarePackageMsg msg) {
        CleanSoftwarePackageReply reply = new CleanSoftwarePackageReply();
        SoftwarePackageVO softwarePackageVO = Q.New(SoftwarePackageVO.class)
                .eq(SoftwarePackageVO_.uuid, msg.getUuid())
                .find();
        if (softwarePackageVO == null) {
            logger.warn(String.format("SoftwarePackageVO[uuid:%s] not found, skip cleanup", msg.getUuid()));
            bus.reply(msg, reply);
            return;
        }
        cleanupSoftwarePackage(softwarePackageVO, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIGetUploadSoftwarePackageJobDetailsMsg msg) {
        APIGetUploadSoftwarePackageJobDetailsReply reply = new APIGetUploadSoftwarePackageJobDetailsReply();

        String tag = SoftwarePackageSystemTags.SOFTWARE_PACKAGE_INFO.instantiateTag(
                Collections.singletonMap(SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ID, msg.getSoftwarePackageId()));
        List<String> longJobUuids = Q.New(SystemTagVO.class, LongJobVO.class)
                .table0()
                    .eq(SystemTagVO_.tag, tag)
                    .eq(SystemTagVO_.resourceUuid).table1(LongJobVO_.uuid)
                .table1()
                    .notIn(LongJobVO_.state, LongJobState.finalStates)
                    .select(LongJobVO_.uuid)
                .list();
        if (longJobUuids.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        List<LongJobVO> jobs = Q.New(LongJobVO.class).in(LongJobVO_.uuid, longJobUuids).list();
        new While<>(jobs).each((job, comp) -> {
            JobDetails detail = new JobDetails();
            detail.setLongJobUuid(job.getUuid());
            detail.setLongJobState(job.getState().toString());
            detail.setSoftwarePackageUuid(job.getTargetResourceUuid());

            APIUploadSoftwarePackageMsg apiMsg = JSONObjectUtil.toObject(job.getJobData(), APIUploadSoftwarePackageMsg.class);

            GetFileDownloadProgressMsg gmsg = new GetFileDownloadProgressMsg();
            gmsg.setTaskUuid(job.getTargetResourceUuid());
            gmsg.setHostUuid(apiMsg.getHostUuid());
            bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, apiMsg.getHostUuid());
            bus.send(gmsg, new CloudBusCallBack(comp) {
                @Override
                public void run(MessageReply r) {
                    if (r.isSuccess()) {
                        GetFileDownloadProgressReply gr = r.castReply();
                        detail.setOffset(gr.getDownloadSize());
                        detail.setSoftwarePackageUploadUrl(gr.getInstallPath());
                        reply.addExistingJobDetails(detail);
                    }
                    comp.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIGetDirectoryUsageMsg msg) {
        APIGetDirectoryUsageReply reply = new APIGetDirectoryUsageReply();
        if (!PathUtil.exists(msg.getDirectoryPath())) {
            ShellCommandUtils.createDirectory(msg.getDirectoryPath());
        }

        ShellCommandUtils.DirectoryUsage usgae = ShellCommandUtils.getDirectoryUsage(msg.getDirectoryPath());
        reply.setTotalCapacity(usgae.totalBytes);
        reply.setAvailableCapacity(usgae.availableBytes);

        bus.reply(msg, reply);
    }

    private void handle(APIInstallSoftwarePackageMsg msg) {
        threads.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "install-software-package-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIInstallSoftwarePackageEvent evt = new APIInstallSoftwarePackageEvent(msg.getId());
                installSoftwarePackage(msg, new Completion(chain, msg) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("install-software-package-%s", msg.getUuid());
            }
        });
    }

    private void handle(APIUninstallSoftwarePackageMsg msg) {
        threads.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "uninstall-software-package-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUninstallSoftwarePackageEvent evt = new APIUninstallSoftwarePackageEvent(msg.getId());
                uninstallSoftwarePackage(msg, new Completion(chain, msg) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("uninstall-software-package-%s", msg.getUuid());
            }
        });
    }

    private void handle(APIUploadSoftwarePackageToBackupStorageMsg msg) {
        threads.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "upload-software-package-to-backupStorage";
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUploadSoftwarePackageToBackupStorageEvent event = new APIUploadSoftwarePackageToBackupStorageEvent(msg.getId());
                uploadSoftwarePackageToBackupStorage(UploadSoftwarePackageToBackupStorageLongJobData.buildFileLongJobDataFromApiMsg(msg),
                        new ReturnValueCompletion<SoftwarePackageInventory>(chain, msg) {
                            @Override
                            public void success(SoftwarePackageInventory inv) {
                                event.setInventory(inv);
                                bus.publish(event);
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                event.setError(errorCode);
                                bus.publish(event);
                                chain.next();
                            }
                        });
            }

            @Override
            public String getName() {
                return "upload-software-package-to-backupStorage";
            }
        });
    }

    private void handle(UploadSoftwarePackageToBackupStorageMsg msg) {
        UploadSoftwarePackageToBackupStorageReply reply = new UploadSoftwarePackageToBackupStorageReply();
        uploadSoftwarePackageToBackupStorage(UploadSoftwarePackageToBackupStorageLongJobData.buildFileLongJobDataFromMsg(msg), new ReturnValueCompletion<SoftwarePackageInventory>(msg) {
            @Override
            public void success(SoftwarePackageInventory inv) {
                reply.setInventory(inv);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIUploadAndExecuteSoftwareUpgradePackageMsg msg) {
        threads.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("upgrade-software-package-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUploadAndExecuteSoftwareUpgradePackageEvent event = new APIUploadAndExecuteSoftwareUpgradePackageEvent(msg.getId());
                uploadAndExecuteSoftwareUpgradePackage(UploadSoftwarePackageToBackupStorageLongJobData.buildFileLongJobDataFromApiMsg(msg), new ReturnValueCompletion<SoftwarePackageInventory>(chain, msg) {
                    @Override
                    public void success(SoftwarePackageInventory softwarePackage) {
                        event.setInventory(softwarePackage);
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("upgrade-software-package-%s", msg.getUuid());
            }
        });
    }

    private void handle(UploadAndExecuteSoftwareUpgradePackageMsg msg) {
        UploadAndExecuteSoftwareUpgradePackageReply reply = new UploadAndExecuteSoftwareUpgradePackageReply();
        uploadAndExecuteSoftwareUpgradePackage(UploadSoftwarePackageToBackupStorageLongJobData.buildFileLongJobDataFromMsg(msg), new ReturnValueCompletion<SoftwarePackageInventory>(msg) {
            @Override
            public void success(SoftwarePackageInventory softwarePackage) {
                reply.setInventory(softwarePackage);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APICleanUpgradeSoftwarePackageMsg msg) {
        APICleanUpgradeSoftwarePackageEvent event = new APICleanUpgradeSoftwarePackageEvent(msg.getId());
        doCleanUpgradePackage(msg.getUuid(), new Completion(msg) {
            @Override
            public void success() {
                SQL.New(SoftwarePackageVO.class)
                        .eq(SoftwarePackageVO_.uuid, msg.getUuid())
                        .set(SoftwarePackageVO_.status, SoftwarePackageStatus.Installed.toString())
                        .update();
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void handle(CleanUpgradeSoftwarePackageMsg msg) {
        CleanSoftwarePackageReply reply = new CleanSoftwarePackageReply();
        SoftwarePackageVO softwarePackage = Q.New(SoftwarePackageVO.class)
                .eq(SoftwarePackageVO_.uuid, msg.getUuid())
                .find();
        if (softwarePackage == null) {
            reply.setError(err(SysErrors.RESOURCE_NOT_FOUND, "software package not found, uuid: %s", msg.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        String upgradeUnzipInstallPath = msg.getUpgradeUnzipInstallPath();
        if (upgradeUnzipInstallPath == null
                && !Objects.equals(softwarePackage.getUnzipInstallPath(), msg.getOriginalUnzipInstallPath())) {
            upgradeUnzipInstallPath = softwarePackage.getUnzipInstallPath();
        }

        String upgradeBackupStorageUuid = msg.getUpgradeBackupStorageUuid() != null
                ? msg.getUpgradeBackupStorageUuid()
                : SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID.getTokenByResourceUuid(
                        msg.getUuid(), SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN);
        String upgradeBackupStorageHostUuid = msg.getUpgradeBackupStorageHostUuid() != null
                ? msg.getUpgradeBackupStorageHostUuid()
                : SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID.getTokenByResourceUuid(
                        msg.getUuid(), SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN);

        doCleanUpgradePackage(softwarePackage, upgradeBackupStorageUuid, upgradeBackupStorageHostUuid,
                msg.getUpgradeInstallPath(), upgradeUnzipInstallPath, new Completion(msg) {
            @Override
            public void success() {
                SQL.New(SoftwarePackageVO.class)
                        .eq(SoftwarePackageVO_.uuid, msg.getUuid())
                        .set(SoftwarePackageVO_.status, SoftwarePackageStatus.Installed.toString())
                        .set(SoftwarePackageVO_.installPath, msg.getOriginalInstallPath())
                        .set(SoftwarePackageVO_.unzipInstallPath, msg.getOriginalUnzipInstallPath())
                        .set(SoftwarePackageVO_.md5sum, msg.getOriginalMd5sum())
                        .set(SoftwarePackageVO_.size, msg.getOriginalSize())
                        .update();
                restoreBackupStorageTag(SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID, msg.getUuid(),
                        msg.getOriginalBackupStorageUuid(), SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN);
                restoreBackupStorageTag(SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID, msg.getUuid(),
                        msg.getOriginalBackupStorageHostUuid(), SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    // ------------------------------------- Software Packages Handers ------------------------------------

    @SuppressWarnings("unchecked")
    private void uploadSoftwarePackage(UploadSoftwarePackageLongJobData msgData, ReturnValueCompletion<SoftwarePackageInventory> valueCompletion) {
        SoftwarePackageVO vo = new SoftwarePackageVO();
        vo.setUuid(msgData.resourceUuid);
        vo.setName(msgData.name);
        vo.setManagementNodeUuid(msgData.managementNodeUuid);
        vo.setHostUuid(msgData.hostUuid);
        vo.setStatus(SoftwarePackageStatus.Uploading.toString());
        vo.setInstallPath(msgData.installPath);
        vo.setType(msgData.type);
        vo = databases.persist(vo);
        SoftwarePackageVO finalVo = vo;

        SimpleFlowChain.of("upload-software-package")
        .then(Flow.of("create-update-config-tag")
            .handle(trigger -> {
                createUploadConfig(finalVo);
                trigger.next();
            })
            .build())
        .then(Flow.of("download-software-package")
            .handle(trigger -> {
                UploadFileToHostMsg umsg = new UploadFileToHostMsg();
                umsg.setHostUuid(msgData.hostUuid);
                umsg.setUrl(msgData.url);
                umsg.setInstallPath(finalVo.getInstallPath());
                umsg.setTaskUuid(finalVo.getUuid());
                bus.makeTargetServiceIdByResourceUuid(umsg, HostConstant.SERVICE_ID, msgData.hostUuid);
                bus.send(umsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        UploadFileToHostReply r = reply.castReply();
                        if (msgData.needTrack()) {
                            SystemTagCreator creator = SoftwarePackageSystemTags.UPLOAD_URL.newSystemTagCreator(finalVo.getUuid());
                            creator.inherent = false;
                            creator.recreate = true;
                            creator.setTagByTokens(map(e(SoftwarePackageSystemTags.UPLOAD_URL_TOKEN, r.getDirectUploadUrl())));
                            creator.create();
                            new UploadSoftwarePackageTracker().runTrackTask(finalVo.getUuid(), msgData.hostUuid);
                        } else {
                            finalVo.setMd5sum(r.getMd5sum());
                            finalVo.setSize(r.getSize());
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .then(Flow.of("get-software-package-true-type")
            .skipIf(data -> msgData.needTrack())
            .handle(trigger -> {
                String unzipInstallPath = String.format("%s_%s_%d", finalVo.getInstallPath(), finalVo.getMd5sum(), System.currentTimeMillis());
                finalVo.setUnzipInstallPath(unzipInstallPath);
                databases.updateAndRefresh(finalVo);

                List<UploadSoftwarePackageExtensionPoint> exts = plugins.getExtensionList(UploadSoftwarePackageExtensionPoint.class);
                String type = null;
                for (UploadSoftwarePackageExtensionPoint ext : exts) {
                    type = ext.resolveAndPrepareActualType(finalVo.getType(), finalVo.getInstallPath(), finalVo.getUnzipInstallPath());
                    if (type != null) {
                        break;
                    }
                }
                if (type == null) {
                    trigger.fail(err(UNSUPPORTED_SOFTWARE_TYPE,
                            "failed to identify software package type. package: %s, installPath: %s, unzipPath: %s. " +
                                    "please verify the package format is correct and a corresponding extension point is registered.",
                            finalVo.getName(), finalVo.getInstallPath(), finalVo.getUnzipInstallPath()));
                    return;
                }
                finalVo.setType(type);
                trigger.next();
            })
            .build())
        .propagateExceptionTo(valueCompletion)
        .done(() -> {
            if (msgData.needTrack()) {
                valueCompletion.success(SoftwarePackageInventory.valueOf(finalVo));
                return;
            }
            finalVo.setStatus(Uploaded.toString());
            databases.updateAndRefresh(finalVo);
            valueCompletion.success(SoftwarePackageInventory.valueOf(finalVo));
        })
        .error(errorCode -> {
            finalVo.setStatus(SoftwarePackageStatus.UploadFailed.toString());
            databases.updateAndRefresh(finalVo);
            valueCompletion.fail(errorCode);
        })
        .start();
    }

    private void cleanupSoftwarePackage(SoftwarePackageVO softwarePackageVO, Completion completion) {
        if (Objects.equals(softwarePackageVO.getType(), STORAGE_SOFTWARE_PACKAGE)) {
            try {
                if (StringUtils.isNotEmpty(softwarePackageVO.getInstallPath())) {
                    ShellCommandUtils.deleteRecursive(softwarePackageVO.getInstallPath());
                }
                if (StringUtils.isNotEmpty(softwarePackageVO.getUnzipInstallPath())) {
                    ShellCommandUtils.deleteRecursive(softwarePackageVO.getUnzipInstallPath());
                }
            } catch (OperationFailureException e) {
                completion.fail(e.getErrorCode());
                return;
            }
            databases.removeByPrimaryKey(softwarePackageVO.getUuid(), SoftwarePackageVO.class);
            completion.success();
            return;
        }

        SoftwarePackageExtensionPoint ext = plugins.getExtensionFromMap(softwarePackageVO.getType(), SoftwarePackageExtensionPoint.class);
        if (ext == null) {
            completion.fail(err(UNSUPPORTED_SOFTWARE_TYPE,
                    "no extension point found for software package type: %s", softwarePackageVO.getType()));
            return;
        }
        ext.cleanSoftwarePackage(softwarePackageVO, new Completion(completion) {
            @Override
            public void success() {
                databases.removeByPrimaryKey(softwarePackageVO.getUuid(), SoftwarePackageVO.class);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void installSoftwarePackage(APIInstallSoftwarePackageMsg msg, Completion completion) {
        SoftwarePackageVO softwarePackageVO = msg.getSoftwarePackageVO();

        SimpleFlowChain.of("install-software-package-" + softwarePackageVO.getUuid())
        .then(Flow.of("create-update-config-tag")
            .handle(trigger -> {
                SystemTagCreator creator = SoftwarePackageSystemTags.SOFTWARE_PACKAGE_INSTALL_CONFIG.newSystemTagCreator(softwarePackageVO.getUuid());
                creator.inherent = false;
                creator.recreate = true;
                creator.setTagByTokens(map(e(SoftwarePackageSystemTags.SOFTWARE_PACKAGE_INSTALL_CONFIG_TOKEN,
                        Base64.getEncoder().encodeToString(JSONObjectUtil.toJsonString(buildAPIInstallSoftwarePackageMsg(msg))
                                .getBytes(StandardCharsets.UTF_8)))));
                creator.create();
                trigger.next();
            })
            .build())
        .then(Flow.of("check-software-package-for-non-mn-installation")
            .skipIf(data -> CoreGlobalProperty.UNIT_TEST_ON)
            .handle(trigger -> {
                SoftwarePackageExtensionPoint ext = plugins.getExtensionFromMap(softwarePackageVO.getType(), SoftwarePackageExtensionPoint.class);
                if (ext == null) {
                    trigger.fail(err(UNSUPPORTED_SOFTWARE_TYPE, "no extension point found for software package type: %s", softwarePackageVO.getType()));
                    return;
                }
                if (ext.isInstalledAndUnmanagedByMn()) {
                    trigger.fail(err(GENERAL_ERROR, "a non-management node installation of the software package is detected in this environment. " +
                            "to proceed with a new management node-based installation, please first:\n" +
                            "1. uninstall the existing manually installed components\n" +
                            "2. ensure the environment is completely clean\n" +
                            "note: this installation must be performed exclusively through the management node"));
                    return;
                }
                trigger.next();
            })
            .build())
        .then(Flow.of("install-software-package")
            .handle(trigger -> {
                SQL.New(SoftwarePackageVO.class).eq(SoftwarePackageVO_.uuid, softwarePackageVO.getUuid()).set(SoftwarePackageVO_.status, Installing.toString()).update();

                SoftwarePackageExtensionPoint ext = plugins.getExtensionFromMap(softwarePackageVO.getType(), SoftwarePackageExtensionPoint.class);
                if (ext == null) {
                    trigger.fail(err(UNSUPPORTED_SOFTWARE_TYPE,
                            "no extension point found for software package type: %s", softwarePackageVO.getType()));
                    return;
                }
                ext.installSoftwarePackage(softwarePackageVO, msg.getConfig(), msg.getSession() , new Completion(trigger) {
                    @Override
                    public void success() {
                        SQL.New(SoftwarePackageVO.class).eq(SoftwarePackageVO_.uuid, softwarePackageVO.getUuid())
                                .set(SoftwarePackageVO_.status, Installed.toString()).update();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        SQL.New(SoftwarePackageVO.class).eq(SoftwarePackageVO_.uuid, softwarePackageVO.getUuid())
                                .set(SoftwarePackageVO_.status, InstallFailed.toString()).update();
                        trigger.fail(errorCode);
                    }
                });
            })
            .build())
        .propagateExceptionTo(completion)
        .done(completion::success)
        .error(completion::fail)
        .start();
    }

    private void uninstallSoftwarePackage(APIUninstallSoftwarePackageMsg msg, Completion completion) {
        SoftwarePackageVO softwarePackageVO = msg.getSoftwarePackageVO();

        SimpleFlowChain.of(String.format("uninstall-software-package-%s", softwarePackageVO.getUuid()))
        .then(Flow.of("uninstall-software-package")
            .handle(trigger -> {
                SoftwarePackageExtensionPoint ext = plugins.getExtensionFromMap(softwarePackageVO.getType(), SoftwarePackageExtensionPoint.class);
                if (ext == null) {
                    trigger.fail(err(UNSUPPORTED_SOFTWARE_TYPE,
                            "no extension point found for software package type: %s", softwarePackageVO.getType()));
                    return;
                }
                ext.uninstallSoftwarePackage(softwarePackageVO, new Completion(trigger) {
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
            .build())
        .propagateExceptionTo(completion)
        .done(completion::success)
        .error(completion::fail)
        .start();
    }

    static class UploadPackageToBsContext {
        SoftwarePackageVO vo;
        Long backupStorageAvailableCapacity;
        final HashMap<String, Long> fileSizes = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private void uploadSoftwarePackageToBackupStorage(UploadSoftwarePackageToBackupStorageLongJobData msgData, ReturnValueCompletion<SoftwarePackageInventory> valueCompletion) {
        UploadPackageToBsContext context = new UploadPackageToBsContext();
        context.vo = new SoftwarePackageVO();
        context.vo.setUuid(msgData.softwarePackageUuid);
        context.vo.setName(msgData.name);
        context.vo.setStatus(SoftwarePackageStatus.Uploading.toString());
        context.vo.setInstallPath(msgData.installPath);
        context.vo.setType(msgData.type);
        context.vo = databases.persist(context.vo);

        SimpleFlowChain.of("upload-software-package-to-backupStorage")
        .then(Flow.of("create-update-config-tag")
            .handle(trigger -> {
                createUploadConfig(context.vo);
                trigger.next();
            })
            .build())
        .then(Flow.of("get-backupStorage-and-available-capacity")
            .handle(trigger -> {
                if (msgData.backupStorageUuid != null) {
                    context.backupStorageAvailableCapacity = Q.New(BackupStorageVO.class)
                            .eq(BackupStorageVO_.uuid, msgData.backupStorageUuid)
                            .select(BackupStorageVO_.availableCapacity).findValue();
                    if (context.backupStorageAvailableCapacity == null) {
                        trigger.fail(err(INVALID_BACKUP_STORAGE_FOR_PACKAGE,
                                "backup storage [uuid:%s] not found", msgData.backupStorageUuid));
                        return;
                    }
                    trigger.next();
                    return;
                }

                Tuple t = SoftwarePackageBackupStorageUtils.getBackupStorageUuidAndAvailableCapacity(getEstimatedImageSize(msgData.type));
                if (t == null) {
                    trigger.fail(err(INVALID_BACKUP_STORAGE_FOR_PACKAGE,
                            "failed to find backup storage state=Enabled and status=Connected to upload package"));
                    return;
                }

                msgData.backupStorageUuid = t.get(0, String.class);
                context.backupStorageAvailableCapacity = t.get(1, Long.class);
                if (context.backupStorageAvailableCapacity == null) {
                    trigger.fail(err(INVALID_BACKUP_STORAGE_FOR_PACKAGE,
                            "backup storage [uuid:%s] has null available capacity", msgData.backupStorageUuid));
                    return;
                }
                trigger.next();
            })
            .build())
        .then(Flow.of("download-software-package")
            .handle(trigger -> {
                UploadFileToBackupStorageHostMsg umsg = new UploadFileToBackupStorageHostMsg();
                umsg.setBackupStorageUuid(msgData.backupStorageUuid);
                umsg.setUrl(msgData.url);
                umsg.setInstallPath(context.vo.getInstallPath());
                umsg.setTaskUuid(context.vo.getUuid());
                bus.makeTargetServiceIdByResourceUuid(umsg, BackupStorageConstant.SERVICE_ID, msgData.backupStorageUuid);
                bus.send(umsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        UploadFileToBackupStorageHostReply r = reply.castReply();
                        msgData.backupStorageHostUuid = r.getBackupStorageHostUuid();

                        SystemTagCreator creator = SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID.newSystemTagCreator(context.vo.getUuid());
                        creator.inherent = false;
                        creator.recreate = true;
                        creator.setTagByTokens(map(e(SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN, msgData.backupStorageHostUuid)));
                        creator.create();

                        creator = SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID.newSystemTagCreator(context.vo.getUuid());
                        creator.inherent = false;
                        creator.recreate = true;
                        creator.setTagByTokens(map(e(SoftwarePackageSystemTags.SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN, msgData.backupStorageUuid)));
                        creator.create();

                        if (msgData.needTrack()) {
                            creator = SoftwarePackageSystemTags.UPLOAD_URL.newSystemTagCreator(context.vo.getUuid());
                            creator.inherent = false;
                            creator.recreate = true;
                            creator.setTagByTokens(map(e(SoftwarePackageSystemTags.UPLOAD_URL_TOKEN, r.getDirectUploadUrl())));
                            creator.create();
                            new UploadSoftwarePackageToBackupStorageTracker().runTrackTask(msgData);
                        } else {
                            context.vo.setMd5sum(r.getMd5sum());
                            context.vo.setSize(r.getSize());
                        }
                        trigger.next();
                    }
                });
            })
            .rollback(trigger -> {
                if (msgData.backupStorageUuid == null || msgData.backupStorageHostUuid == null) {
                    trigger.rollback();
                    return;
                }

                DeleteFilesOnBackupStorageHostMsg dmsg = new DeleteFilesOnBackupStorageHostMsg();
                dmsg.setBackupStorageUuid(msgData.backupStorageUuid);
                dmsg.setBackupStorageHostUuid(msgData.backupStorageHostUuid);
                if (msgData.installPath != null) {
                    dmsg.getFilePaths().add(msgData.installPath);
                }
                if (msgData.unzipInstallPath != null) {
                    dmsg.getFilePaths().add(msgData.unzipInstallPath);
                }
                bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, msgData.backupStorageUuid);
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to delete files [%s, %s] on backup storage host %s",
                                    msgData.installPath, msgData.unzipInstallPath, msgData.backupStorageHostUuid));
                        }
                        trigger.rollback();
                    }
                });
            })
            .build())
        .then(Flow.of("unzip-software-package")
            .skipIf(data -> msgData.needTrack())
            .handle(trigger -> {
                UnzipFileOnBackupStorageHostMsg unzipMsg = new UnzipFileOnBackupStorageHostMsg();
                unzipMsg.setBackupStorageUuid(msgData.backupStorageUuid);
                unzipMsg.setBackupStorageHostUuid(msgData.backupStorageHostUuid);
                unzipMsg.setInstallPath(context.vo.getInstallPath());
                bus.makeTargetServiceIdByResourceUuid(unzipMsg, BackupStorageConstant.SERVICE_ID, msgData.backupStorageUuid);
                bus.send(unzipMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }
                        UnzipFileOnBackupStorageHostReply unzipR = reply.castReply();
                        context.vo.setUnzipInstallPath(unzipR.getUnzipInstallPath());
                        msgData.unzipInstallPath = unzipR.getUnzipInstallPath();
                        context.fileSizes.putAll(unzipR.getFileSizes());
                        trigger.next();
                    }
                });
            })
            .rollback(trigger -> {
                if (msgData.unzipInstallPath != null
                        && msgData.backupStorageUuid != null
                        && msgData.backupStorageHostUuid != null) {
                    DeleteFilesOnBackupStorageHostMsg dmsg = new DeleteFilesOnBackupStorageHostMsg();
                    dmsg.setBackupStorageUuid(msgData.backupStorageUuid);
                    dmsg.setBackupStorageHostUuid(msgData.backupStorageHostUuid);
                    dmsg.getFilePaths().add(msgData.unzipInstallPath);
                    bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, msgData.backupStorageUuid);
                    bus.send(dmsg);
                }
                trigger.rollback();
            })
            .build())
        .then(Flow.of("adjust-bs-capacity")
            .skipIf(data -> msgData.needTrack())
            .handle(trigger -> {
                UploadSoftwarePackageToBackupStorageExtensionPoint ext = plugins.getExtensionFromMap(context.vo.getType(), UploadSoftwarePackageToBackupStorageExtensionPoint.class);
                if (ext == null) {
                    trigger.fail(err(UNSUPPORTED_SOFTWARE_TYPE,
                            "no UploadSoftwarePackageToBackupStorageExtensionPoint found for software package type: %s", context.vo.getType()));
                    return;
                }
                Map<String, Long> imagesSize = ext.getImagesSize(context.fileSizes);
                long imageTotalSize = imagesSize.values().stream().mapToLong(Long::longValue).sum();
                if (imageTotalSize > context.backupStorageAvailableCapacity) {
                    trigger.fail(err(INSUFFICIENT_CAPACITY_FOR_BACKUP_STORAGE,
                            "imagesTotalSize %d greater than backupStorage available capacity %d", imageTotalSize, context.backupStorageAvailableCapacity));
                    return;
                }
                msgData.imagesPath = new ArrayList<>(imagesSize.keySet());
                trigger.next();
            })
            .build())
        .then(Flow.of("after-upload-software-package-to-backupStorage")
            .skipIf(data -> msgData.needTrack())
            .handle(trigger -> {
                UploadSoftwarePackageToBackupStorageExtensionPoint ext = plugins.getExtensionFromMap(context.vo.getType(), UploadSoftwarePackageToBackupStorageExtensionPoint.class);
                if (ext == null) {
                    trigger.fail(err(UNSUPPORTED_SOFTWARE_TYPE, "no UploadSoftwarePackageToBackupStorageExtensionPoint found for software package type: %s", context.vo.getType()));
                    return;
                }
                ext.afterUploadSoftwarePackageToBackupStorage(context.vo, msgData, new Completion(trigger) {
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
            .build())
        .propagateExceptionTo(valueCompletion)
        .done(() -> {
            if (msgData.needTrack()) {
                valueCompletion.success(SoftwarePackageInventory.valueOf(context.vo));
                return;
            }
            context.vo.setStatus(Uploaded.toString());
            databases.updateAndRefresh(context.vo);
            valueCompletion.success(SoftwarePackageInventory.valueOf(context.vo));
        })
        .error(errorCode -> {
            context.vo.setStatus(SoftwarePackageStatus.UploadFailed.toString());
            databases.updateAndRefresh(context.vo);
            valueCompletion.fail(errorCode);
        })
        .start();
    }

    static class UploadExecuteUpgradePackageContext {
        Long backupStorageAvailableCapacity;
        final HashMap<String, Long> fileSizes = new HashMap<>();
        boolean upgradePackageUploaded = false;
    }

    @SuppressWarnings("unchecked")
    private void uploadAndExecuteSoftwareUpgradePackage(UploadSoftwarePackageToBackupStorageLongJobData msgData, ReturnValueCompletion<SoftwarePackageInventory> valueCompletion) {
        if (Objects.equals(msgData.upgradeType, UpgradeType.Reexecute.toString())) {
            reUploadAndExecuteSoftwareUpgradePackage(msgData, valueCompletion);
            return;
        }

        SoftwarePackageVO original = Q.New(SoftwarePackageVO.class)
                .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid).find();
        if (original != null) {
            msgData.originalInstallPath = original.getInstallPath();
            msgData.originalUnzipInstallPath = original.getUnzipInstallPath();
            msgData.originalMd5sum = original.getMd5sum();
            msgData.originalSize = original.getSize();
            msgData.originalBackupStorageUuid =
                    SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID.getTokenByResourceUuid(original.getUuid(), SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN);
            msgData.originalBackupStorageHostUuid =
                    SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID.getTokenByResourceUuid(original.getUuid(), SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN);
        }

        UploadExecuteUpgradePackageContext context = new UploadExecuteUpgradePackageContext();

        SimpleFlowChain.of("upload-software-upgrade-package")
        .then(Flow.of("update-software-package-status-to-upgrading")
            .handle(trigger -> {
                SQL.New(SoftwarePackageVO.class)
                        .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid)
                        .set(SoftwarePackageVO_.status, Upgrading.toString())
                        .set(SoftwarePackageVO_.installPath, msgData.installPath)
                        .update();
                trigger.next();
            })
            .build())
        .then(Flow.of("get-backupStorage-and-available-capacity")
            .handle(trigger -> {
                if (msgData.backupStorageUuid != null) {
                    context.backupStorageAvailableCapacity = Q.New(BackupStorageVO.class)
                            .eq(BackupStorageVO_.uuid, msgData.backupStorageUuid)
                            .select(BackupStorageVO_.availableCapacity).findValue();
                    if (context.backupStorageAvailableCapacity == null) {
                        trigger.fail(err(INVALID_BACKUP_STORAGE_FOR_PACKAGE,
                                "backup storage [uuid:%s] not found", msgData.backupStorageUuid));
                        return;
                    }
                    trigger.next();
                    return;
                }

                Tuple t = SoftwarePackageBackupStorageUtils.getUpgradeBackupStorageUuidAndAvailableCapacity(
                        msgData.softwarePackageUuid, getEstimatedImageSize(msgData.type));
                if (t == null) {
                    trigger.fail(err(INVALID_BACKUP_STORAGE_FOR_PACKAGE,
                            "no backup storage state=Enabled and status=Connected with sufficient capacity available in the same zone(s)" +
                            " as the backup storage currently hosting software package [uuid:%s]",
                            msgData.softwarePackageUuid));
                    return;
                }

                msgData.backupStorageUuid = t.get(0, String.class);
                context.backupStorageAvailableCapacity = t.get(1, Long.class);
                if (context.backupStorageAvailableCapacity == null) {
                    trigger.fail(err(INVALID_BACKUP_STORAGE_FOR_PACKAGE,
                            "backup storage [uuid:%s] has null available capacity", msgData.backupStorageUuid));
                    return;
                }
                trigger.next();
            })
            .build())
        .then(Flow.of("download-software-package")
            .handle(trigger -> {
                UploadFileToBackupStorageHostMsg umsg = new UploadFileToBackupStorageHostMsg();
                umsg.setBackupStorageUuid(msgData.backupStorageUuid);
                umsg.setUrl(msgData.url);
                umsg.setInstallPath(msgData.installPath);
                umsg.setTaskUuid(msgData.softwarePackageUuid);
                bus.makeTargetServiceIdByResourceUuid(umsg, BackupStorageConstant.SERVICE_ID, msgData.backupStorageUuid);
                bus.send(umsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        UploadFileToBackupStorageHostReply r = reply.castReply();
                        msgData.backupStorageHostUuid = r.getBackupStorageHostUuid();

                        SystemTagCreator creator = SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID.newSystemTagCreator(msgData.softwarePackageUuid);
                        creator.inherent = false;
                        creator.recreate = true;
                        creator.setTagByTokens(map(e(SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN, msgData.backupStorageHostUuid)));
                        creator.create();

                        creator = SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID.newSystemTagCreator(msgData.softwarePackageUuid);
                        creator.inherent = false;
                        creator.recreate = true;
                        creator.setTagByTokens(map(e(SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN, msgData.backupStorageUuid)));
                        creator.create();

                        if (msgData.needTrack()) {
                            creator = UPLOAD_URL.newSystemTagCreator(msgData.softwarePackageUuid);
                            creator.inherent = false;
                            creator.recreate = true;
                            creator.setTagByTokens(map(e(UPLOAD_URL_TOKEN, r.getDirectUploadUrl())));
                            creator.create();

                            UploadSoftwarePackageToBackupStorageTracker tracker = new UploadSoftwarePackageToBackupStorageTracker();
                            tracker.setUpgrade(true);
                            tracker.runTrackTask(msgData);
                        } else {
                            SQL.New(SoftwarePackageVO.class)
                                    .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid)
                                    .set(SoftwarePackageVO_.md5sum, r.getMd5sum())
                                    .set(SoftwarePackageVO_.size, r.getSize())
                                    .update();
                        }
                        trigger.next();
                    }
                });
            })
            .rollback(trigger -> {
                // Skip file cleanup when failure occurred in execute phase (after upload completed).
                // Files must be preserved for Reexecute.
                if (!context.upgradePackageUploaded) {
                    trigger.rollback();
                    return;
                }

                String bsUuid = msgData.backupStorageUuid;
                String bsHostUuid = msgData.backupStorageHostUuid;

                // When upload fails before receiving a reply, backupStorageHostUuid is still null.
                // Restore it from the system tag written by the previous successful upload.
                if (bsHostUuid == null) {
                    bsHostUuid = SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID
                            .getTokenByResourceUuid(msgData.softwarePackageUuid, SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN);
                }

                if (bsUuid == null || bsHostUuid == null) {
                    trigger.rollback();
                    return;
                }

                final String finalBsHostUuid = bsHostUuid;
                DeleteFilesOnBackupStorageHostMsg dmsg = new DeleteFilesOnBackupStorageHostMsg();
                dmsg.setBackupStorageUuid(bsUuid);
                dmsg.setBackupStorageHostUuid(finalBsHostUuid);
                if (msgData.installPath != null) {
                    dmsg.getFilePaths().add(msgData.installPath);
                }
                if (msgData.unzipInstallPath != null) {
                    dmsg.getFilePaths().add(msgData.unzipInstallPath);
                }
                bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to delete files [%s, %s] on backup storage host %s",
                                    msgData.installPath, msgData.unzipInstallPath, finalBsHostUuid));
                        }
                        trigger.rollback();
                    }
                });
            })
            .build())
        .then(Flow.of("unzip-software-package")
            .skipIf(data -> msgData.needTrack())
            .handle(trigger -> {
                UnzipFileOnBackupStorageHostMsg unzipMsg = new UnzipFileOnBackupStorageHostMsg();
                unzipMsg.setBackupStorageUuid(msgData.backupStorageUuid);
                unzipMsg.setBackupStorageHostUuid(msgData.backupStorageHostUuid);
                unzipMsg.setInstallPath(msgData.installPath);
                bus.makeTargetServiceIdByResourceUuid(unzipMsg, BackupStorageConstant.SERVICE_ID, msgData.backupStorageUuid);
                bus.send(unzipMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }
                        UnzipFileOnBackupStorageHostReply unzipR = reply.castReply();
                        msgData.unzipInstallPath = unzipR.getUnzipInstallPath();
                        SQL.New(SoftwarePackageVO.class)
                                .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid)
                                .set(SoftwarePackageVO_.unzipInstallPath, unzipR.getUnzipInstallPath())
                                .update();
                        context.fileSizes.putAll(unzipR.getFileSizes());
                        trigger.next();
                    }
                });
            })
            .rollback(trigger -> {
                // Skip file cleanup when failure occurred in execute phase (after upload completed).
                // Files must be preserved for Reexecute.
                if (!context.upgradePackageUploaded) {
                    trigger.rollback();
                    return;
                }

                if (msgData.unzipInstallPath != null
                        && msgData.backupStorageUuid != null
                        && msgData.backupStorageHostUuid != null) {
                    DeleteFilesOnBackupStorageHostMsg dmsg = new DeleteFilesOnBackupStorageHostMsg();
                    dmsg.setBackupStorageUuid(msgData.backupStorageUuid);
                    dmsg.setBackupStorageHostUuid(msgData.backupStorageHostUuid);
                    dmsg.getFilePaths().add(msgData.unzipInstallPath);
                    bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, msgData.backupStorageUuid);
                    bus.send(dmsg);
                }
                trigger.rollback();
            })
            .build())
        .then(Flow.of("adjust-bs-capacity")
            .skipIf(data -> msgData.needTrack())
            .handle(trigger -> {
                String type = Q.New(SoftwarePackageVO.class)
                        .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid)
                        .select(SoftwarePackageVO_.type)
                        .findValue();
                UploadSoftwarePackageToBackupStorageExtensionPoint ext = plugins.getExtensionFromMap(type, UploadSoftwarePackageToBackupStorageExtensionPoint.class);
                if (ext == null) {
                    trigger.fail(err(UNSUPPORTED_SOFTWARE_TYPE,
                            "no UploadSoftwarePackageToBackupStorageExtensionPoint found for software package type: %s", type));
                    return;
                }
                Map<String, Long> imagesSize = ext.getImagesSize(context.fileSizes);
                long imageTotalSize = imagesSize.values().stream().mapToLong(Long::longValue).sum();
                if (imageTotalSize > context.backupStorageAvailableCapacity) {
                    trigger.fail(err(INSUFFICIENT_CAPACITY_FOR_BACKUP_STORAGE,
                            "imagesTotalSize %d greater than backupStorage available capacity %d",
                            imageTotalSize, context.backupStorageAvailableCapacity));
                    return;
                }
                msgData.imagesPath = new ArrayList<>(imagesSize.keySet());
                trigger.next();
            })
            .build())
        .then(Flow.of("upgrade-software-package")
            .skipIf(data -> msgData.needTrack())
            .handle(trigger -> {
                SoftwarePackageVO softwarePackageVO = Q.New(SoftwarePackageVO.class).eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid).find();
                UploadSoftwarePackageToBackupStorageExtensionPoint ext =
                        plugins.getExtensionFromMap(softwarePackageVO.getType(), UploadSoftwarePackageToBackupStorageExtensionPoint.class);
                if (ext == null) {
                    trigger.fail(err(UNSUPPORTED_SOFTWARE_TYPE, "no UploadSoftwarePackageToBackupStorageExtensionPoint found for software package type: %s", softwarePackageVO.getType()));
                    return;
                }
                String upgradePackagePath = ext.getUpgradePackagePath(context.fileSizes);
                if (upgradePackagePath == null) {
                    trigger.fail(err(GENERAL_ERROR, "no upgrade package found in software package"));
                    return;
                }
                msgData.upgradePackagePath = upgradePackagePath;

                SystemTagCreator creator = SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH.newSystemTagCreator(msgData.softwarePackageUuid);
                creator.inherent = false;
                creator.recreate = true;
                creator.setTagByTokens(map(e(SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH_TOKEN, upgradePackagePath)));
                creator.create();

                SQL.New(SoftwarePackageVO.class)
                        .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid)
                        .set(SoftwarePackageVO_.status, UpgradePackageUploaded.toString())
                        .update();
                softwarePackageVO.setStatus(UpgradePackageUploaded.toString());
                context.upgradePackageUploaded = true;

                ext.upgradeSoftwarePackage(softwarePackageVO, msgData, new Completion(trigger) {
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
            .build())
        .propagateExceptionTo(valueCompletion)
        .done(() -> {
            if (msgData.needTrack()) {
                valueCompletion.success(SoftwarePackageInventory.valueOf(
                        databases.findByUuid(msgData.softwarePackageUuid, SoftwarePackageVO.class)));
                return;
            }
            SQL.New(SoftwarePackageVO.class)
                    .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid)
                    .set(SoftwarePackageVO_.status, Upgraded.toString())
                    .update();
            SoftwarePackageVO vo = databases.findByUuid(msgData.softwarePackageUuid, SoftwarePackageVO.class);
            valueCompletion.success(SoftwarePackageInventory.valueOf(vo));
        })
        .error(errorCode -> {
            SoftwarePackageStatus failStatus = context.upgradePackageUploaded
                    ? UpgradeExecuteFailed
                    : UpgradePackageUploadFailed;
            SQL.New(SoftwarePackageVO.class)
                    .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid)
                    .set(SoftwarePackageVO_.status, failStatus.toString())
                    .update();
            valueCompletion.fail(errorCode);
        })
        .start();
    }

    private void reUploadAndExecuteSoftwareUpgradePackage(UploadSoftwarePackageToBackupStorageLongJobData msgData, ReturnValueCompletion<SoftwarePackageInventory> valueCompletion) {
        SimpleFlowChain.of("re-upload-software-upgrade-package")
        .then(Flow.of("update-software-package-status-to-upgrading")
            .handle(trigger -> {
                SQL.New(SoftwarePackageVO.class)
                        .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid)
                        .set(SoftwarePackageVO_.status, SoftwarePackageStatus.Upgrading.toString())
                        .update();
                trigger.next();
            })
            .build())
        .then(Flow.of("upgrade-software-package")
            .handle(trigger -> {
                SoftwarePackageVO vo = Q.New(SoftwarePackageVO.class)
                        .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid)
                        .find();
                UploadSoftwarePackageToBackupStorageExtensionPoint ext = plugins.getExtensionFromMap(vo.getType(), UploadSoftwarePackageToBackupStorageExtensionPoint.class);
                if (ext == null) {
                    trigger.fail(err(UNSUPPORTED_SOFTWARE_TYPE,
                            "no UploadSoftwarePackageToBackupStorageExtensionPoint found for software package type: %s", vo.getType()));
                    return;
                }

                String upgradePackagePath = SoftwarePackageSystemTags.SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH
                        .getTokenByResourceUuid(msgData.softwarePackageUuid, SoftwarePackageSystemTags.SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH_TOKEN);
                if (StringUtils.isEmpty(upgradePackagePath)) {
                    trigger.fail(err(INVALID_INSTALL_PATH,
                            "upgrade package path not found for software package[uuid:%s], please re-upload the upgrade package before reexecute",
                            msgData.softwarePackageUuid));
                    return;
                }
                msgData.upgradePackagePath = upgradePackagePath;
                ext.upgradeSoftwarePackage(vo, msgData, new Completion(trigger) {
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
            .build())
        .propagateExceptionTo(valueCompletion)
        .done(() -> {
            SQL.New(SoftwarePackageVO.class)
                    .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid)
                    .set(SoftwarePackageVO_.status, Upgraded.toString())
                    .update();
            SoftwarePackageVO vo = databases.findByUuid(msgData.softwarePackageUuid, SoftwarePackageVO.class);
            valueCompletion.success(SoftwarePackageInventory.valueOf(vo));
        })
        .error(errorCode -> {
            SQL.New(SoftwarePackageVO.class)
                    .eq(SoftwarePackageVO_.uuid, msgData.softwarePackageUuid)
                    .set(SoftwarePackageVO_.status, UpgradeExecuteFailed.toString())
                    .update();
            valueCompletion.fail(errorCode);
        })
        .start();
    }

    private void doCleanUpgradePackage(String softwarePackageUuid, Completion completion) {
        SoftwarePackageVO softwarePackageVO = Q.New(SoftwarePackageVO.class).eq(SoftwarePackageVO_.uuid, softwarePackageUuid).find();
        if (softwarePackageVO == null) {
            completion.fail(err(SysErrors.RESOURCE_NOT_FOUND, "software package not found, uuid: %s", softwarePackageUuid));
            return;
        }

        String backupStorageUuid = SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID.getTokenByResourceUuid(
                softwarePackageUuid, SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN);
        String backupStorageHostUuid = SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID.getTokenByResourceUuid(
                softwarePackageUuid, SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN);
        doCleanUpgradePackage(softwarePackageVO, backupStorageUuid, backupStorageHostUuid,
                softwarePackageVO.getInstallPath(), softwarePackageVO.getUnzipInstallPath(), completion);
    }

    private void doCleanUpgradePackage(SoftwarePackageVO softwarePackageVO, String backupStorageUuid,
                                       String backupStorageHostUuid, String installPath,
                                       String unzipInstallPath, Completion completion) {
        UploadSoftwarePackageToBackupStorageExtensionPoint ext = plugins.getExtensionFromMap(softwarePackageVO.getType(), UploadSoftwarePackageToBackupStorageExtensionPoint.class);
        if (ext == null) {
            completion.fail(err(UNSUPPORTED_SOFTWARE_TYPE, "no UploadSoftwarePackageToBackupStorageExtensionPoint found for software package type: %s", softwarePackageVO.getType()));
            return;
        }
        ext.cleanUpgradeSoftwarePackage(softwarePackageVO, backupStorageUuid, backupStorageHostUuid,
                installPath, unzipInstallPath, completion);
    }

    private void handle(APIUploadSoftwarePackageToVmMsg msg) {
        threads.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "upload-software-package-to-vm";
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUploadSoftwarePackageToVmEvent event = new APIUploadSoftwarePackageToVmEvent(msg.getId());
                String uploadTaskUuid = Platform.getUuid();
                event.setUploadTaskUuid(uploadTaskUuid);
                UploadSoftwarePackageToVmMsg uploadMsg =
                        UploadSoftwarePackageToVmMsg.fromApiMessage(msg, uploadTaskUuid);
                ErrorCode targetError;
                try {
                    targetError = UploadSoftwarePackageToVmTargetChecker.refreshForUpload(
                            uploadMsg, getUploadSoftwarePackageToVmBackend(uploadMsg.getType()));
                } catch (OperationFailureException e) {
                    targetError = e.getErrorCode();
                }
                if (targetError != null) {
                    event.setError(targetError);
                    bus.publish(event);
                    chain.next();
                    return;
                }

                uploadSoftwarePackageToVm(uploadMsg, () -> false,
                        new ReturnValueCompletion<String>(chain, msg) {
                            @Override
                            public void success(String uploadUrl) {
                                event.setUploadUrl(uploadUrl);
                                bus.publish(event);
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                event.setError(errorCode);
                                bus.publish(event);
                                chain.next();
                            }
                        });
            }

            @Override
            public String getName() {
                return "upload-software-package-to-vm";
            }
        });
    }

    private void handle(UploadSoftwarePackageToVmMsg msg) {
        UploadSoftwarePackageToVmReply reply = new UploadSoftwarePackageToVmReply();
        uploadSoftwarePackageToVm(msg, () -> jobCanceled(msg.getUploadTaskUuid()),
                new ReturnValueCompletion<String>(msg) {
                    @Override
                    public void success(String uploadUrl) {
                        reply.setUploadUrl(uploadUrl);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
    }

    private void uploadSoftwarePackageToVm(UploadSoftwarePackageToVmMsg msg,
                                           BooleanSupplier canceled,
                                           ReturnValueCompletion<String> completion) {
        class UploadContext {
            String hostUuid;
            String targetIp;
            String hostPath;
            String uploadUrl;
            UploadSoftwarePackageToVmSpec spec;
            UploadSoftwarePackageToVmBackend backend;
        }

        String uploadTaskUuid = msg.getUploadTaskUuid();
        UploadContext context = new UploadContext();
        SimpleFlowChain.of("upload-software-package-to-vm")
        .then(Flow.of("prepare-software-package-upload-to-vm")
            .handle(trigger -> {
                try {
                    context.hostUuid = msg.getHostUuid();
                    context.targetIp = msg.getTargetIp();

                    URI uri = URI.create(msg.getUrl());
                    String path = uri.getPath();
                    String fileName = path != null && !path.isEmpty() && !path.endsWith("/")
                            ? path.substring(path.lastIndexOf('/') + 1) : uri.getAuthority();
                    context.hostPath = String.format(
                            "/var/lib/zstack/software-package/vm-upload/%s/%s",
                            uploadTaskUuid, fileName);
                    context.backend = getUploadSoftwarePackageToVmBackend(msg.getType());
                    context.spec = context.backend.getUploadSpec(uploadTaskUuid);
                    trigger.next();
                } catch (OperationFailureException e) {
                    trigger.fail(e.getErrorCode());
                } catch (RuntimeException e) {
                    trigger.fail(err(GENERAL_ERROR,
                            "failed to prepare software package upload to VM[uuid:%s]: %s",
                            msg.getVmInstanceUuid(), e.getMessage()));
                }
            })
            .build())
        .then(Flow.of("upload-software-package-to-host")
            .handle(trigger -> {
                if (canceled.getAsBoolean()) {
                    trigger.fail(err(GENERAL_ERROR,
                            "software package upload to VM[uuid:%s] was canceled",
                            msg.getVmInstanceUuid()));
                    return;
                }

                UploadFileToHostMsg hostMsg = new UploadFileToHostMsg();
                hostMsg.setHostUuid(context.hostUuid);
                hostMsg.setTaskUuid(uploadTaskUuid);
                hostMsg.setUrl(msg.getUrl());
                hostMsg.setInstallPath(context.hostPath);
                bus.makeTargetServiceIdByResourceUuid(hostMsg, HostConstant.SERVICE_ID, context.hostUuid);
                bus.send(hostMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        UploadFileToHostReply hostReply = reply.castReply();
                        if (canceled.getAsBoolean()) {
                            trigger.fail(err(GENERAL_ERROR,
                                    "software package upload to VM[uuid:%s] was canceled",
                                    msg.getVmInstanceUuid()));
                            return;
                        }
                        if (msg.needTrack()) {
                            context.uploadUrl = hostReply.getDirectUploadUrl();
                            new UploadSoftwarePackageToVmTracker().runTrackTask(
                                    msg, context.hostPath, context.spec, context.backend, canceled);
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .then(Flow.of("copy-software-package-to-vm")
            .skipIf(data -> msg.needTrack())
            .handle(trigger -> {
                if (canceled.getAsBoolean()) {
                    trigger.fail(err(GENERAL_ERROR,
                            "software package upload to VM[uuid:%s] was canceled",
                            msg.getVmInstanceUuid()));
                    return;
                }

                ErrorCode targetError = UploadSoftwarePackageToVmTargetChecker.refreshBeforeCopy(
                        msg, context.backend);
                if (targetError != null) {
                    trigger.fail(targetError);
                    return;
                }
                context.targetIp = msg.getTargetIp();

                UploadFileToVmMsg vmMsg = new UploadFileToVmMsg();
                vmMsg.setHostUuid(context.hostUuid);
                vmMsg.setTaskUuid(uploadTaskUuid);
                vmMsg.setSourcePath(context.hostPath);
                vmMsg.setTargetIp(context.targetIp);
                vmMsg.setTargetPath(context.spec.getTargetPath());
                vmMsg.setUsername(context.spec.getUsername());
                vmMsg.setSshPort(context.spec.getSshPort());
                vmMsg.setPassword(context.spec.getPassword());
                bus.makeTargetServiceIdByResourceUuid(vmMsg, HostConstant.SERVICE_ID, context.hostUuid);
                bus.send(vmMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .then(Flow.of("install-software-package-on-vm")
            .skipIf(data -> msg.needTrack())
            .handle(trigger -> {
                if (canceled.getAsBoolean()) {
                    trigger.fail(err(GENERAL_ERROR,
                            "software package upload to VM[uuid:%s] was canceled",
                            msg.getVmInstanceUuid()));
                    return;
                }

                try {
                    context.backend.install(msg.getVmInstanceUuid(), context.targetIp, uploadTaskUuid, canceled,
                            new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                } catch (RuntimeException e) {
                    trigger.fail(err(GENERAL_ERROR,
                            "failed to install software package on VM[uuid:%s]: %s",
                            msg.getVmInstanceUuid(), e.getMessage()));
                }
            })
            .build())
        .propagateExceptionTo(completion)
        .done(() -> {
            if (msg.needTrack() || msg.isDeferCleanupToLongJob()) {
                completion.success(context.uploadUrl);
                return;
            }
            new UploadSoftwarePackageToVmCleanup().cleanup(
                    context.hostUuid, uploadTaskUuid, new NoErrorCompletion(completion) {
                        @Override
                        public void done() {
                            completion.success(context.uploadUrl);
                        }
                    });
        })
        .error(errorCode -> {
            logger.warn(String.format(
                    "failed to upload software package task[uuid:%s] to VM[uuid:%s]: %s",
                    uploadTaskUuid, msg.getVmInstanceUuid(), errorCode.getReadableDetails()));
            new UploadSoftwarePackageToVmCleanup().cleanup(
                    context.hostUuid, uploadTaskUuid, new NoErrorCompletion(completion) {
                        @Override
                        public void done() {
                            completion.fail(errorCode);
                        }
                    });
        })
        .start();
    }

    UploadSoftwarePackageToVmBackend getUploadSoftwarePackageToVmBackend(String type) {
        UploadSoftwarePackageToVmBackend backend = plugins.getExtensionFromMap(
                type, UploadSoftwarePackageToVmBackend.class);
        if (backend == null) {
            throw new OperationFailureException(err(UNSUPPORTED_SOFTWARE_TYPE,
                    "cannot find UploadSoftwarePackageToVmBackend for type[%s]", type));
        }
        return backend;
    }

    // ------------------------------------- Utility Handers ------------------------------------

    @SuppressWarnings("unchecked")
    private void createUploadConfig(SoftwarePackageVO vo) {
        SystemTagCreator creator = SoftwarePackageSystemTags.SOFTWARE_PACKAGE_UPLOAD_CONFIG.newSystemTagCreator(vo.getUuid());
        creator.inherent = false;
        creator.recreate = true;
        creator.setTagByTokens(map(e(SoftwarePackageSystemTags.SOFTWARE_PACKAGE_UPLOAD_CONFIG_TOKEN,
                Base64.getEncoder().encodeToString(JSONObjectUtil.toJsonString(vo).getBytes(StandardCharsets.UTF_8)))));
        creator.create();
    }

    private long getEstimatedImageSize(String softwarePackageType) {
        EstimatedImageSizeExtensionPoint ext = plugins.getExtensionFromMap(
                softwarePackageType, EstimatedImageSizeExtensionPoint.class);
        return ext != null ? ext.getEstimatedImageTotalSize() : 0;
    }

    @SuppressWarnings("unchecked")
    private void restoreBackupStorageTag(org.zstack.tag.PatternedSystemTag tag, String softwarePackageUuid,
                                         String value, String tokenName) {
        tag.delete(softwarePackageUuid);
        if (value == null) {
            return;
        }

        SystemTagCreator creator = tag.newSystemTagCreator(softwarePackageUuid);
        creator.inherent = false;
        creator.recreate = true;
        creator.setTagByTokens(map(e(tokenName, value)));
        creator.create();
    }
}
