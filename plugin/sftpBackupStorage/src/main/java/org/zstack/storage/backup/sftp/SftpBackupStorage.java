package org.zstack.storage.backup.sftp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.agent.AgentConstant;
import org.zstack.core.ansible.*;
import org.zstack.core.cloudbus.CloudBusGlobalProperty;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.SyncThread;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.storage.backup.BackupStoragePathMaker;
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;

import javax.persistence.Query;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.zstack.core.Platform.operr;
import static org.zstack.header.storage.backup.BackupStorageConstant.RESTORE_IMAGES_BACKUP_STORAGE_METADATA_TO_DATABASE;

public class SftpBackupStorage extends BackupStorageBase {
    private static final CLogger logger = Utils.getLogger(SftpBackupStorage.class);

    @Autowired
    protected RESTFacade restf;
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutManager;
    @Autowired
    private SftpBackupStorageMetaDataMaker metaDataMaker;

    private String agentPackageName = SftpBackupStorageGlobalProperty.AGENT_PACKAGE_NAME;

    public SftpBackupStorage() {
    }

    public SftpBackupStorage(SftpBackupStorageVO vo) {
        super(vo);
    }


    public String buildUrl(String subPath) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(SftpBackupStorageGlobalProperty.AGENT_URL_SCHEME);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
        } else {
            ub.host(getSelf().getHostname());
        }

        ub.port(SftpBackupStorageGlobalProperty.AGENT_PORT);
        if (!"".equals(SftpBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(SftpBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(subPath);
        return ub.build().toUriString();
    }

    private SftpBackupStorageVO getSelf() {
        return (SftpBackupStorageVO) self;
    }

    protected BackupStorageInventory getSelfInventory() {
        return SftpBackupStorageInventory.valueOf(getSelf());
    }

    private class DownloadResult {
        String md5sum;
        long size;
        long actualSize;
        String format;
    }

    private void download(String url, String installPath, String uuid, final ReturnValueCompletion<DownloadResult> completion) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (!SftpBackupStorageFactory.type.getSupportedSchemes().contains(scheme)) {
                throw new OperationFailureException(operr("SftpBackupStorage doesn't support scheme[%s] in url[%s]", scheme, url));
            }

            DownloadCmd cmd = new DownloadCmd();
            cmd.setUuid(self.getUuid());
            cmd.setImageUuid(uuid);
            cmd.setUrl(url);
            cmd.setUrlScheme(scheme);
            cmd.setInstallPath(installPath);
            cmd.setTimeout(timeoutManager.getTimeout());

            restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH), cmd, new JsonAsyncRESTCallback<DownloadResponse>(completion) {
                @Override
                public void fail(ErrorCode err) {
                    completion.fail(err);
                }

                @Override
                public void success(DownloadResponse ret) {
                    updateCapacity(ret.getTotalCapacity(), ret.getAvailableCapacity());
                    if (ret.isSuccess()) {
                        DownloadResult res = new DownloadResult();
                        res.md5sum = ret.getMd5Sum();
                        res.size = ret.getSize();
                        res.actualSize = ret.getActualSize();
                        res.format = ret.format;
                        completion.success(res);
                    } else {
                        completion.fail(
                                operr("failed to download image[url: %s] on backup storage[uuid: %s], because: %s",
                                        url, self.getUuid(), ret.getError())
                        );
                    }
                }

                @Override
                public Class<DownloadResponse> getReturnClass() {
                    return DownloadResponse.class;
                }
            });
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    protected void handle(final GetImageSizeOnBackupStorageMsg msg) {
        final GetImageSizeOnBackupStorageReply reply = new GetImageSizeOnBackupStorageReply();

        GetImageSizeCmd cmd = new GetImageSizeCmd();
        cmd.uuid = self.getUuid();
        cmd.imageUuid = msg.getImageUuid();
        cmd.installPath = msg.getImageUrl();

        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.GET_IMAGE_SIZE), cmd,
                new JsonAsyncRESTCallback<GetImageSizeRsp>(msg) {
                    @Override
                    public void fail(ErrorCode err) {
                        reply.setError(err);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void success(GetImageSizeRsp rsp) {
                        if (!rsp.isSuccess()) {
                            reply.setError(operr("operation error, because:%s", rsp.getError()));
                        } else {
                            reply.setSize(rsp.size);
                        }

                        bus.reply(msg, reply);
                    }

                    @Override
                    public Class<GetImageSizeRsp> getReturnClass() {
                        return GetImageSizeRsp.class;
                    }
                });
    }

    @Override
    @Transactional
    protected void handle(final DownloadImageMsg msg) {
        final DownloadImageReply reply = new DownloadImageReply();
        final ImageInventory iinv = msg.getImageInventory();
        final String installPath = PathUtil.join(getSelf().getUrl(), BackupStoragePathMaker.makeImageInstallPath(iinv));

        String sql = "update ImageBackupStorageRefVO set installPath = :installPath " +
                "where backupStorageUuid = :bsUuid and imageUuid = :imageUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("installPath", installPath);
        q.setParameter("bsUuid", msg.getBackupStorageUuid());
        q.setParameter("imageUuid", msg.getImageInventory().getUuid());
        q.executeUpdate();

        download(iinv.getUrl(), installPath, iinv.getUuid(), new ReturnValueCompletion<DownloadResult>(msg) {
            @Override
            public void success(DownloadResult res) {
                reply.setInstallPath(installPath);
                reply.setSize(res.size);
                reply.setActualSize(res.actualSize);
                reply.setMd5sum(res.md5sum);
                reply.setFormat(res.format);
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
    protected void handle(final CancelDownloadImageMsg msg) {
        CancelDownloadImageReply reply = new CancelDownloadImageReply();

        CancelCommand cmd = new CancelCommand();
        cmd.setCancellationApiId(msg.getCancellationApiId());
        cmd.setAllowTaskNotFound(msg.isAllowTaskNotFound());

        restf.asyncJsonPost(buildUrl(AgentConstant.CANCEL_JOB), cmd, new JsonAsyncRESTCallback<CancelResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CancelResponse rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("fail to cancel download image, because %s", rsp.getError()));
                } else {
                    reply.setCancelResult(rsp.getCancelResult());
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<CancelResponse> getReturnClass() {
                return CancelResponse.class;
            }
        });
    }

    @Override
    protected void handle(final DownloadVolumeMsg msg) {
        final DownloadVolumeReply reply = new DownloadVolumeReply();
        final String installPath = PathUtil.join(getSelf().getUrl(), BackupStoragePathMaker.makeVolumeInstallPath(msg.getUrl(), msg.getVolume()));
        download(msg.getUrl(), installPath, msg.getVolume().getUuid(), new ReturnValueCompletion<DownloadResult>(msg) {
            @Override
            public void success(DownloadResult res) {
                reply.setInstallPath(installPath);
                reply.setSize(res.size);
                reply.setMd5sum(res.md5sum);
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
    protected void connectHook(boolean newAdded, Completion completion) {
        connect(new Completion(completion) {
            @Override
            public void success() {
               if (!newAdded) {
                   String backupStorageUrl = getSelf().getUrl();
                   String backStorageHostName = getSelf().getHostname();
                   String backupStorageUuid = getSelf().getUuid();
                   SftpBackupStorageDumpMetadataInfo dumpInfo = new SftpBackupStorageDumpMetadataInfo();
                   dumpInfo.setDumpAllInfo(true);
                   dumpInfo.setBackupStorageUuid(backupStorageUuid);
                   dumpInfo.setBackupStorageUrl(backupStorageUrl);
                   dumpInfo.setBackupStorageHostname(backStorageHostName);

                   metaDataMaker.dumpImagesBackupStorageInfoToMetaDataFile(dumpInfo);
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
    protected void pingHook(final Completion completion) {
        final PingCmd cmd = new PingCmd();
        cmd.uuid = self.getUuid();
        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.PING_PATH), cmd, new JsonAsyncRESTCallback<PingResponse>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(PingResponse ret) {
                if (ret.isSuccess() && !self.getUuid().equals(ret.getUuid())) {
                    ErrorCode err = operr("the uuid of sftpBackupStorage agent changed[expected:%s, actual:%s], it's most likely" +
                            " the agent was manually restarted. Issue a reconnect to sync the status", self.getUuid(), ret.getUuid());

                    completion.fail(err);
                } else if (ret.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(operr("operation error, because:%s", ret.getError()));
                }
            }

            @Override
            public Class<PingResponse> getReturnClass() {
                return PingResponse.class;
            }
        });
    }

    private void continueConnect(final Completion complete) {
        restf.echo(buildUrl(SftpBackupStorageConstant.ECHO_PATH), new Completion(complete) {
            @Override
            public void success() {
                String url = buildUrl(SftpBackupStorageConstant.CONNECT_PATH);
                ConnectCmd cmd = new ConnectCmd();
                cmd.setUuid(self.getUuid());
                cmd.setStoragePath(getSelf().getUrl());
                cmd.setSendCommandUrl(restf.getSendCommandUrl());
                ConnectResponse rsp = restf.syncJsonPost(url, cmd, ConnectResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("unable to connect to SimpleHttpBackupStorage[url:%s], because %s", url, rsp.getError());
                    complete.fail(err);
                    return;
                }

                updateCapacity(rsp.getTotalCapacity(), rsp.getAvailableCapacity());
                logger.debug(String.format("connected to backup storage[uuid:%s, name:%s, total capacity:%sG, available capacity: %sG",
                        getSelf().getUuid(), getSelf().getName(), rsp.getTotalCapacity(), rsp.getAvailableCapacity()));
                complete.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }

    private void connect(final Completion complete) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            continueConnect(complete);
            return;
        }

        SshFileMd5Checker checker = new SshFileMd5Checker();
        checker.setTargetIp(getSelf().getHostname());
        checker.setUsername(getSelf().getUsername());
        checker.setPassword(getSelf().getPassword());
        checker.setSshPort(getSelf().getSshPort());
        checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/sftpbackupstorage/package/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
        checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/sftpbackupstorage/%s", agentPackageName), true).getAbsolutePath(),
                String.format("/var/lib/zstack/sftpbackupstorage/package/%s", agentPackageName));


        SshChronyConfigChecker chronyChecker = new SshChronyConfigChecker();
        chronyChecker.setTargetIp(getSelf().getHostname());
        chronyChecker.setUsername(getSelf().getUsername());
        chronyChecker.setPassword(getSelf().getPassword());
        chronyChecker.setSshPort(getSelf().getSshPort());

        SshYumRepoChecker repoChecker = new SshYumRepoChecker();
        repoChecker.setTargetIp(getSelf().getHostname());
        repoChecker.setUsername(getSelf().getUsername());
        repoChecker.setPassword(getSelf().getPassword());
        repoChecker.setSshPort(getSelf().getSshPort());

        CallBackNetworkChecker callbackChecker = new CallBackNetworkChecker();
        callbackChecker.setTargetIp(getSelf().getHostname());
        callbackChecker.setUsername(getSelf().getUsername());
        callbackChecker.setPassword(getSelf().getPassword());
        callbackChecker.setPort(getSelf().getSshPort());
        callbackChecker.setCallbackIp(Platform.getManagementServerIp());
        callbackChecker.setCallBackPort(CloudBusGlobalProperty.HTTP_PORT);

        AnsibleRunner runner = new AnsibleRunner();
        runner.installChecker(checker);
        runner.installChecker(chronyChecker);
        runner.installChecker(repoChecker);
        runner.installChecker(callbackChecker);
        runner.setPassword(getSelf().getPassword());
        runner.setUsername(getSelf().getUsername());
        runner.setTargetIp(getSelf().getHostname());
        runner.setTargetUuid(getSelf().getUuid());
        runner.setSshPort(getSelf().getSshPort());
        runner.setAgentPort(SftpBackupStorageGlobalProperty.AGENT_PORT);
        runner.setPlayBookName(SftpBackupStorageConstant.ANSIBLE_PLAYBOOK_NAME);
        runner.setDeployArguments(new SftpBackupStorageDeployArguments());
        runner.run(new ReturnValueCompletion<Boolean>(complete) {
            @Override
            public void success(Boolean deployed) {
                StringBuilder builder = new StringBuilder();
                if (!SftpBackupStorageGlobalProperty.MN_NETWORKS.isEmpty()) {
                    builder.append(String.format("sudo bash %s -m %s -p %s -c %s",
                            "/var/lib/zstack/sftpbackupstorage/package/sftp-iptables",
                            SftpBackupStorageConstant.IPTABLES_COMMENTS,
                            SftpBackupStorageGlobalConfig.SFTP_ALLOW_PORTS.value(),
                            String.join(",", SftpBackupStorageGlobalProperty.MN_NETWORKS)));
                } else {
                    builder.append(String.format("sudo bash %s -m %s -p %s",
                            "/var/lib/zstack/sftpbackupstorage/package/sftp-iptables",
                            SftpBackupStorageConstant.IPTABLES_COMMENTS,
                            SftpBackupStorageGlobalConfig.SFTP_ALLOW_PORTS.value()));
                }
                try {
                    new Ssh().shell(builder.toString())
                            .setUsername(getSelf().getUsername())
                            .setPassword(getSelf().getPassword())
                            .setHostname(getSelf().getHostname())
                            .setPort(getSelf().getSshPort()).runErrorByExceptionAndClose();
                } catch (SshException ex) {
                    throw new OperationFailureException(operr(ex.toString()));
                }

                continueConnect(complete);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }

    @Override
    public List<ImageInventory> scanImages() {
        return null;
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIReconnectSftpBackupStorageMsg) {
            handle((APIReconnectSftpBackupStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    @Override
    protected void handleLocalMessage(Message msg) throws URISyntaxException {
        if (msg instanceof GetSftpBackupStorageDownloadCredentialMsg) {
            handle((GetSftpBackupStorageDownloadCredentialMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    @Override
    protected void handle(final DeleteBitsOnBackupStorageMsg msg) {
        final DeleteBitsOnBackupStorageReply reply = new DeleteBitsOnBackupStorageReply();

        DeleteCmd cmd = new DeleteCmd();
        cmd.uuid = self.getUuid();
        cmd.setInstallUrl(msg.getInstallPath());
        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.DELETE_PATH), cmd, new JsonAsyncRESTCallback<DeleteResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteResponse ret) {
                if (!ret.isSuccess()) {
                    logger.warn(String.format("failed to delete bits[%s], schedule clean up, %s",
                            msg.getInstallPath(), ret.getError()));
                    //TODO GC
                } else {
                    updateCapacity(ret.getTotalCapacity(), ret.getAvailableCapacity());
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<DeleteResponse> getReturnClass() {
                return DeleteResponse.class;
            }
        });
    }

    @Override
    protected void handle(BackupStorageAskInstallPathMsg msg) {
        BackupStorageAskInstallPathReply reply = new BackupStorageAskInstallPathReply();
        String installPath = PathUtil.join(self.getUrl(), BackupStoragePathMaker.makeImageInstallPath(msg.getImageUuid(), msg.getImageMediaType()));
        reply.setInstallPath(installPath);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final SyncImageSizeOnBackupStorageMsg msg) {
        final SyncImageSizeOnBackupStorageReply reply = new SyncImageSizeOnBackupStorageReply();

        ImageInventory image = msg.getImage();
        GetImageSizeCmd cmd = new GetImageSizeCmd();
        cmd.imageUuid = image.getUuid();
        cmd.uuid = self.getUuid();

        ImageBackupStorageRefInventory ref = CollectionUtils.findOneOrNull(image.getBackupStorageRefs(),
                arg -> arg.getBackupStorageUuid().equals(self.getUuid()));

        if (ref == null) {
            throw new CloudRuntimeException(String.format("cannot find ImageBackupStorageRefInventory of image[uuid:%s] for the backup storage[uuid:%s]",
                    image.getUuid(), self.getUuid()));
        }

        cmd.installPath = ref.getInstallPath();
        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.GET_IMAGE_SIZE), cmd, new JsonAsyncRESTCallback<GetImageSizeRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(GetImageSizeRsp rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("operation error, because:%s", rsp.getError()));
                } else {
                    reply.setActualSize(rsp.actualSize);
                    reply.setSize(rsp.size);
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<GetImageSizeRsp> getReturnClass() {
                return GetImageSizeRsp.class;
            }
        });
    }

    @Override
    protected void handle(GetLocalFileSizeOnBackupStorageMsg msg) {
        GetLocalFileSizeOnBackupStorageReply reply = new GetLocalFileSizeOnBackupStorageReply();
        GetLocalFileSizeCmd cmd = new GetLocalFileSizeCmd();
        cmd.path = msg.getUrl();
        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.GET_LOCAL_FILE_SIZE), cmd,
                new JsonAsyncRESTCallback<GetLocalFileSizeRsp>(msg) {
                    @Override
                    public void fail(ErrorCode err) {
                        reply.setError(err);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void success(GetLocalFileSizeRsp rsp) {
                        if (!rsp.isSuccess()) {
                            reply.setError(operr("operation error, because:%s", rsp.getError()));
                        } else {
                            reply.setSize(rsp.size);
                        }

                        bus.reply(msg, reply);
                    }

                    @Override
                    public Class<GetLocalFileSizeRsp> getReturnClass() {
                        return GetLocalFileSizeRsp.class;
                    }
        });
    }

    @Override
    protected void handle(GetImageEncryptedOnBackupStorageMsg msg) {
        GetImageEncryptedOnBackupStorageReply reply = new GetImageEncryptedOnBackupStorageReply();
        String  installPath = Q.New(ImageBackupStorageRefVO.class)
                .select(ImageBackupStorageRefVO_.installPath)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, self.getUuid())
                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                .findValue();
        GetImageHashCmd cmd = new GetImageHashCmd();
        cmd.path = installPath;

        restf.asyncJsonPost(buildUrl(SftpBackupStorageConstant.GET_IMAGE_HASH), cmd, new JsonAsyncRESTCallback<GetImageHashRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(GetImageHashRsp rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("operation error, because:%s", rsp.getError()));
                } else {
                    reply.setEncrypted(rsp.hash);
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<GetImageHashRsp> getReturnClass() {
                return GetImageHashRsp.class;
            }
        });

    }

    private void handle(final GetSftpBackupStorageDownloadCredentialMsg msg) {
        final GetSftpBackupStorageDownloadCredentialReply reply = new GetSftpBackupStorageDownloadCredentialReply();

        String key = asf.getPrivateKey();
        reply.setHostname(getSelf().getHostname());
        reply.setUsername(getSelf().getUsername());
        reply.setSshKey(key);
        reply.setSshPort(getSelf().getSshPort());
        bus.reply(msg, reply);
    }

    private void handle(final APIReconnectSftpBackupStorageMsg msg) {
        final APIReconnectSftpBackupStorageEvent evt = new APIReconnectSftpBackupStorageEvent(msg.getId());
        connect(new Completion(msg) {
            @Override
            public void success() {
                changeStatus(BackupStorageStatus.Connected, new NoErrorCompletion(msg) {
                    @Override
                    public void done() {
                        self = dbf.reload(self);
                        evt.setInventory(SftpBackupStorageInventory.valueOf(getSelf()));
                        bus.publish(evt);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    @Override
    protected BackupStorageVO updateBackupStorage(APIUpdateBackupStorageMsg msg) {
        if (!(msg instanceof APIUpdateSftpBackupStorageMsg)) {
            return super.updateBackupStorage(msg);
        }

        SftpBackupStorageVO vo = (SftpBackupStorageVO) super.updateBackupStorage(msg);
        vo = vo == null ? getSelf() : vo;

        APIUpdateSftpBackupStorageMsg umsg = (APIUpdateSftpBackupStorageMsg) msg;
        if (umsg.getUsername() != null) {
            vo.setUsername(umsg.getUsername());
        }
        if (umsg.getPassword() != null) {
            vo.setPassword(umsg.getPassword());
        }
        if (umsg.getHostname() != null) {
            vo.setHostname(umsg.getHostname());
        }
        if (umsg.getSshPort() != null && umsg.getSshPort() > 0 && umsg.getSshPort() <= 65535) {
            vo.setSshPort(umsg.getSshPort());
        }

        return vo;
    }

    @Override
    protected void handle(RestoreImagesBackupStorageMetadataToDatabaseMsg msg) {
        RestoreImagesBackupStorageMetadataToDatabaseReply reply = new RestoreImagesBackupStorageMetadataToDatabaseReply();
        doRestoreImagesBackupStorageMetadataToDatabase(msg);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(CalculateImageHashOnBackupStorageMsg msg) {
        CalculateImageHashOnBackupStorageReply reply = new CalculateImageHashOnBackupStorageReply();
        reply.setError(operr("sftp backup storage do not support calculate image hash"));
    }

    protected void handle(GetBackupStorageManagerHostnameMsg msg) {
        GetBackupStorageManagerHostnameReply reply = new GetBackupStorageManagerHostnameReply();
        reply.setHostname(getSelf().getHostname());
        bus.reply(msg, reply);
    }

    @SyncThread(signature = RESTORE_IMAGES_BACKUP_STORAGE_METADATA_TO_DATABASE)
    private void doRestoreImagesBackupStorageMetadataToDatabase(RestoreImagesBackupStorageMetadataToDatabaseMsg msg) {
        metaDataMaker.restoreImagesBackupStorageMetadataToDatabase(msg.getImagesMetadata(), msg.getBackupStorageUuid());
    }
}
