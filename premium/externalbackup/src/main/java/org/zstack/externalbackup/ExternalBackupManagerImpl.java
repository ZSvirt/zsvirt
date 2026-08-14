package org.zstack.externalbackup;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.progress.TaskTracker;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.longjob.LongJobUtils;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.externalbackup.ExternalBackupRecoverTracker.PROCESS;
import static org.zstack.externalbackup.ExternalBackupRecoverTracker.RESOURCE_TYPE;

/**
 * Created by MaJin on 2019/12/3.
 */

public class ExternalBackupManagerImpl extends AbstractService implements ExternalBackupManager, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ExternalBackupManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;

    private Map<String, ExternalBackupFactory> factories = new HashMap<>();

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof ExternalBackupMessage) {
            passThrough((ExternalBackupMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(ExternalBackupMessage msg) {
        ExternalBackup externalBackup = getExternalBackup(msg.getExternalBackupUuid());
        if (externalBackup == null) {
            bus.replyErrorByMessageType((Message) msg, err(SysErrors.RESOURCE_NOT_FOUND,
                    "Cannot find external backup[uuid:%s], it may have been deleted", msg.getExternalBackupUuid()));
            return;
        }

        externalBackup.handleMessage((Message) msg);
    }

    private ExternalBackup getExternalBackup(String uuid) {
        return getExternalBackup(uuid, null);
    }

    private ExternalBackup getExternalBackup(String uuid, Consumer<ExternalBackupVO> consumer) {
        ExternalBackupVO vo = dbf.findByUuid(uuid, ExternalBackupVO.class);
        if (vo == null) {
            return null;
        }

        if (consumer != null) {
            consumer.accept(vo);
            vo = dbf.updateAndRefresh(vo);
        }

        ExternalBackupFactory factory = factories.get(vo.getType());
        ExternalBackupVO finalVo = vo;
        return Platform.New(()-> factory.getExternalBackup(finalVo));
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateExternalBackupMsg) {
            handle((APICreateExternalBackupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateExternalBackupMsg) {
            handle((CreateExternalBackupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ExternalBackupConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        factories = pluginRgty.getExtensionList(ExternalBackupFactory.class).stream()
                .collect(Collectors.toMap(ExternalBackupFactory::getBackupType, it -> it));
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    private void handle(APICreateExternalBackupMsg msg) {
        CreateExternalBackupMsg cmsg = msg.toLocalMessage();
        bus.makeLocalServiceId(cmsg, ExternalBackupConstants.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APICreateExternalBackupEvent event = new APICreateExternalBackupEvent(msg.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                } else {
                    event.setInventory(((CreateExternalBackupReply) reply).getInventory());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(CreateExternalBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                createExternalBackup(msg, chain);
            }

            @Override
            public String getName() {
                return msg.isDryRun() ? "external-dryrun-backup" : "external-backup";
            }
        });
    }

    private void createExternalBackup(CreateExternalBackupMsg msg, SyncTaskChain taskChain) {
        CreateExternalBackupReply reply = new CreateExternalBackupReply();

        ExternalBackupFactory factory = factories.get(msg.getType());
        FlowChain chain = factory.getCreateBackupFlowChain();
        chain.enableProgressReport();

        Map data = new HashMap();
        ExternalBackupVO backup;

        if (msg.getSpec() == null) {
            ExternalBackupVO originBackup = new ExternalBackupVO();
            originBackup.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
            originBackup.setName(msg.getName());
            originBackup.setDescription(msg.getDescription());
            originBackup.setVersion(dbf.getDbVersion());
            originBackup.setState(ExternalBackupState.Creating);
            backup = factory.createExternalBackup(originBackup, msg);
            msg.setSpec(factory.createBackupSpec(backup, msg));
        } else {
            backup = factory.getEntity(msg.getSpec().getBackupUuid());
        }

        data.put(ExternalBackupConstants.EXTERNAL_BACKUP_SPEC, msg.getSpec());
        data.put(ExternalBackupConstants.EXTERNAL_BACKUP_MSG, msg);
        chain.setData(data);
        chain.preCheck(it -> {
            ExternalBackupContextReloader.saveContext((CreateExternalBackupMsg)it.get(ExternalBackupConstants.EXTERNAL_BACKUP_MSG));
            return LongJobUtils.buildErrIfCanceled();
        });
        chain.done(new FlowDoneHandler(taskChain) {
            @Override
            public void handle(Map data) {
                ExternalBackupInventory inv = factory.getInventory(completeBackup(data));
                reply.setInventory(inv);
                bus.reply(msg, reply);
                taskChain.next();
            }

            private ExternalBackupVO completeBackup(Map data) {
                ExternalBackupSpec spec = (ExternalBackupSpec) data.get(ExternalBackupConstants.EXTERNAL_BACKUP_SPEC);

                ExternalBackupVO newestBackup = dbf.reload(backup);
                newestBackup.setState(ExternalBackupState.Ready);
                newestBackup.setTotalSize(spec.getTotalSize());
                newestBackup.setInstallPath(spec.getBackupInstallPath());
                newestBackup = dbf.updateAndRefresh(newestBackup);

                if (msg.isDryRun()) {
                    dbf.remove(newestBackup);
                }
                return newestBackup;
            }
        }).error(new FlowErrorHandler(taskChain) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                if (!msg.allowResume() || errCode.isError(SysErrors.INTERNAL) || LongJobUtils.jobCanceled()) {
                    dbf.remove(backup);
                }

                reply.setError(errCode);
                bus.reply(msg, reply);
                taskChain.next();
            }
        }).start();
    }

    @Override
    public void managementNodeReady() {
        String backupUuid = ExternalBackupHelper.getLabelBackupUuid();
        if (backupUuid == null) {
            return;
        }

        logger.info(String.format("start to recover from external backup[uuid:%s]", backupUuid));

        ExternalBackup extBackup = getExternalBackup(backupUuid, vo -> vo.setState(ExternalBackupState.InUse));
        if (extBackup == null) {
            return;
        }

        ExternalBackupRecoverTracker tracker = new ExternalBackupRecoverTracker(backupUuid);
        trackProgress();
        tracker.start();
        MevocoGlobalConfig.PAUSE_THE_WORLD.updateValue(true);
        extBackup.recoverHook(new Completion(null) {
            @Override
            public void success() {
                SQL.New(ExternalBackupVO.class).eq(ExternalBackupVO_.uuid, backupUuid)
                        .set(ExternalBackupVO_.state, ExternalBackupState.Ready)
                        .update();
                MevocoGlobalConfig.PAUSE_THE_WORLD.updateValue(false);
                ExternalBackupHelper.deleteBackupLabel();
                tracker.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                tracker.fail(errorCode);
            }
        });
    }

    private void trackProgress() {
        String path = PathUtil.join(System.getProperty("java.io.tmpdir"), "RecoverExternalBackup.log");
        PathUtil.forceRemoveFile(path);

        File tmpJobFile = new File(path);
        try {
            if (!tmpJobFile.createNewFile()) {
                logger.warn(String.format("fail to create new File[%s]", tmpJobFile));
            }
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }

        try (FileOutputStream outputStream = new FileOutputStream(tmpJobFile, true)) {
            TaskTracker.registerConsumer(ExternalBackupRecoverTracker.TASK_NAME, task -> {
                logger.debug(String.format("[recover backup: %s] %s", task.resourceUuid, task.info));
                try {
                    outputStream.write((task.info + "\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (IOException e) {
                    logger.warn("fail to write progress", e);
                }

                ExternalBackupRecoverTracker.Process process = (ExternalBackupRecoverTracker.Process) task.parameters.get(PROCESS);
                if (task.parameters.get(RESOURCE_TYPE) == null && (
                        process == ExternalBackupRecoverTracker.Process.success ||
                                process == ExternalBackupRecoverTracker.Process.failure)) {
                    try {
                        outputStream.write(("EOF_" + process).getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ignore) {}
                }
            });
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
