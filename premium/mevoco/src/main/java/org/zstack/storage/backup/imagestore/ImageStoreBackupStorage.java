package org.zstack.storage.backup.imagestore;

import com.google.common.base.Joiner;
import lombok.Synchronized;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.*;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusGlobalProperty;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.*;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.*;
import org.zstack.header.imagestore.ImageStorageContinueConnectExtensionPoint;
import org.zstack.header.imagestore.ImageStoreReclaimSpaceExtensionPoint;
import org.zstack.header.imagestore.PullImageToLocalMsg;
import org.zstack.header.imagestore.PullImageToLocalReply;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.AsyncRESTCallback;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.identity.AccountManager;
import org.zstack.image.ImageSystemTags;
import org.zstack.longjob.LongJobGlobalConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.storage.backup.BackupStorageGlobalConfig;
import org.zstack.storage.backup.BackupStorageSystemTags;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageCommands.*;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.*;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.path.RemotePathValidator;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;

import javax.persistence.Query;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.header.Constants.THREAD_CONTEXT_TASK_NAME;
import static org.zstack.header.storage.backup.BackupStorageConstant.RESTORE_IMAGES_BACKUP_STORAGE_METADATA_TO_DATABASE;
import static org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant.VOLUME_BACKUP_PACKAGE_NAME;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class ImageStoreBackupStorage extends BackupStorageBase {
    private static final CLogger logger = Utils.getLogger(ImageStoreBackupStorage.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ImageStoreBackupStorageMetaDataMaker metaDataMaker;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private ApiTimeoutManager timeoutManager;

    private static final Map<String, Boolean> imageStoreGCTaskMap = new ConcurrentHashMap<>();
    private static Map<String, String> inodeMd5Sums = new HashMap<>();

    public ImageStoreBackupStorage() {
    }

    private static final String agentPackageName = ImageStoreBackupStorageGlobalProperty.AGENT_PACKAGE_NAME;

    private ImageStoreBackupStorageVO getSelf() {
        return (ImageStoreBackupStorageVO) self;
    }

    protected BackupStorageInventory getSelfInventory() {
        return ImageStoreBackupStorageInventory.valueOf(getSelf());
    }

    private String buildUrl(String subPath) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(ImageStoreBackupStorageGlobalProperty.AGENT_URL_SCHEME);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
        } else {
            ub.host(getSelf().getHostname());
        }

        ub.port(ImageStoreBackupStorageGlobalProperty.AGENT_PORT);
        if (!"".equals(ImageStoreBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(ImageStoreBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(subPath);
        return ub.build().toUriString();
    }

    private String buildRegistryCmd(String destIp) {
        StringBuilder buf = new StringBuilder();
        buf.append(ImageStoreBackupStorageGlobalProperty.REGISTRY_CMD);
        buf.append(" ");
        buf.append("-rootca");
        buf.append(" ");
        buf.append(PathUtil.getFilePathUnderZStackHomeFolder("imagestore/bin/certs/ca.pem"));
        buf.append(" ");
        buf.append("-url");
        buf.append(" ");
        buf.append(destIp);
        buf.append(":");
        buf.append(ImageStoreBackupStorageGlobalProperty.REGISTRY_PORT);
        buf.append(" ");
        buf.append("-json");
        buf.append(" ");
        return buf.toString();
    }

    static public String zstoreProto = "zstore://";

    // Notes about 'installPath'.
    //
    // This plugin stores the image information to 'installPath' with the schema below:
    //   zstore://namespace/image-id
    //
    // We use the `uuid' of the base image as the namespace - since the image store
    // backend has restrictions on image name (enforced by storage driver, e.g. 's3' etc.)
    //
    // The `image-id' is returned by the image store, after successfully imported a base image.

    static private String buildInstallPath(String name, String id) {
        //   zstore://namespace/image-id
        return String.format("%s%s/%s", zstoreProto, name, id);
    }

    static private String getNameFromInstallPath(String installPath) {
        return installPath.replace(zstoreProto, "").split("/")[0];
    }

    static private String getIdFromInstallPath(String installPath) {
        return installPath.replace(zstoreProto, "").split("/")[1];
    }

    static private boolean isZstore(final String url) {
        return url.startsWith(zstoreProto);
    }

    static private String timestampToTag(Timestamp timestamp) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
            Date d = format.parse(timestamp.toString());
            return format.format(d);
        } catch (Exception e) {
            return null;
        }
    }

    private String getImageOwner(String imageInventoryUuid) {
        return acntMgr.getOwnerAccountUuidOfResource(imageInventoryUuid);
    }

    public ImageStoreBackupStorage(ImageStoreBackupStorageVO vo) {
        super(vo);
    }

    @Override
    protected void connectHook(boolean newAdded, Completion completion) {
        connect(newAdded, new Completion(completion) {
            @Override
            public void success() {
                if (!newAdded) {
                    String backupStorageUrl = getSelf().getUrl();
                    String backStorageHostName = getSelf().getHostname();
                    String backupStorageUuid = getSelf().getUuid();
                    ImageStoreBackupStorageDumpMetadataInfo dumpInfo = new ImageStoreBackupStorageDumpMetadataInfo();
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

    private void continueConnect(boolean newAdded, final Completion completion) {
        SimpleFlowChain chain = new SimpleFlowChain();
        ImageStoreBackupStorageInventory inv = ImageStoreBackupStorageInventory.valueOf(getSelf());
        chain.setName("continue-connect-imagestore-" + inv.getUuid());
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                init(new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });
        pluginRgty.getExtensionList(ImageStorageContinueConnectExtensionPoint.class).forEach(it -> {
            List<Flow> flows = it.createImageStoreConnectingFlow(newAdded, inv);
            if (flows != null) {
                flows.forEach(chain::then);
            }
        });
        chain.error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).start();
    }

    private void init(final Completion complete) {
        restf.echo(buildUrl(ImageStoreBackupStorageConstant.ECHO_PATH), new Completion(complete) {
            @Override
            public void success() {
                String url = buildUrl(ImageStoreBackupStorageConstant.CONNECT_PATH);
                ConnectCmd cmd = new ConnectCmd();
                cmd.setUuid(self.getUuid());
                ConnectResponse rsp = restf.syncJsonPost(url, cmd, ConnectResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("unable to connect to SimpleHttpBackupStorage[url:%s], because %s",
                            url, rsp.getError());
                    complete.fail(err);
                    return;
                }

                updateCapacity(rsp.getTotalSize(), rsp.getFreeSize());
                updateExtraIps(rsp.getIpAddresses());
                updateFuseSystemTag(rsp.isSupportFuse());
                initIscsiInitiatorName(rsp.getIscsiInitiatorName());
                initStorageInfo(rsp.getStorageInfo());
                updateIORate(rsp.getIoRate());

                logger.debug(String.format("connected to backup storage[uuid:%s, name:%s, total capacity:%sG, available capacity: %sG",
                        getSelf().getUuid(), getSelf().getName(), rsp.getTotalSize(), rsp.getFreeSize()));
                complete.success();
            }

            private void updateIORate(int ioRate) {
                SystemTagCreator creator = ImageStoreSystemTags.IO_RATE.newSystemTagCreator(self.getUuid());
                creator.setTagByTokens(map(e(ImageStoreSystemTags.IO_RATE_TOKEN, ioRate)));
                creator.inherent = false;
                creator.recreate = true;
                creator.create();
            }

            private void updateFuseSystemTag(boolean support) {
                if (support && !ImageStoreSystemTags.SUPPORT_FUSE.hasTag(self.getUuid())) {
                    SystemTagCreator creator = ImageStoreSystemTags.SUPPORT_FUSE.newSystemTagCreator(self.getUuid());
                    creator.recreate = true;
                    creator.create();
                } else if (!support && ImageStoreSystemTags.SUPPORT_FUSE.hasTag(self.getUuid())) {
                    ImageStoreSystemTags.SUPPORT_FUSE.delete(self.getUuid());
                }
            }

            private void updateExtraIps(List<String> ips) {
                if (ips != null) {
                    ips.remove(getSelf().getHostname());
                    ips.remove("127.0.0.1");
                    if (!ips.isEmpty()) {
                        SystemTagCreator creator = BackupStorageSystemTags.EXTRA_IPS.newSystemTagCreator(self.getUuid());
                        Optional.ofNullable(BackupStorageSystemTags.EXTRA_IPS_TOKEN)
                                .ifPresent(it -> creator.setTagByTokens(Collections.singletonMap(BackupStorageSystemTags.EXTRA_IPS_TOKEN, Joiner.on(",").join(ips))));
                        creator.inherent = false;
                        creator.recreate = true;
                        creator.create();
                    } else {
                        BackupStorageSystemTags.EXTRA_IPS.delete(self.getUuid());
                    }
                }
            }

            private void initStorageInfo(StorageInfo storageInfo) {
                if (storageInfo == null) {
                    return;
                }

                SystemTagCreator creator = ImageStoreSystemTags.STORAGE_INFO.newSystemTagCreator(self.getUuid());
                creator.setTagByTokens(map(
                        e(ImageStoreSystemTags.FS_TYPE_TOKEN, storageInfo.getType()),
                        e(ImageStoreSystemTags.URL_TOKEN, storageInfo.getUrl()),
                        e(ImageStoreSystemTags.OPTION_TOKEN, storageInfo.getOptions())
                ));
                creator.inherent = false;
                creator.ignoreIfExisting = true;
                creator.create();
            }

            private void initIscsiInitiatorName(String iscsi) {
                if (iscsi == null) {
                    return;
                }

                SystemTagCreator creator = BackupStorageSystemTags.ISCSI_INITIATOR_NAME.newSystemTagCreator(self.getUuid());
                creator.setTagByTokens(Collections.singletonMap(BackupStorageSystemTags.ISCSI_INITIATOR_NAME_TOKEN, iscsi));
                creator.inherent = false;
                creator.recreate = true;
                creator.create();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }

    private void connect(final Completion complete) {
        connect(false, complete);
    }

    private void connect(boolean newAdded, Completion complete) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            continueConnect(newAdded, complete);
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("deploy imagestore backup storage");
        chain.then(new NoRollbackFlow() {
            String __name__ = "get-imagestore-agent-package-inode-and-hash";

            final Map<String, String> currentInodeMd5Sums = new HashMap<>();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                File folder = PathUtil.findFolderOnClassPath("ansible/imagestorebackupstorage/", false);
                if (folder == null) {
                    inodeMd5Sums = new HashMap<>();
                    trigger.next();
                    return;
                }
                File[] storeBinFiles = folder.listFiles((dir, name) -> name.endsWith(".bin"));
                if (storeBinFiles == null) {
                    inodeMd5Sums = new HashMap<>();
                    trigger.next();
                    return;
                }

                for (File file : storeBinFiles) {
                    setCurrentInodeMd5Sums(file.getAbsolutePath());
                }
                inodeMd5Sums = currentInodeMd5Sums;
                trigger.next();
            }

            @ExceptionSafe
            private void setCurrentInodeMd5Sums(String filePath) {
                String inode = PathUtil.getFileInode(filePath).toString();
                String lastModifiedTime = Long.toString(PathUtil.getFileLastModifiedTime(filePath).toMillis());
                String inodeLastModifiedTime = String.format("%s-%s", inode, lastModifiedTime);

                String md5sum;
                if (inodeMd5Sums.containsKey(inodeLastModifiedTime)) {
                    md5sum = inodeMd5Sums.get(inodeLastModifiedTime);
                } else {
                    md5sum = SHAUtils.getFileMd5sum(filePath);
                }
                currentInodeMd5Sums.put(inodeLastModifiedTime, md5sum);
            }

        }).then(new NoRollbackFlow() {
            String __name__ = "deploy-imagestore-agent";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SshFilesMd5Checker checker = new SshFilesMd5Checker();
                checker.setIp(getSelf().getHostname());
                checker.setUsername(getSelf().getUsername());
                checker.setPassword(getSelf().getPassword());
                checker.setSshPort(getSelf().getSshPort());
                checker.setFileMd5sums(new ArrayList<>(inodeMd5Sums.values()));
                checker.setFilePath(String.format("/var/lib/zstack/imagestorebackupstorage/package/%s", agentPackageName));

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

                String quota = rcf.getResourceConfigValue(ImageStoreGlobalConfig.MAX_CAPACITY, self.getUuid(), String.class);
                SshYamlChecker yamlChecker = new SshYamlChecker();
                yamlChecker.setTargetIp(getSelf().getHostname());
                yamlChecker.setUsername(getSelf().getUsername());
                yamlChecker.setPassword(getSelf().getPassword());
                yamlChecker.setSshPort(getSelf().getSshPort());
                yamlChecker.expectConfig("quota", quota);
                yamlChecker.setYamlFilePath("/usr/local/zstack/imagestore/bin/zstore.yaml");

                AnsibleRunner runner = new AnsibleRunner();
                runner.installChecker(checker);
                runner.installChecker(chronyChecker);
                runner.installChecker(repoChecker);
                runner.installChecker(callbackChecker);
                runner.installChecker(yamlChecker);
                runner.setPassword(getSelf().getPassword());
                runner.setUsername(getSelf().getUsername());
                runner.setTargetIp(getSelf().getHostname());
                runner.setTargetUuid(getSelf().getUuid());
                runner.setSshPort(getSelf().getSshPort());
                runner.setAgentPort(ImageStoreBackupStorageGlobalProperty.AGENT_PORT);
                runner.setPlayBookName(ImageStoreBackupStorageConstant.ANSIBLE_PLAYBOOK_NAME);

                ImageStoreAgentDeployArguments deployArguments = new ImageStoreAgentDeployArguments();
                deployArguments.setFsRootPath(getSelf().getUrl());
                deployArguments.setNewAdd(String.valueOf(newAdded));
                deployArguments.setMaxCapacity(quota);
                if (ImageStoreBackupStorageSelector.isRemote(self.getUuid())) {
                    deployArguments.setZstackRepo("false");
                }
                runner.setDeployArguments(deployArguments);
                runner.setFullDeploy(newAdded);
                runner.run(new ReturnValueCompletion<Boolean>(complete) {
                    @Override
                    public void success(Boolean deployed) {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "deploy-more-agent-to-imagestore-backupStorage";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ImageStoreExtensionPoint> exts = pluginRgty.getExtensionList(ImageStoreExtensionPoint.class);
                FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                chain.allowEmptyFlow();
                for(ImageStoreExtensionPoint ext: exts) {
                    chain.then(new NoRollbackFlow() {
                        @Override
                        public void run(FlowTrigger trigger1, Map data) {
                            ext.addMoreAgentInBackupStorage(getSelf(), new Completion(trigger1) {
                                @Override
                                public void success() {
                                    trigger1.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger1.fail(errorCode);
                                }
                            });
                        }
                    });
                }
                chain.done(new FlowDoneHandler(trigger) {
                    @Override
                    public void handle(Map data) {
                        trigger.next();
                    }
                }).error(new FlowErrorHandler(trigger) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        trigger.fail(errCode);
                    }
                }).start();
            }
        }).then(new Flow() {
            String __name__ = "configure-iptables";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                StringBuilder builder = new StringBuilder();
                if (!ImageStoreBackupStorageGlobalProperty.MN_NETWORKS.isEmpty()) {
                    builder.append(String.format("sudo bash %s -m %s -p %s -c %s",
                            "/var/lib/zstack/imagestorebackupstorage/package/zstore-iptables",
                            ImageStoreBackupStorageConstant.IPTABLES_COMMENTS,
                            ImageStoreGlobalConfig.ZSTORE_ALLOW_PORTS.value(),
                            String.join(",", ImageStoreBackupStorageGlobalProperty.MN_NETWORKS)));
                } else {
                    builder.append(String.format("sudo bash %s -m %s -p %s",
                            "/var/lib/zstack/imagestorebackupstorage/package/zstore-iptables",
                            ImageStoreBackupStorageConstant.IPTABLES_COMMENTS,
                            ImageStoreGlobalConfig.ZSTORE_ALLOW_PORTS.value()));
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

                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        }).done(new FlowDoneHandler(complete) {
            @Override
            public void handle(Map data) {
                continueConnect(newAdded, complete);
            }
        }).error(new FlowErrorHandler(complete) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                complete.fail(errCode);
            }
        }).start();
    }

    private static String getCaCert(final ImageInventory iinv, final List<String> sysTags) {
        final String cert = ImageSystemTags.IMAGE_SERVER_CERT.getTokenByResourceUuid(
                iinv.getUuid(), ImageSystemTags.IMAGE_SEVER_CERT_TOKEN);
        if (cert == null) {
            throw new OperationFailureException(operr("Missing cert file for downloading image: %s", iinv.getName()));
        }

        return cert;
    }

    private DownloadImgCmd buildDownloadCmd(final DownloadImageMsg msg) {
        final ImageInventory iinv = msg.getImageInventory();

        final String imgurl = iinv.getUrl();
        final String name = iinv.getUuid();
        final String arch = ImageStoreBackupStorageConstant.DEFAULT_ARCH;
        final String desc = iinv.getDescription();
        final String tag = timestampToTag(iinv.getCreateDate());
        final String user = getImageOwner(iinv.getUuid());

        DownloadImgCmd cmd = new DownloadImgCmd();
        cmd.uuid = msg.getBackupStorageUuid();
        cmd.imgurl = imgurl;
        cmd.name = name;
        cmd.arch = arch;
        cmd.desc = desc;
        cmd.tag = tag;
        cmd.user = user;
        cmd.imageuuid = iinv.getUuid();
        cmd.desc = iinv.getName();

        if (isZstore(imgurl)) {
            cmd.cacert = getCaCert(iinv, msg.getSystemTags());
        }

        return cmd;
    }

    private DownloadImgCmd buildDownloadCmd(final DownloadImageFromRemoteTargetMsg msg) {
        final ImageInventory iinv = msg.getImage();

        final String imgurl = msg.getRemoteTargetUrl();
        final String name = iinv.getUuid();
        final String arch = ImageStoreBackupStorageConstant.DEFAULT_ARCH;
        final String desc = iinv.getDescription();
        final String tag = timestampToTag(iinv.getCreateDate());
        final String user = getImageOwner(iinv.getUuid());

        DownloadImgCmd cmd = new DownloadImgCmd();
        cmd.uuid = msg.getBackupStorageUuid();
        cmd.imgurl = imgurl;
        cmd.name = name;
        cmd.arch = arch;
        cmd.desc = desc;
        cmd.tag = tag;
        cmd.user = user;
        cmd.imageuuid = iinv.getUuid();
        cmd.desc = iinv.getName();

        return cmd;
    }

    private CancelDownloadImgCmd buildCancelDownloadCmd(final CancelDownloadImageMsg msg) {
        final ImageInventory iinv = msg.getImageInventory();
        final String imgurl = iinv.getUrl();
        final String name = iinv.getUuid();

        CancelDownloadImgCmd cmd = new CancelDownloadImgCmd();
        cmd.imgurl = imgurl;
        cmd.name = name;
        cmd.imageuuid = iinv.getUuid();
        cmd.cancellationApiId = msg.getCancellationApiId();
        return cmd;
    }

    private class DownloadResult {
        String md5sum;
        String installPath;
        long diskSize;
        long virtualSize;
        String format;
    }

    private void download(final DownloadImgCmd cmd, final ReturnValueCompletion<DownloadResult> completion) {
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.DOWNLOAD_IMAGE_PATH), cmd, new JsonAsyncRESTCallback<DownloadImgResponse>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(DownloadImgResponse ret) {
                updateCapacity(ret.getTotalSize(), ret.getFreeSize());
                if (!ret.isSuccess()) {
                    completion.fail(operr("failed to download image[url: %s] on backup storage[uuid: %s], because: %s",
                            cmd.imgurl, cmd.uuid, ret.getError())
                    );
                    return;
                }

                DownloadResult res = new DownloadResult();
                res.diskSize = ret.getDiskSize();
                res.virtualSize = ret.getVirtualsize();
                res.format = ret.format;
                if (cmd.imgurl.startsWith("upload://")) {
                    res.installPath = ret.getBlobsum();
                } else {
                    res.installPath = buildInstallPath(ret.getName(), ret.getId());
                }
                completion.success(res);
            }

            @Override
            public Class<DownloadImgResponse> getReturnClass() {
                return DownloadImgResponse.class;
            }
        });
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIReconnectImageStoreBackupStorageMsg) {
            handle((APIReconnectImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APIExportImageFromBackupStorageMsg) {
            handle((APIExportImageFromBackupStorageMsg) msg);
        } else if (msg instanceof APIDeleteExportedImageFromBackupStorageMsg) {
            handle((APIDeleteExportedImageFromBackupStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private boolean isImageStoreGCTaskRunning() {
        return imageStoreGCTaskMap.get(self.getUuid()) != null && imageStoreGCTaskMap.get(self.getUuid());
    }

    @Synchronized
    private void setImageStoreGCTaskStatus(boolean to) {
        final String finalBackupStorageUuid = self.getUuid();
        refreshVO();

        if (self == null) {
            logger.debug(String.format("image store[uuid:%s] might be deleted, remove related GC task status", finalBackupStorageUuid));
            imageStoreGCTaskMap.remove(finalBackupStorageUuid);
            return;
        }

        if (to) {
            logger.debug(String.format("mark image store[uuid:%s] is executing GC task, write operations will be refused", self.getUuid()));
        } else {
            logger.debug(String.format("mark image store[uuid:%s] GC finished, service is available now", self.getUuid()));
        }

        imageStoreGCTaskMap.putIfAbsent(self.getUuid(), false);
        logger.debug(String.format("image store gc task running status change from %s to %s", imageStoreGCTaskMap.get(self.getUuid()), to));
        imageStoreGCTaskMap.put(self.getUuid(), to);
    }

    private void checkImageStoreStatus() {
        if (isImageStoreGCTaskRunning()) {
            // TODO: increase timeout
            logger.warn("image store service is temporary not available, because it is reclaiming space now");
        }
    }

    @Override
    protected void handleLocalMessage(Message msg) throws URISyntaxException {
        if (msg instanceof GetImageStoreBackupStorageDownloadCredentialMsg) {
            handle((GetImageStoreBackupStorageDownloadCredentialMsg) msg);
        } else if (msg instanceof PackExportedImagesOnImageStoreMsg) {
            handle((PackExportedImagesOnImageStoreMsg) msg);
        } else if (msg instanceof DeleteImagePackageOnImageStoreMsg) {
            handle((DeleteImagePackageOnImageStoreMsg) msg);
        } else if (msg instanceof ExportImageFromBackupStorageMsg) {
            handle((ExportImageFromBackupStorageMsg) msg);
        } else if (msg instanceof DeleteExportedImageFromImageStoreBackupStorageMsg) {
            handle((DeleteExportedImageFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof GetImageDownloadProgressMsg) {
            handle((GetImageDownloadProgressMsg) msg);
        } else if (msg instanceof ReclaimSpaceFromImageStoreMsg) {
            handle((ReclaimSpaceFromImageStoreMsg) msg);
        } else if (msg instanceof ListImagesFromImageStoreMsg) {
            handle((ListImagesFromImageStoreMsg) msg);
        } else if (msg instanceof AllocateUploadWorkspaceMsg) {
            checkImageStoreStatus();
            handle((AllocateUploadWorkspaceMsg) msg);
        } else if (msg instanceof ExportNbdImagesMsg) {
            handle((ExportNbdImagesMsg) msg);
        } else if (msg instanceof CancelExportNbdImagesMsg) {
            handle((CancelExportNbdImagesMsg) msg);
        } else if (msg instanceof GetImageChainInfoMsg) {
            handle((GetImageChainInfoMsg) msg);
        } else if (msg instanceof ImportImageMsg) {
            checkImageStoreStatus();
            handle((ImportImageMsg) msg);
        } else if (msg instanceof PullImageToLocalMsg) {
            handle((PullImageToLocalMsg) msg);
        } else if (msg instanceof CancelJobBackupStorageMsg) {
            handle((CancelJobBackupStorageMsg) msg);
        } else if (msg instanceof SetImageStoreQuotaMsg) {
            handle((SetImageStoreQuotaMsg) msg);
        } else if (msg instanceof ArchiveBackupStorageMsg) {
            handle((ArchiveBackupStorageMsg) msg);
        } else if (msg instanceof UnpackBackupStorageMsg) {
            handle((UnpackBackupStorageMsg) msg);
        } else if (msg instanceof SyncBackupStorageDataMsg) {
            handle((SyncBackupStorageDataMsg) msg);
        } else if (msg instanceof AllocateImageInstallPathMsg) {
            handle((AllocateImageInstallPathMsg) msg);
        } else if (msg instanceof DownloadImageFromRemoteTargetMsg) {
            handle((DownloadImageFromRemoteTargetMsg) msg);
        } else if (msg instanceof UploadImageToRemoteTargetMsg) {
            handle((UploadImageToRemoteTargetMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(PullImageToLocalMsg msg) {
        ImageStoreBackupStorageVO vo = dbf.findByUuid(msg.getBackupStorageUuid(), ImageStoreBackupStorageVO.class);

        String name = getNameFromInstallPath(msg.getInstallPath());
        String id = getIdFromInstallPath(msg.getInstallPath());
        String localPath = PathUtil.createTempFile("zsdb-", ".gz");
        String cmd = buildRegistryCmd(vo.getHostname()) + String.format("pull -installpath %s %s:%s", localPath, name, id);
        ShellUtils.runAndReturn(cmd).raiseExceptionIfFail();

        PullImageToLocalReply reply = new PullImageToLocalReply();
        reply.setLocalInstallPath(localPath);
        bus.reply(msg, reply);
    }

    private void handle(final ListImagesFromImageStoreMsg msg) {
        ListImagesFromImageStoreReply reply = new ListImagesFromImageStoreReply();
        ImageStoreBackupStorageVO vo = dbf.findByUuid(msg.getUuid(), ImageStoreBackupStorageVO.class);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("list images directly from image store: %s", msg.getBackupStorageUuid()));

        chain.then(new NoRollbackFlow() {
            String __name__ = "list-image-names";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ShellResult result;
                if (CoreGlobalProperty.UNIT_TEST_ON) {
                    ImageStoreImageResponse is = new ImageStoreImageResponse();
                    is.setId("a250162e75c9b3f95ef1c737d3e9a6fb93c38c30");
                    is.setParent("");
                    is.setBlobsum("554e797bcb82c547a91abd19e39cd53e9390cdb65fa2b13ccedbd0e8ea1581d3");
                    is.setCreated("2017-09-20T13:48:39+08:00");
                    is.setAuthor("");
                    is.setArch("amd64");
                    is.setDesc("test test");
                    is.setSize(848568320L);
                    is.setVirtualsize(848568320L);
                    is.setName("2899f2b5cffc24a7ac8ea37fedee90d5");
                    result = new ShellResult();
                    result.setRetCode(0);
                    result.setStdout(JSONObjectUtil.toJsonString(Arrays.asList(is)));
                } else {
                    result = ShellUtils.runAndReturn(buildRegistryCmd(vo.getHostname()) + " list");
                }
                if (result.getRetCode() != 0) {
                    trigger.fail(operr("operation error, because:%s", result.getStderr()));
                    return;
                }
                if (result.getStdout() != null) {
                    List<ImageStoreImageStruct> images = new ArrayList<>();
                    List<ImageStoreImageResponse> resps = JSONObjectUtil.toCollection(result.getStdout(), ArrayList.class, ImageStoreImageResponse.class);
                    for (ImageStoreImageResponse resp : resps) {
                        images.add(new ImageStoreImageStruct(resp));
                    }
                    data.put("names", images);
                }
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "get-images-from-name";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ImageStoreImageStruct> structs = (List<ImageStoreImageStruct>) data.get("names");
                if (structs == null || structs.size() == 0) {
                    trigger.next();
                } else {
                    for (ImageStoreImageStruct struct : structs) {
                        ShellResult result;
                        if (CoreGlobalProperty.UNIT_TEST_ON) {
                            ImageStoreImageResponse is = new ImageStoreImageResponse();
                            is.setId("a250162e75c9b3f95ef1c737d3e9a6fb93c38c30");
                            is.setParent("");
                            is.setBlobsum("554e797bcb82c547a91abd19e39cd53e9390cdb65fa2b13ccedbd0e8ea1581d3");
                            is.setCreated("2017-09-20T13:48:39+08:00");
                            is.setAuthor("");
                            is.setArch("amd64");
                            is.setDesc("test test");
                            is.setSize(848568320L);
                            is.setVirtualsize(848568320L);
                            is.setName("2899f2b5cffc24a7ac8ea37fedee90d5");
                            result = new ShellResult();
                            result.setRetCode(0);
                            result.setStdout(JSONObjectUtil.toJsonString(Collections.singletonList(is)));
                        } else {
                            result = ShellUtils.runAndReturn(buildRegistryCmd(vo.getHostname()) + " images -name " + struct.getName());
                        }
                        List<ImageStoreImageResponse> resps = JSONObjectUtil.toCollection(result.getStdout(), ArrayList.class, ImageStoreImageResponse.class);
                        resps.forEach(resp -> reply.getStructs().add(new ImageStoreImageStruct(resp)));
                    }
                    trigger.next();
                }
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


        bus.reply(msg, reply);
    }

    private String getBackupStorageInstallPath(String imageUuid) {
        SimpleQuery<ImageBackupStorageRefVO> q = dbf.createQuery(ImageBackupStorageRefVO.class);
        q.select(ImageBackupStorageRefVO_.installPath);
        q.add(ImageBackupStorageRefVO_.backupStorageUuid, SimpleQuery.Op.EQ, self.getUuid());
        q.add(ImageBackupStorageRefVO_.imageUuid, SimpleQuery.Op.EQ, imageUuid);
        return q.findValue();
    }

    private List<String> getAvailableImages(String bsUuid) {
        List<String> images = Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, bsUuid)
                .select(ImageBackupStorageRefVO_.installPath)
                .listValues();

        pluginRgty.getExtensionList(ImageStoreReclaimSpaceExtensionPoint.class).forEach(it ->
                images.addAll(it.getAvailableImageInstallPaths(bsUuid)));
        return images;
    }

    private void handle(final GetImageDownloadProgressMsg msg) {
        GetDownloadProgressCmd cmd = new GetDownloadProgressCmd();
        cmd.setImageUuid(msg.getImageUuid());

        GetImageDownloadProgressReply r = new GetImageDownloadProgressReply();
        GetDownloadProgressResponse resp = restf.syncJsonPost(
                buildUrl(ImageStoreBackupStorageConstant.GET_DOWNLOAD_PROGRESS_PATH),
                cmd,
                GetDownloadProgressResponse.class);
        if (resp == null) {
            r.setError(operr("No response"));
            bus.reply(msg, r);
            return;
        }

        if (!resp.isSuccess()) {
            r.setError(operr("operation error, because:%s", resp.getError()));
            bus.reply(msg, r);
            return;
        }

        r.setCompleted(resp.isCompleted());
        r.setProgress(resp.getProgress());
        r.setDownloadSize(resp.getDownloadSize());
        r.setActualSize(resp.getActualSize());
        r.setSize(resp.getSize());
        r.setInstallPath(resp.getInstallPath());
        r.setFormat(resp.getFormat());
        r.setLastOpTime(resp.getLastOpTime());
        r.setSupportSuspend(true);
        bus.reply(msg, r);
    }

    private void doReclaimSpaceLocally(final RunGarbageCollectorCmd cmd, ReturnValueCompletion<Long> completion) {
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.RUNGC_IMAGE_PATH), cmd, new JsonAsyncRESTCallback<RunGarbageCollectorResponse>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(RunGarbageCollectorResponse ret) {
                if (!ret.isSuccess()) {
                    completion.fail(Platform.experr("operation error, because: %1$s", ret.getError()));
                    return;
                }

                if (ret.getFreed() > 0) {
                    updateCapacity(self.getTotalCapacity(), self.getAvailableCapacity() + ret.getFreed());
                }
                completion.success(ret.getFreed());
            }

            @Override
            public Class<RunGarbageCollectorResponse> getReturnClass() {
                return RunGarbageCollectorResponse.class;
            }
        });
    }

    private void handle(final ReclaimSpaceFromImageStoreMsg msg) {
        final List<String> installPaths = getAvailableImages(msg.getBackupStorageUuid());
        RunGarbageCollectorCmd cmd = new RunGarbageCollectorCmd();
        cmd.setImagesToKeep(installPaths);
        ReclaimSpaceFromImageStoreReply reply = new ReclaimSpaceFromImageStoreReply();
        if (ImageStoreBackupStorageSelector.isRemote(msg.getBackupStorageUuid())) {
            thdf.syncSubmit(new SyncTask<Object>() {
                @Override
                public String getSyncSignature() {
                    return String.format("reclaim-imagestore-thdf-for-bs-%s", self.getUuid());
                }

                @Override
                public int getSyncLevel() {
                    return 10;
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }

                @Override
                public Object call() {
                    RunGarbageCollectorResponse ret = restf.syncJsonPost(buildUrl(ImageStoreBackupStorageConstant.SYNC_RUNGC_IMAGE_PATH), cmd, RunGarbageCollectorResponse.class, TimeUnit.MINUTES, 10);
                    if (!ret.isSuccess()) {
                        reply.setError(operr("reclaim imagestore error, because:%s", ret.getError()));
                    } else {
                        reply.setFreedSpaceInBytes(ret.getFreed());
                        if (ret.getFreed() > 0) {
                            updateCapacity(self.getTotalCapacity(), self.getAvailableCapacity() + ret.getFreed());
                        }
                    }
                    bus.reply(msg, reply);
                    return null;
                }
            });
        } else {
            thdf.chainSubmit(new ChainTask(msg) {
                @Override
                public String getSyncSignature() {
                    return getName();
                }

                @Override
                public void run(SyncTaskChain chain) {
                    setImageStoreGCTaskStatus(true);
                    doReclaimSpaceLocally(cmd, new ReturnValueCompletion<Long>(chain) {
                        @Override
                        public void success(Long freed) {
                            setImageStoreGCTaskStatus(false);
                            reply.setFreedSpaceInBytes(freed);
                            bus.reply(msg, reply);
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            setImageStoreGCTaskStatus(false);
                            reply.setError(errorCode);
                            bus.reply(msg, reply);
                            chain.next();
                        }
                    });
                }

                @Override
                public String getName() {
                    return String.format("reclaim-space-from-image-store-%s", msg.getBackupStorageUuid());
                }
            });
        }
    }

    private void handle(final SetImageStoreQuotaMsg msg) {
        SetImageStoreQuotaReply reply = new SetImageStoreQuotaReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                setStorageQuota(msg.getQuota(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("set-image-store-%s-quota", msg.getBackupStorageUuid());
            }
        });
    }

    private void setStorageQuota(long quota, Completion completion) {
        SetStorageQuotaCmd cmd = new SetStorageQuotaCmd();
        cmd.setMaxCapacity(quota);
        if (ImageStoreBackupStorageSelector.isRemote(self.getUuid())) {
            SetStorageQuotaResponse ret = restf.syncJsonPost(buildUrl(ImageStoreBackupStorageConstant.SYNC_SET_STORAGE_QUOTA), cmd, SetStorageQuotaResponse.class);
            if (!ret.isSuccess()) {
                completion.fail(operr("failed to set max capacity on image store[uuid:%s], because: %s",
                        self.getUuid(), ret.getError()));
            } else {
                completion.success();
            }
        } else {
            restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.SET_STORAGE_QUOTA), cmd, new AsyncRESTCallback(completion) {
                @Override
                public void fail(ErrorCode err) {
                    completion.fail(operr("failed to set max capacity on image store[uuid:%s]", self.getUuid())
                            .withCause(err));
                }

                @Override
                public void success(HttpEntity<String> responseEntity) {
                    completion.success();
                }
            });
        }
    }

    private void handle(final DeleteExportedImageFromImageStoreBackupStorageMsg msg) {
        if (msg.getTargetProtocol().equals(RemoteTargetProtocol.HTTP)) {
            deleteHttpProtocolExportedImage(msg);
            return;
        }

        deleteDesignedProtocolExportedImage(msg);
    }

    private void deleteHttpProtocolExportedImage(final DeleteExportedImageFromImageStoreBackupStorageMsg msg) {
        DeleteExportedImageFromImageStoreBackupStorageReply reply = new DeleteExportedImageFromImageStoreBackupStorageReply();
        String installPath = msg.getRawPath() == null ? getBackupStorageInstallPath(msg.getImageUuid()) : msg.getRawPath();
        if (installPath == null) {
            reply.setError(operr("image[%s] not found on backup storage[%s]", msg.getImageUuid(), self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        DelExpImageCmd cmd = new DelExpImageCmd();
        cmd.setInstallPath(installPath);
        if (msg.getExportFormat() != null) {
            cmd.setExtFmt(msg.getExportFormat());
        }

        if (ImageStoreBackupStorageSelector.isRemote(msg.getBackupStorageUuid())) {
            thdf.syncSubmit(new SyncTask<Object>() {
                @Override
                public String getSyncSignature() {
                    return String.format("delete-export-image-thdf-for-image-%s", self.getUuid());
                }

                @Override
                public int getSyncLevel() {
                    return 10;
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }

                @Override
                public Object call() {
                    DelExpImageResponse ret = restf.syncJsonPost(buildUrl(ImageStoreBackupStorageConstant.SYNC_DELEXP_IMAGE_PATH), cmd, DelExpImageResponse.class, TimeUnit.MINUTES, 10);
                    if (!ret.isSuccess()) {
                        reply.setError(operr("operation error, because:%s", ret.getError()));
                    } else if (msg.getImageUuid() != null) {
                        SQL.New(ImageBackupStorageRefVO.class)
                                .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                                .set(ImageBackupStorageRefVO_.exportUrl, null)
                                .set(ImageBackupStorageRefVO_.exportMd5Sum, null)
                                .update();
                    }
                    bus.reply(msg, reply);
                    return null;
                }
            });
        } else {
            restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.DELEXP_IMAGE_PATH), cmd, new JsonAsyncRESTCallback<DelExpImageResponse>(msg) {
                @Override
                public void fail(ErrorCode err) {
                    logger.warn(String.format("failed to export image[%s], because %s", msg.getImageUuid(), err));
                    reply.setError(err);
                    bus.reply(msg, reply);
                }

                @Override
                public void success(DelExpImageResponse ret) {
                    if (!ret.isSuccess()) {
                        reply.setError(operr("operation error, because:%s", ret.getError()));
                    } else if (msg.getImageUuid() != null) {
                        SQL.New(ImageBackupStorageRefVO.class)
                                .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                                .set(ImageBackupStorageRefVO_.exportUrl, null)
                                .set(ImageBackupStorageRefVO_.exportMd5Sum, null)
                                .update();
                    }
                    bus.reply(msg, reply);
                }

                @Override
                public Class<DelExpImageResponse> getReturnClass() {
                    return DelExpImageResponse.class;
                }
            });
        }

    }

    private void handle(final APIDeleteExportedImageFromBackupStorageMsg msg) {
        final APIDeleteExportedImageFromBackupStorageEvent evt = new APIDeleteExportedImageFromBackupStorageEvent(msg.getId());

        DeleteExportedImageFromImageStoreBackupStorageMsg demsg = new DeleteExportedImageFromImageStoreBackupStorageMsg();
        demsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        demsg.setImageUuid(msg.getImageUuid());
        bus.makeTargetServiceIdByResourceUuid(demsg, BackupStorageConstant.SERVICE_ID, msg.getImageUuid());
        bus.send(demsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });

    }

    private void handle(final DeleteImagePackageOnImageStoreMsg msg) {
        DeleteImagePackageOnImageStoreReply reply = new DeleteImagePackageOnImageStoreReply();
        DeleteImagePackageCmd cmd = new DeleteImagePackageCmd();
        cmd.setExportUrl(msg.getExportUrl());
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.DELETE_IMAGE_PACKAGE), cmd,
                new JsonAsyncRESTCallback<DeleteImagePackageResponse>(msg) {
                    @Override
                    public void fail(ErrorCode err) {
                        logger.warn(String.format("failed to delete image package: %s, because: %s",
                                msg.getExportUrl(), err.getDetails()));
                        reply.setError(err);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void success(DeleteImagePackageResponse ret) {
                        if (!ret.isSuccess()) {
                            reply.setError(operr("failed to delete image package, because: %s", ret.getError()));
                        }
                        bus.reply(msg, reply);
                    }

                    @Override
                    public Class<DeleteImagePackageResponse> getReturnClass() {
                        return DeleteImagePackageResponse.class;
                    }
                }
        );
    }

    private void handle(final PackExportedImagesOnImageStoreMsg msg) {
        PackExportedImagesOnImageStoreReply reply = new PackExportedImagesOnImageStoreReply();
        List<ImageBackupStorageRefVO> refs = Q.New(ImageBackupStorageRefVO.class)
                .in(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuids())
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, self.getUuid())
                .list();
        Map<String, ImageBackupStorageRefInventory> imageRefMap = refs.stream()
                .collect(Collectors.toMap(ImageBackupStorageRefVO::getImageUuid,
                        ImageBackupStorageRefInventory::valueOf));
        List<String> notOnBsImageUuids = checkExportedImageOnBS(imageRefMap, msg.getImageUuids());
        if (!notOnBsImageUuids.isEmpty()) {
            reply.setError(operr("some images [%s] are not exported on the backup storage[uuid: %s]",
                    StringUtils.join(notOnBsImageUuids, ','), msg.getBackupStorageUuid()));
            bus.reply(msg, reply);
            return;
        }

        List<String> installPathList = msg.getImageUuids().stream()
                .map(imgUuid -> imageRefMap.get(imgUuid).getInstallPath())
                .collect(Collectors.toList());

        PackExportedImagesCmd cmd = new PackExportedImagesCmd();
        cmd.setInstallPathList(installPathList);
        cmd.setImageExportFormat(msg.getImageExportFormat());
        cmd.setConfigFileContent(msg.getConfigFileContent());
        cmd.setConfigFileFormat(msg.getConfigFileFormat());
        cmd.setPackageName(msg.getPackageName());
        cmd.setPackageFormat(msg.getPackageFormat());

        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.PACKAGE_EXPORTED_IMAGES_PATH), cmd,
                new JsonAsyncRESTCallback<PackExportedImagesResponse>(msg) {
                    @Override
                    public void fail(ErrorCode err) {
                        logger.warn(String.format("failed to package exported images, because %s", err));
                        reply.setError(err);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void success(PackExportedImagesResponse ret) {
                        if (ret.isSuccess()) {
                            reply.setExportUrl(buildUrl(ret.getPackageUrl()));
                            reply.setMd5Sum(ret.getMd5Sum());
                            reply.setSize(ret.getSize());
                        } else {
                            reply.setError(operr("failed to package exported images, because %s", ret.getError()));
                        }
                        bus.reply(msg, reply);
                    }

                    @Override
                    public Class<PackExportedImagesResponse> getReturnClass() {
                        return PackExportedImagesResponse.class;
                    }
                }
        );
    }

    private List<String> checkExportedImageOnBS(Map<String, ImageBackupStorageRefInventory> imageRefs, List<String> imageUuids) {
        List<String> notOnBSImageUuids = new ArrayList<>();
        for (String uuid : imageUuids) {
            if (!imageRefs.containsKey(uuid)) {
                notOnBSImageUuids.add(uuid);
            }
        }
        return notOnBSImageUuids;
    }

    private RemoteTarget castReplyToTarget(String uri, RemoteTargetProtocol type) {
        if (type.equals(RemoteTargetProtocol.NBD)) {
            return new NbdRemoteTarget(uri);
        }

        throw new OperationFailureException(operr("not implement remote target type: %s", type.toString()));
    }

    private void exportImageWithDesignedProtocol(final ExportImageFromBackupStorageMsg msg) {
        ExportImageFromBackupStorageReply reply = new ExportImageFromBackupStorageReply();
        ExportImageAsRemoteTargetCmd cmd = new ExportImageAsRemoteTargetCmd();
        cmd.setHostname(getSelf().getHostname());
        cmd.setRemoteTargetType(msg.getTargetProtocol().toString());
        cmd.setInstallPath(getBackupStorageInstallPath(msg.getImageUuid()));
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.EXPORT_IMAGE_AS_REMOTE_TARGET_PATH), cmd, new JsonAsyncRESTCallback<ExportImageAsRemoteTargetResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                logger.warn(String.format("failed to export image[%s] as remote target, because %s", msg.getImageUuid(), err));
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(ExportImageAsRemoteTargetResponse ret) {
                if (ret.isSuccess()) {
                    reply.setImageUrl(ret.getTargetUri());
                } else {
                    reply.setError(operr("operation error, because:%s", ret.getError()));
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<ExportImageAsRemoteTargetResponse> getReturnClass() {
                return ExportImageAsRemoteTargetResponse.class;
            }
        });
    }

    private void handle(final ExportImageFromBackupStorageMsg msg) {
        if (msg.getTargetProtocol().equals(RemoteTargetProtocol.HTTP)) {
            exportImageWithHttpProtocol(msg);
            return;
        }

        exportImageWithDesignedProtocol(msg);
    }

    private void exportImageWithHttpProtocol(final ExportImageFromBackupStorageMsg msg) {
        String installPath = msg.getRawPath() == null ? getBackupStorageInstallPath(msg.getImageUuid()) : msg.getRawPath();
        ExportImageFromBackupStorageReply reply = new ExportImageFromBackupStorageReply();
        if (installPath == null) {
            reply.setError(operr("image[%s] not found on backup storage[%s]", msg.getImageUuid(), self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        refreshVO();
        Long actualSize = msg.getRequiredSize() == null ? Q.New(ImageVO.class)
                .select(ImageVO_.actualSize)
                .eq(ImageVO_.uuid, msg.getImageUuid())
                .findValue() : msg.getRequiredSize();
        if (!backupStorageHasEnoughSpace(actualSize)) {
            throw new OperationFailureException(operr("the backup storage[uuid:%s] has not enough capacity[%s] to export",
                    self.getUuid(), actualSize));
        }

        ExportImageCmd cmd = new ExportImageCmd();
        if (msg.getExportFormat() != null) {
            cmd.setExtFmt(msg.getExportFormat());
        }
        cmd.setInstallPath(installPath);

        if (ImageStoreBackupStorageSelector.isRemote(msg.getBackupStorageUuid())) {
            thdf.syncSubmit(new SyncTask<Object>() {
                @Override
                public String getSyncSignature() {
                    return String.format("export-image-thdf-for-image-%s", self.getUuid());
                }

                @Override
                public int getSyncLevel() {
                    return 10;
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }

                @Override
                public Object call() {
                    ExportImageResponse ret = restf.syncJsonPost(buildUrl(ImageStoreBackupStorageConstant.SYNC_EXPORT_IMAGE_PATH), cmd, ExportImageResponse.class, TimeUnit.MINUTES, 30);
                    if (ret.isSuccess()) {
                        final String imgUrl = ret.getImgUrl();
                        String exportUrl = ImageStoreHelper.ImageStoreExportUrl.addNameToExportUrl(buildUrl(imgUrl), msg.getImageName());
                        reply.setImageUrl(exportUrl);
                        reply.setMd5sum(ret.getMd5Sum());
                        reply.setImageLocalPath(imgUrl);
                    } else {
                        reply.setError(operr("operation error, because:%s", ret.getError()));
                    }
                    bus.reply(msg, reply);
                    return null;
                }
            });
        } else {
            restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.EXPORT_IMAGE_PATH), cmd, new JsonAsyncRESTCallback<ExportImageResponse>(msg) {
                @Override
                public void fail(ErrorCode err) {
                    logger.warn(String.format("failed to export image[%s], because %s", msg.getImageUuid(), err));
                    reply.setError(err);
                    bus.reply(msg, reply);
                }

                @Override
                public void success(ExportImageResponse ret) {
                    if (ret.isSuccess()) {
                        final String imgUrl = ret.getImgUrl();
                        reply.setMd5sum(ret.getMd5Sum());
                        reply.setImageLocalPath(imgUrl);
                        String exportUrl = ImageStoreHelper.ImageStoreExportUrl.addNameToExportUrl(buildUrl(imgUrl), msg.getImageName());
                        reply.setImageUrl(exportUrl);
                        SQL.New(ImageBackupStorageRefVO.class)
                                .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                                .set(ImageBackupStorageRefVO_.exportUrl, exportUrl)
                                .set(ImageBackupStorageRefVO_.exportMd5Sum, reply.getMd5sum())
                                .update();
                    } else {
                        reply.setError(operr("operation error, because:%s", ret.getError()));
                    }
                    bus.reply(msg, reply);
                }

                @Override
                public Class<ExportImageResponse> getReturnClass() {
                    return ExportImageResponse.class;
                }
            });
        }
    }

    private boolean backupStorageHasEnoughSpace(long requiredSize) {
        long reservedCapacity = SizeUtils.sizeStringToBytes(rcf.getResourceConfigValue(BackupStorageGlobalConfig.RESERVED_CAPACITY, self.getUuid(), String.class));
        return self.getAvailableCapacity() - reservedCapacity >= requiredSize;
    }

    private void handle(final APIExportImageFromBackupStorageMsg msg) {
        final APIExportImageFromBackupStorageEvent evt = new APIExportImageFromBackupStorageEvent(msg.getId());

        ExportImageFromBackupStorageMsg eimsg = new ExportImageFromBackupStorageMsg();
        eimsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        eimsg.setImageUuid(msg.getImageUuid());
        if (msg.getExportFormat() != null) {
            eimsg.setExportFormat(msg.getExportFormat());
        }
        eimsg.setImageName(Q.New(ImageVO.class).select(ImageVO_.name).eq(ImageVO_.uuid, msg.getImageUuid()).findValue());

        bus.makeTargetServiceIdByResourceUuid(eimsg, BackupStorageConstant.SERVICE_ID, msg.getImageUuid());
        bus.send(eimsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    ExportImageFromBackupStorageReply rpl = (ExportImageFromBackupStorageReply) reply;
                    evt.setImageUrl(rpl.getImageUrl());
                    evt.setExportMd5Sum(rpl.getMd5sum());
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });

    }

    @Override
    protected void handle(final DownloadImageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                checkImageStoreStatus();

                final DownloadImageReply reply = new DownloadImageReply();

                doDownload(msg, new ReturnValueCompletion<DownloadResult>(chain) {
                    @Override
                    public void success(DownloadResult res) {
                        reply.setInstallPath(res.installPath);
                        reply.setMd5sum(res.md5sum);
                        reply.setSize(res.virtualSize);
                        reply.setActualSize(res.diskSize);
                        reply.setFormat(res.format);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("imagestore-%s-download-image-%s", msg.getBackupStorageUuid(), msg.getImageInventory().getUrl());
            }
        });
    }

    @Transactional
    private void doDownload(final DownloadImageMsg msg, ReturnValueCompletion<DownloadResult> completion) {
        if (ImageStoreBackupStorageSelector.isRemote(msg.getBackupStorageUuid())) {
            completion.fail(operr("image store [%s] cannot add image, because it is used for backup remote"));
            return;
        }

        String installPath = buildInstallPath(msg.getImageInventory().getName(), msg.getImageInventory().getUuid());

        String sql = "update ImageBackupStorageRefVO set installPath = :installPath " +
                "where backupStorageUuid = :bsUuid and imageUuid = :imageUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("installPath", installPath);
        q.setParameter("bsUuid", msg.getBackupStorageUuid());
        q.setParameter("imageUuid", msg.getImageInventory().getUuid());
        q.executeUpdate();

        download(buildDownloadCmd(msg), completion);
    }

    protected void handle(final CancelDownloadImageMsg msg) {
        CancelDownloadImageReply reply = new CancelDownloadImageReply();

        CancelDownloadImgCmd cmd = buildCancelDownloadCmd(msg);
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.CANCEL_DOWNLOAD_IMAGE_PATH), cmd, new JsonAsyncRESTCallback<CancelDownloadImgRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CancelDownloadImgRsp rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("operation error, because:%s", rsp.getError()));
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<CancelDownloadImgRsp> getReturnClass() {
                return CancelDownloadImgRsp.class;
            }
        });
    }

    @Override
    protected void handle(final DownloadVolumeMsg msg) {
        throw new CloudRuntimeException("not implemented");
    }

    @Override
    protected void handle(final GetImageSizeOnBackupStorageMsg msg) {
        // get disk and virtual size
        final GetImageSizeOnBackupStorageReply reply = new GetImageSizeOnBackupStorageReply();

        GetImageInfoCmd cmd = new GetImageInfoCmd();
        cmd.imageUuid = msg.getImageUuid();
        cmd.installPath = msg.getImageUrl();

        if (ImageStoreBackupStorageSelector.isRemote(self.getUuid())) {
            thdf.syncSubmit(new SyncTask<Object>() {
                @Override
                public String getSyncSignature() {
                    return String.format("get-image-size-thdf-for-image-%s", cmd.imageUuid);
                }

                @Override
                public int getSyncLevel() {
                    return 20;
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }

                @Override
                public Object call() {
                    ImageInfoResponse rsp = restf.syncJsonPost(
                            buildUrl(ImageStoreBackupStorageConstant.SYNC_GET_IMAGE_INFO), cmd, ImageInfoResponse.class, TimeUnit.SECONDS, 5);
                    if (!rsp.isSuccess()) {
                        reply.setError(operr("%s", rsp.getError()));
                    } else {
                        reply.setSize(rsp.size);
                    }

                    bus.reply(msg, reply);
                    return null;
                }
            });
        } else {
            restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.GET_IMAGE_INFO), cmd,
                    new JsonAsyncRESTCallback<ImageInfoResponse>(msg) {
                        @Override
                        public void fail(ErrorCode err) {
                            reply.setError(err);
                            bus.reply(msg, reply);
                        }

                        @Override
                        public void success(ImageInfoResponse rsp) {
                            if (!rsp.isSuccess()) {
                                reply.setError(operr("operation error, because:%s", rsp.getError()));
                            } else {
                                reply.setSize(rsp.size);
                            }

                            bus.reply(msg, reply);
                        }

                        @Override
                        public Class<ImageInfoResponse> getReturnClass() {
                            return ImageInfoResponse.class;
                        }
                    });
        }

    }

    @Override
    protected void handle(BackupStorageAskInstallPathMsg msg) {
        // Image store cannot reply this message.
        BackupStorageAskInstallPathReply reply = new BackupStorageAskInstallPathReply();
        String installPath = buildInstallPath(msg.getImageUuid(), "dummy");
        reply.setInstallPath(installPath);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final DeleteBitsOnBackupStorageMsg msg) {
        checkImageStoreStatus();
        final DeleteBitsOnBackupStorageReply reply = new DeleteBitsOnBackupStorageReply();

        DeleteCmd cmd = new DeleteCmd();
        cmd.setInstallPath(msg.getInstallPath());
        cmd.setUuid(self.getUuid());

        if (!ImageStoreBackupStorageSelector.isRemote(self.getUuid())) {
            restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.DELETE_IMAGE_PATH), cmd, new JsonAsyncRESTCallback<DeleteResponse>(msg) {
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
                        updateCapacity(ret.getTotalSize(), ret.getFreeSize());
                    }
                    bus.reply(msg, reply);
                }

                @Override
                public Class<DeleteResponse> getReturnClass() {
                    return DeleteResponse.class;
                }
            });
        } else {
            thdf.syncSubmit(new SyncTask<Object>() {
                @Override
                public String getSyncSignature() {
                    return String.format("delete-image-bits-thdf-for-image-%s", self.getUuid());
                }

                @Override
                public int getSyncLevel() {
                    return 20;
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }

                @Override
                public Object call() {
                    DeleteResponse ret = restf.syncJsonPost(buildUrl(ImageStoreBackupStorageConstant.SYNC_DELETE_IMAGE_PATH), cmd, DeleteResponse.class, TimeUnit.MINUTES, 30);
                    if (!ret.isSuccess()) {
                        logger.warn(String.format("failed to delete bits[%s], schedule clean up, %s",
                                msg.getInstallPath(), ret.getError()));
                        //TODO GC
                    } else {
                        updateCapacity(ret.getTotalSize(), ret.getFreeSize());
                    }
                    bus.reply(msg, reply);
                    return null;
                }
            });
        }
    }

    @Override
    protected void pingHook(final Completion completion) {
        final Integer MAX_PING_CNT = BackupStorageGlobalConfig.MAXIMUM_PING_FAILURE.value(Integer.class);
        final List<Integer> stepCount = new ArrayList<>();
        for (int i = 1; i <= MAX_PING_CNT; i++) {
            stepCount.add(i);
        }
        final List<ErrorCode> errs = new ArrayList<>();
        new While<>(stepCount).each((currentStep, compl) -> ping(new Completion(compl) {
            @Override
            public void success() {
                compl.allDone();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("ping backup storage failed (%d/%d):%n%s",
                        currentStep, MAX_PING_CNT, errorCode.getReadableDetails()));
                errs.add(errorCode);

                if (errs.size() != stepCount.size()) {
                    int sleep = BackupStorageGlobalConfig.SLEEP_TIME_AFTER_PING_FAILURE.value(Integer.class);
                    if (sleep > 0) {
                        try {
                            TimeUnit.SECONDS.sleep(sleep);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                compl.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.size() == stepCount.size()) {
                    completion.fail(errs.get(0));
                    return;
                }
                completion.success();
            }
        });

    }

    private void ping(final Completion completion) {
        PingCmd cmd = new PingCmd();
        cmd.uuid = self.getUuid();

        if (!ImageStoreBackupStorageSelector.isRemote(self.getUuid())) {
            restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.PING_PATH), cmd, new JsonAsyncRESTCallback<PingResponse>(completion) {
                @Override
                public void fail(ErrorCode err) {
                    completion.fail(err);
                }

                @Override
                public void success(PingResponse ret) {
                    if (ret.isSuccess()) {
                        if (ret.getUuid() == null || ret.getUuid().equals("")) {
                            connect(new Completion(completion) {
                                @Override
                                public void success() {
                                    changeStatus(BackupStorageStatus.Connected, new NoErrorCompletion(completion) {
                                        @Override
                                        public void done() {
                                            self = dbf.reload(self);
                                            completion.success();
                                        }
                                    });
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    completion.fail(errorCode);
                                }
                            });
                        } else if (!self.getUuid().equals(ret.getUuid())) {
                            ErrorCode err = err(BackupStorageErrors.OTHER_NODE_MANAGE_ERROR, "the uuid of imagestoreBackupStorage agent " +
                                    "changed[expected:%s, actual:%s], it's most likely the agent was manually restarted. " +
                                    "Issue a reconnect to sync the status", self.getUuid(), ret.getUuid());

                            completion.fail(err);
                        } else {
                            updateCapacity(ret.totalSize, ret.freeSize);
                            completion.success();
                        }
                    } else {
                        completion.fail(operr("operation error, because:%s", ret.getError()));
                    }
                }

                @Override
                public Class<PingResponse> getReturnClass() {
                    return PingResponse.class;
                }
            }, TimeUnit.SECONDS, 60);
        } else {
            // image store deployed in public cloud, it couldn't callback mn node
            PingResponse resp = restf.syncJsonPost(buildUrl(ImageStoreBackupStorageConstant.SYNC_PING_PATH), cmd, PingResponse.class, TimeUnit.SECONDS, 5);
            if (resp.isSuccess()) {
                if (resp.getUuid() == null || resp.getUuid().equals("")) {
                    connect(new Completion(completion) {
                        @Override
                        public void success() {
                            changeStatus(BackupStorageStatus.Connected, new NoErrorCompletion(completion) {
                                @Override
                                public void done() {
                                    self = dbf.reload(self);
                                    completion.success();
                                }
                            });
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                        }
                    });
                } else if (!self.getUuid().equals(resp.getUuid())) {
                    completion.fail(operr("the uuid of imagestoreBackupStorage agent changed[expected:%s, actual:%s], it's most likely" +
                            " the agent was manually restarted. Issue a reconnect to sync the status", self.getUuid(), resp.getUuid()));
                } else {
                    updateCapacity(resp.totalSize, resp.freeSize);
                    completion.success();
                }
            } else {
                completion.fail(operr("operation error, because:%s", resp.getError()));
            }
        }
    }

    @Override
    public List<ImageInventory> scanImages() {
        return null;
    }

    @Override
    protected void handle(final SyncImageSizeOnBackupStorageMsg msg) {
        // get disk and virtual size
        final SyncImageSizeOnBackupStorageReply reply = new SyncImageSizeOnBackupStorageReply();

        ImageInventory image = msg.getImage();
        GetImageInfoCmd cmd = new GetImageInfoCmd();
        cmd.imageUuid = image.getUuid();

        ImageBackupStorageRefInventory ref = CollectionUtils.findOneOrNull(image.getBackupStorageRefs(),
                arg -> arg.getBackupStorageUuid().equals(self.getUuid()));

        if (ref == null) {
            throw new CloudRuntimeException(String.format("cannot find ImageBackupStorageRefInventory of image[uuid:%s] for the backup storage[uuid:%s]",
                    image.getUuid(), self.getUuid()));
        }

        cmd.installPath = ref.getInstallPath();

        if (ImageStoreBackupStorageSelector.isRemote(self.getUuid())) {
            thdf.syncSubmit(new SyncTask<Object>() {
                @Override
                public String getSyncSignature() {
                    return String.format("sync-image-size-thdf-for-image-%s", cmd.imageUuid);
                }

                @Override
                public int getSyncLevel() {
                    return 20;
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }

                @Override
                public Object call() {
                    ImageInfoResponse rsp = restf.syncJsonPost(
                            buildUrl(ImageStoreBackupStorageConstant.SYNC_GET_IMAGE_INFO), cmd, ImageInfoResponse.class, TimeUnit.SECONDS, 5);
                    if (!rsp.isSuccess()) {
                        reply.setError(operr("operation error, because:%s", rsp.getError()));
                    } else {
                        reply.setActualSize(rsp.size);
                        reply.setSize(rsp.virtualsize);
                    }

                    bus.reply(msg, reply);
                    return null;
                }
            });
        } else {
            restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.GET_IMAGE_INFO), cmd, new JsonAsyncRESTCallback<ImageInfoResponse>(msg) {
                @Override
                public void fail(ErrorCode err) {
                    reply.setError(err);
                    bus.reply(msg, reply);
                }

                @Override
                public void success(ImageInfoResponse rsp) {
                    if (!rsp.isSuccess()) {
                        reply.setError(operr("operation error, because:%s", rsp.getError()));
                    } else {
                        reply.setActualSize(rsp.size);
                        reply.setSize(rsp.virtualsize);
                    }

                    bus.reply(msg, reply);
                }

                @Override
                public Class<ImageInfoResponse> getReturnClass() {
                    return ImageInfoResponse.class;
                }
            });
        }
    }

    @Override
    protected void handle(GetLocalFileSizeOnBackupStorageMsg msg) {
        GetLocalFileSizeOnBackupStorageReply reply = new GetLocalFileSizeOnBackupStorageReply();
        GetLocalFileSizeCmd cmd = new GetLocalFileSizeCmd();
        cmd.path = msg.getUrl();
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.GET_LOCAL_FILE_SIZE), cmd,
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

        ImageVO image = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);

        if (image.getMd5Sum() != null) {
            reply.setEncrypted(image.getMd5Sum());
            bus.reply(msg, reply);
            return;
        }

        GetImageInfoCmd cmd = new GetImageInfoCmd();
        cmd.imageUuid = msg.getImageUuid();

        ImageBackupStorageRefVO ref = CollectionUtils.findOneOrNull(image.getBackupStorageRefs(),
                arg -> arg.getBackupStorageUuid().equals(self.getUuid()));

        if (ref == null) {
            throw new CloudRuntimeException(String.format("cannot find ImageBackupStorageRefVO of image[uuid:%s] for the backup storage[uuid:%s]",
                    image.getUuid(), self.getUuid()));
        }

        cmd.installPath = ref.getInstallPath();

        thdf.syncSubmit(new SyncTask<Object>() {
            @Override
            public String getSyncSignature() {
                return String.format("sync-image-into-%s", cmd.imageUuid);
            }

            @Override
            public int getSyncLevel() {
                return 20;
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public Object call() {
                ImageInfoResponse rsp = restf.syncJsonPost(
                        buildUrl(ImageStoreBackupStorageConstant.SYNC_GET_IMAGE_INFO), cmd, ImageInfoResponse.class, TimeUnit.SECONDS, 5);
                if (!rsp.isSuccess()) {
                    reply.setError(operr( rsp.getError()," failed to get image info in imageStorage"));
                } else {
                    reply.setEncrypted(rsp.blobsum);
                }

                bus.reply(msg, reply);
                return null;
            }
        });

    }

    @Override
    protected BackupStorageVO updateBackupStorage(APIUpdateBackupStorageMsg msg) {
        if (!(msg instanceof APIUpdateImageStoreBackupStorageMsg)) {
            return super.updateBackupStorage(msg);
        }

        ImageStoreBackupStorageVO vo = (ImageStoreBackupStorageVO) super.updateBackupStorage(msg);
        vo = vo == null ? getSelf() : vo;

        APIUpdateImageStoreBackupStorageMsg umsg = (APIUpdateImageStoreBackupStorageMsg) msg;
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
    protected void handle(final RestoreImagesBackupStorageMetadataToDatabaseMsg msg) {
        RestoreImagesBackupStorageMetadataToDatabaseReply reply = new RestoreImagesBackupStorageMetadataToDatabaseReply();
        doRestoreImagesBackupStorageMetadataToDatabase(msg);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(CalculateImageHashOnBackupStorageMsg msg) {
        CalculateImageHashOnBackupStorageReply reply = new CalculateImageHashOnBackupStorageReply();
        thdf.chainSubmit(new ChainTask(msg) {
            private final String name = String.format("calculate-image-%s-hash-from-bs-%s", msg.getImageUuid(), self.getUuid());

            @Override
            public String getSyncSignature() {
                return name;
            }

            @Override
            public void run(SyncTaskChain chain) {
                calculateImageHash(msg, new ReturnValueCompletion<String>(chain) {

                    @Override
                    public void success(String returnValue) {
                        reply.setHashValue(returnValue);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return name;
            }

        });
    }

    private void calculateImageHash(CalculateImageHashOnBackupStorageMsg msg, ReturnValueCompletion<String> completion) {
        ImageVO image = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        if (image.getMd5Sum() != null) {
            completion.success(image.getMd5Sum());
            return;
        }

        GetImageHashCmd cmd = new GetImageHashCmd();
        cmd.setInstallPath(getBackupStorageInstallPath(msg.getImageUuid()));
        cmd.setAlogrithm(msg.getAlgorithm());
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.GET_IMAGE_HASH), cmd,
                new JsonAsyncRESTCallback<GetImageHashRsp>(msg) {
                    @Override
                    public void fail(ErrorCode err) {
                        completion.fail(err);
                    }

                    @Override
                    public void success(GetImageHashRsp rsp) {
                        if (!rsp.isSuccess()) {
                            completion.fail(operr("get image hash failed, because:%s", rsp.getError()));
                            return;
                        }

                        completion.success(rsp.getHash());
                    }

                    @Override
                    public Class<GetImageHashRsp> getReturnClass() {
                        return GetImageHashRsp.class;
                    }
                });
    }

    @SyncThread(signature = RESTORE_IMAGES_BACKUP_STORAGE_METADATA_TO_DATABASE)
    private void doRestoreImagesBackupStorageMetadataToDatabase(RestoreImagesBackupStorageMetadataToDatabaseMsg msg) {
        metaDataMaker.restoreImagesBackupStorageMetadataToDatabase(msg.getImagesMetadata(), msg.getBackupStorageUuid());
    }

    private void handle(final APIReconnectImageStoreBackupStorageMsg msg) {
        final APIReconnectImageStoreBackupStorageEvent evt = new APIReconnectImageStoreBackupStorageEvent(msg.getId());
        connect(new Completion(msg) {
            @Override
            public void success() {
                changeStatus(BackupStorageStatus.Connected, new NoErrorCompletion(msg) {
                    @Override
                    public void done() {
                        self = dbf.reload(self);
                        evt.setInventory(ImageStoreBackupStorageInventory.valueOf(getSelf()));
                        bus.publish(evt);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(err(ImageStoreBackupStorageErrors.RECONNECT_ERROR, "unable to reconnect target server: %s, detail error info: %s",
                        msg.getBackupStorageUuid(), errorCode.getDetails()));
                bus.publish(evt);
            }
        });
    }

    private void handle(final GetImageStoreBackupStorageDownloadCredentialMsg msg) {
        final GetImageStoreBackupStorageDownloadCredentialReply reply = new GetImageStoreBackupStorageDownloadCredentialReply();

        String hostname = getImageStoreBackupStorageHostName(msg.getBackupStorageUuid());
        hostname = hostname != null ? hostname : getSelf().getHostname();

        reply.setHostname(hostname);
        reply.setUsername(getSelf().getUsername());
        reply.setPassword(getSelf().getPassword());
        reply.setSshPort(getSelf().getSshPort());
        bus.reply(msg, reply);
    }

    static String getSyncNetworkAddress(String bsUuid) {
        return getNetworkAddress(bsUuid,
                ImageStoreSystemTags.SYNC_NETWORK,
                ImageStoreSystemTags.SYNC_NETWORK_TOKEN);
    }

    private static String getDataNetworkAddress(String bsUuid) {
        return getNetworkAddress(bsUuid,
                BackupStorageSystemTags.BACKUP_STORAGE_DATA_NETWORK,
                BackupStorageSystemTags.BACKUP_STORAGE_DATA_NETWORK_TOKEN);
    }

    private static String getBackupNetworkAddress(String bsUuid) {
        if (isVolumeBackupMsg()) {
            return getNetworkAddress(bsUuid, ImageStoreSystemTags.BACKUP_CIDR, ImageStoreSystemTags.BACKUP_CIDR_TOKEN);
        }
        return null;
    }

    static String getImageStoreBackupStorageHostName(String bsUuid) {
        String backupNetworkAddress = getBackupNetworkAddress(bsUuid);
        if (backupNetworkAddress != null) {
            return backupNetworkAddress;
        }

        return getDataNetworkAddress(bsUuid);
    }

    private static boolean isVolumeBackupMsg() {
        if (ThreadContext.get(THREAD_CONTEXT_TASK_NAME) == null) {
            return false;
        }
        return ThreadContext.get(THREAD_CONTEXT_TASK_NAME).contains(VOLUME_BACKUP_PACKAGE_NAME);
    }

    public static String getNetworkAddress(String bsUuid, PatternedSystemTag systemTag, String token) {
        final String cidr = systemTag.getTokenByResourceUuid(bsUuid, token);
        if (cidr == null) {
            logger.warn(String.format("BS[uuid:%s] has no %s config", bsUuid, token));
            return null;
        }

        final String extraIps = BackupStorageSystemTags.EXTRA_IPS.getTokenByResourceUuid(
                bsUuid, BackupStorageSystemTags.EXTRA_IPS_TOKEN);
        if (extraIps == null) {
            logger.debug(String.format("Backup storage[uuid:%s] has no IPs in storage network", bsUuid));
            return null;
        }

        final Set<String> ips = Arrays.stream(extraIps.split(","))
                .filter(ip -> NetworkUtils.isIpv4InCidr(ip, cidr))
                .collect(Collectors.toSet());
        if (ips.isEmpty()) {
            return null;
        }

        if (ips.size() != 1) {
            ips.remove(CoreGlobalProperty.MN_VIP);
        }

        return ips.iterator().next();
    }

    private void handle(final AllocateUploadWorkspaceMsg msg) {
        AllocateUploadWorkspaceReply reply = new AllocateUploadWorkspaceReply();
        AllocateUploadSpaceCmd cmd = new AllocateUploadSpaceCmd();
        cmd.setExpireHour(TimeUnit.SECONDS.toHours(LongJobGlobalConfig.LONG_JOB_DEFAULT_TIMEOUT.value(Long.class)));
        AllocateUploadSpaceResponse rsp = restf.syncJsonPost(buildUrl(ImageStoreBackupStorageConstant.ALLOCATE_UPLOAD_DIR),
                cmd, AllocateUploadSpaceResponse.class, TimeUnit.SECONDS, 10);
        if (!rsp.isSuccess()) {
            reply.setError(operr("%s", rsp.getError()));
        } else {
            reply.setUploadWorkspace(rsp.getUploadDir());
            reply.setBsInstallPath(self.getUrl());
        }

        bus.reply(msg, reply);
    }

    private void handle(final ExportNbdImagesMsg msg) {
        ExportNbdImagesReply reply = new ExportNbdImagesReply();
        ExportNbdImagesCmd cmd = new ExportNbdImagesCmd();
        cmd.setSizes(msg.getSizes());
        cmd.setWorkspace(msg.getWorkspace());
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.EXPORT_NBD_IMAGE), cmd, new JsonAsyncRESTCallback<ExportNbdImagesRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(ExportNbdImagesRsp rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                } else {
                    reply.setPorts(rsp.getPorts());
                    reply.setImagePaths(rsp.getImagePaths());
                    reply.setNbdDescription(rsp.getNbdDescription());
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<ExportNbdImagesRsp> getReturnClass() {
                return ExportNbdImagesRsp.class;
            }
        });
    }

    private void handle(final CancelExportNbdImagesMsg msg) {
        CancelExportNbdImagesReply reply = new CancelExportNbdImagesReply();
        CancelExportNbdImagesCmd cmd = new CancelExportNbdImagesCmd();
        cmd.setPorts(msg.getPorts());
        cmd.setImagePaths(msg.getImagePaths());
        cmd.setNbdDescription(msg.getNbdDescription());
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.CANCEL_EXPORT_NBD_IMAGE), cmd, new JsonAsyncRESTCallback<CancelExportNbdImagesRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CancelExportNbdImagesRsp rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<CancelExportNbdImagesRsp> getReturnClass() {
                return CancelExportNbdImagesRsp.class;
            }
        });
    }

    private void deleteDesignedProtocolExportedImage(final DeleteExportedImageFromImageStoreBackupStorageMsg msg) {
        DeleteExportedImageFromImageStoreBackupStorageReply reply = new DeleteExportedImageFromImageStoreBackupStorageReply();
        DeleteRemoteTargetCmd cmd = new DeleteRemoteTargetCmd();
        cmd.setTargetUri(msg.getRawPath());
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.DELETE_REMOTE_TARGET_PATH), cmd, new JsonAsyncRESTCallback<DeleteRemoteTargetRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteRemoteTargetRsp rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<DeleteRemoteTargetRsp> getReturnClass() {
                return DeleteRemoteTargetRsp.class;
            }
        });
    }

    private void handle(final GetImageChainInfoMsg msg) {
        GetImageChainInfoReply reply = new GetImageChainInfoReply();
        GetImageChainInfoCmd cmd = new GetImageChainInfoCmd();
        cmd.installPath = msg.getInstallPath();

        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.Get_IMAGE_CHAIN_INFO), cmd, new JsonAsyncRESTCallback<GetImageChainInfoResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(GetImageChainInfoResponse rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                } else {
                    reply.setChain(rsp.chain);
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<GetImageChainInfoResponse> getReturnClass() {
                return GetImageChainInfoResponse.class;
            }
        });
    }

    private void handle(final ImportImageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "import-image";
            }

            @Override
            public void run(SyncTaskChain chain) {
                doImport(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("import-image-%s", msg.getName());
            }

            @Override
            public int getSyncLevel() {
                return Math.max(1, ImageStoreBackupStorageGlobalProperty.UPLOAD_QUEUE_SIZE);
            }
        });
    }

    private void doImport(final ImportImageMsg msg, NoErrorCompletion completion) {
        ImportImageReply reply = new ImportImageReply();
        ImportImageCmd cmd = new ImportImageCmd();
        cmd.setParent(msg.getParent());
        cmd.imgurl = msg.getFilename();
        cmd.name = msg.getName();
        cmd.desc = msg.getDescription();
        cmd.processToRelease = msg.getProcessToRelease();

        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.IMPORT_BACKUP_PATH), cmd, new JsonAsyncRESTCallback<ImportImageResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(ImportImageResponse ret) {
                updateCapacity(ret.getTotalSize(), ret.getFreeSize());
                if (!ret.isSuccess()) {
                    reply.setError(operr("%s", ret.getError()));
                } else {
                    reply.setSize(ret.size);
                    reply.setInstallPath(buildInstallPath(ret.getName(), ret.getId()));
                }

                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<ImportImageResponse> getReturnClass() {
                return ImportImageResponse.class;
            }
        });
    }

    private void handle(CancelJobBackupStorageMsg msg) {
        CancelJobBackupStorageReply reply = new CancelJobBackupStorageReply();
        CancelJobCmd cmd = new CancelJobCmd();
        cmd.cancellationApiId = msg.getCancellationApiId();

        CancelJobRsp rsp = restf.syncJsonPost(buildUrl(ImageStoreBackupStorageConstant.SYNC_CANCEL_JOB), cmd, CancelJobRsp.class);
        if (!rsp.isSuccess()) {
            reply.setError(operr("operation error, because:%s", rsp.getError()));
        }

        bus.reply(msg, reply);
    }

    private void handle(ArchiveBackupStorageMsg msg) {
        ArchiveBackupStorageReply reply = new ArchiveBackupStorageReply();

        ArchiveStorageDataCmd cmd = new ArchiveStorageDataCmd();
        cmd.dstInstallPath = msg.getTargetInstallPath();
        cmd.dryRun = msg.isDryRun();
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.ARCHIVE_STORAGE_DATA), cmd, new JsonAsyncRESTCallback<ArchiveStorageDataRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(ArchiveStorageDataRsp rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                } else {
                    reply.setSize(rsp.size);
                }

                bus.reply(msg, reply);
            }

            @Override
            public Class<ArchiveStorageDataRsp> getReturnClass() {
                return ArchiveStorageDataRsp.class;
            }
        });
    }

    private void handle(UnpackBackupStorageMsg msg) {
        UnpackBackupStorageReply reply = new UnpackBackupStorageReply();

        FlowChain chain = new SimpleFlowChain();
        chain.setName("unpack-backup-storage-" + msg.getBackupStorageUuid());
        chain.then(new NoRollbackFlow() {
            String __name__ = "unpack-data";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                UnpackStorageDataCmd cmd = new UnpackStorageDataCmd();
                cmd.srcInstallPath = msg.getSrcInstallPath();
                restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.UNPACK_STORAGE_DATA), cmd, new JsonAsyncRESTCallback<UnpackStorageDataRsp>(msg) {
                    @Override
                    public void fail(ErrorCode err) {
                        trigger.fail(err);
                    }

                    @Override
                    public void success(UnpackStorageDataRsp rsp) {
                        if (!rsp.isSuccess()) {
                            trigger.fail(operr("%s", rsp.getError()));
                        } else {
                            trigger.next();
                        }
                    }

                    @Override
                    public Class<UnpackStorageDataRsp> getReturnClass() {
                        return UnpackStorageDataRsp.class;
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "after-unpack-extension";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                BackupStorageInventory inventory = getSelfInventory();
                ErrorCodeList errors = new ErrorCodeList();
                new While<>(pluginRgty.getExtensionList(AfterUnpackBackupStorageExtensionPoint.class)).each((ext, compl) -> {
                    ext.afterUnpackBackupStorage(inventory, new Completion(compl) {
                        @Override
                        public void success() {
                            compl.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            errors.getCauses().add(errorCode);
                            compl.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errors.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errors.getCauses().get(0));
                        }
                    }
                });
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).start();
    }

    protected RunInQueue inQueue() {
        return new RunInQueue(id, thdf, getImageStoreSyncLevel());
    }

    protected int getImageStoreSyncLevel() {
        return ImageStoreGlobalConfig.IMAGE_STORE_SYNC_LEVEL.value(Integer.class);
    }

    private void handle(final SyncBackupStorageDataMsg msg) {
        inQueue().name(String.format("sync-backup-storage-data-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> syncBackupStorageDataMsg(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void syncBackupStorageDataMsg(SyncBackupStorageDataMsg msg, final NoErrorCompletion completion) {
        SyncBackupStorageDataReply reply = new SyncBackupStorageDataReply();
        ImageStoreBackupStorageVO dstBs = Q.New(ImageStoreBackupStorageVO.class).eq(ImageStoreBackupStorageVO_.uuid, msg.getDstBackupStorageUuid()).find();

        SyncStorageDataCmd cmd = new SyncStorageDataCmd();
        cmd.hostname = dstBs.getHostname();
        cmd.installPath = dstBs.getUrl();
        cmd.username = dstBs.getUsername();
        cmd.password = dstBs.getPassword();
        cmd.sshPort = dstBs.getSshPort();
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.SYNC_STORAGE_DATA), cmd, new JsonAsyncRESTCallback<SyncStorageDataRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(SyncStorageDataRsp rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                }
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public Class<SyncStorageDataRsp> getReturnClass() {
                return SyncStorageDataRsp.class;
            }
        });
    }

    private void handle(final AllocateImageInstallPathMsg msg) {
        AllocateImageInstallPathReply reply = new AllocateImageInstallPathReply();
        AllocateImageStoreInstallPathCmd cmd = new AllocateImageStoreInstallPathCmd();
        cmd.parent = msg.getParentInstallPath();

        AllocateImageStoreInstallPathResponse rsp = restf.syncJsonPost(buildUrl(ImageStoreBackupStorageConstant.INSTALL_PATH_ALLOCATE),
                cmd, AllocateImageStoreInstallPathResponse.class, TimeUnit.MINUTES, 1);
        if (!rsp.isSuccess()) {
            reply.setError(operr("%s", rsp.getError()));
        } else {
            reply.setName(getNameFromInstallPath(rsp.installPath));
            reply.setInstallPath(rsp.installPath);
        }
        bus.reply(msg, reply);
    }

    private void handle(UploadImageToRemoteTargetMsg msg) {
        String installPath = msg.getImage().getBackupStorageRefs().stream()
                .filter(ref -> ref.getBackupStorageUuid().equals(self.getUuid()))
                .findFirst().orElseThrow(() ->
                        new OperationFailureException(operr("miss image path on bs[%s]", self.getUuid()))
                ).getInstallPath();

        UploadImageToRemoteTargetReply reply = new UploadImageToRemoteTargetReply();
        UploadImageToRemoteTargetCmd cmd = new UploadImageToRemoteTargetCmd();
        cmd.setInstallPath(installPath);
        cmd.setRemoteTargetUrl(msg.getRemoteTargetUrl());
        cmd.setFormat(msg.getFormat());
        cmd.setImageFormat(msg.getImage().getFormat());
        // download means download from BS, so the concurrency is download concurrency
        cmd.setConcurrency(ImageStoreGlobalConfig.BLOB_DOWNLOAD_CONCURRENCY.value(Integer.class));
        uploadToRemote(cmd, new Completion(msg) {
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

    private void uploadToRemote(UploadImageToRemoteTargetCmd cmd, Completion completion) {
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.UPLOAD_IMAGE_TO_REMOTE_PATH), cmd, new JsonAsyncRESTCallback<UploadImageToRemoteTargetRsp>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(UploadImageToRemoteTargetRsp rsp) {
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }

                completion.success();
            }

            @Override
            public Class<UploadImageToRemoteTargetRsp> getReturnClass() {
                return UploadImageToRemoteTargetRsp.class;
            }
        });
    }

    private void handle(DownloadImageFromRemoteTargetMsg msg) {
        DownloadImageFromRemoteTargetReply reply = new DownloadImageFromRemoteTargetReply();

        download(buildDownloadCmd(msg), new ReturnValueCompletion<DownloadResult>(msg) {
            @Override
            public void success(DownloadResult ret) {
                reply.setInstallPath(ret.installPath);
                reply.setSize(ret.virtualSize);
                reply.setActualSize(ret.diskSize);
                reply.setFormat(ret.format);
                reply.setMd5sum(ret.md5sum);
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
    protected void handle(GetBackupStorageManagerHostnameMsg msg) {
        GetBackupStorageManagerHostnameReply reply = new GetBackupStorageManagerHostnameReply();
        reply.setHostname(getSelf().getHostname());
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final UploadFileToBackupStorageHostMsg msg) {
        UploadFileToBackupStorageHostReply reply = new UploadFileToBackupStorageHostReply();
        if (StringUtils.isEmpty(msg.getUrl())) {
            reply.setError(operr("url cannot be null or empty"));
            bus.reply(msg, reply);
            return;
        }

        // Validate installPath to prevent path traversal and injection attacks.
        if (msg.getInstallPath() != null) {
            String pathErr = RemotePathValidator.validateRemotePath(msg.getInstallPath(), "installPath");
            if (pathErr != null) {
                reply.setError(operr(pathErr));
                bus.reply(msg, reply);
                return;
            }
        }

        // "upload://" scheme: the caller will push file data directly to the agent's upload endpoint.
        // The agent returns a directUploadUrl that the caller uses for the actual data transfer.
        if (msg.getUrl().startsWith("upload://")) {
            UploadFileCmd cmd = new UploadFileCmd();
            cmd.url = msg.getUrl();
            cmd.installPath = msg.getInstallPath();
            cmd.timeout = timeoutManager.getTimeout();
            cmd.taskUuid = msg.getTaskUuid();
            restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.FILE_UPLOAD_PATH), cmd,
                    new JsonAsyncRESTCallback<UploadFileResponse>(msg) {
                @Override
                public void fail(ErrorCode err) {
                    reply.setError(err);
                    bus.reply(msg, reply);
                }

                @Override
                public void success(UploadFileResponse ret) {
                    if (!ret.isSuccess()) {
                        reply.setError(operr("operation error, because:%s", ret.getError()));
                    } else {
                        reply.setDirectUploadUrl(ret.directUploadUrl);
                        reply.setBackupStorageHostUuid(getSelf().getUuid());
                    }
                    bus.reply(msg, reply);
                }

                @Override
                public Class<UploadFileResponse> getReturnClass() {
                    return UploadFileResponse.class;
                }
            });
            return;
        }

        // Other URL schemes (http://, https://, ftp://, etc.): the agent pulls the file
        // from the given URL. Used for remote download scenarios where the file is
        // hosted on an accessible server.
        DownloadFileCmd cmd = new DownloadFileCmd();
        cmd.url = msg.getUrl();
        cmd.installPath = msg.getInstallPath();
        cmd.timeout = timeoutManager.getTimeout();
        cmd.taskUuid = msg.getTaskUuid();
        cmd.sendCommandUrl = restf.getSendCommandUrl();

        String[] urlResult = RemotePathValidator.validateAndExtractUrlScheme(msg.getUrl());
        if (urlResult[0] != null) {
            reply.setError(operr(urlResult[0]));
            bus.reply(msg, reply);
            return;
        }
        cmd.urlScheme = urlResult[1];

        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.FILE_DOWNLOAD_PATH), cmd,
                new JsonAsyncRESTCallback<DownloadFileResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DownloadFileResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setError(operr("operation error, because:%s", ret.getError()));
                } else {
                    reply.setMd5sum(ret.md5sum);
                    reply.setSize(ret.size);
                    reply.setFormat(ret.format);
                    reply.setBackupStorageHostUuid(getSelf().getUuid());
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<DownloadFileResponse> getReturnClass() {
                return DownloadFileResponse.class;
            }
        });
    }

    @Override
    protected void handle(final UnzipFileOnBackupStorageHostMsg msg) {
        UnzipFileOnBackupStorageHostReply reply = new UnzipFileOnBackupStorageHostReply();

        if (StringUtils.isEmpty(msg.getInstallPath())) {
            reply.setError(operr("installPath cannot be null or empty"));
            bus.reply(msg, reply);
            return;
        }

        String pathErr = RemotePathValidator.validateRemotePath(msg.getInstallPath(), "installPath");
        if (pathErr != null) {
            reply.setError(operr(pathErr));
            bus.reply(msg, reply);
            return;
        }

        UnzipFileCmd cmd = new UnzipFileCmd();
        cmd.installPath = msg.getInstallPath();

        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.UNZIP_FILE_PATH), cmd,
                new JsonAsyncRESTCallback<UnzipFileResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(UnzipFileResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setError(operr("operation error, because:%s", ret.getError()));
                } else {
                    reply.setUnzipInstallPath(ret.unzipInstallPath);
                    reply.setFileSizes(ret.fileSizes);
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<UnzipFileResponse> getReturnClass() {
                return UnzipFileResponse.class;
            }
        });
    }

    @Override
    protected void handle(final DeleteFilesOnBackupStorageHostMsg msg) {
        DeleteFilesOnBackupStorageHostReply reply = new DeleteFilesOnBackupStorageHostReply();

        if (msg.getFilePaths() == null || msg.getFilePaths().isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        // Validate each file path to prevent path traversal and injection attacks.
        String filePathErr = RemotePathValidator.validateFilePaths(msg.getFilePaths());
        if (filePathErr != null) {
            reply.setError(operr(filePathErr));
            bus.reply(msg, reply);
            return;
        }

        DeleteFilesCmd cmd = new DeleteFilesCmd();
        cmd.filePaths = msg.getFilePaths();

        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.DELETE_FILES_PATH), cmd,
                new JsonAsyncRESTCallback<DeleteFilesResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteFilesResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setError(operr("operation error, because:%s", ret.getError()));
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<DeleteFilesResponse> getReturnClass() {
                return DeleteFilesResponse.class;
            }
        });
    }

    @Override
    protected void handle(GetFileDownloadProgressFromBackupStorageHostMsg msg) {
        GetFileDownloadProgressFromBackupStorageHostReply reply = new GetFileDownloadProgressFromBackupStorageHostReply();

        if (msg.getTaskUuid() == null || msg.getTaskUuid().isEmpty()) {
            reply.setError(operr("taskUuid cannot be null or empty"));
            bus.reply(msg, reply);
            return;
        }

        GetDownloadFileProgressCmd cmd = new GetDownloadFileProgressCmd();
        cmd.taskUuid = msg.getTaskUuid();

        GetDownloadFileProgressResponse ret = restf.syncJsonPost(
                buildUrl(ImageStoreBackupStorageConstant.FILE_DOWNLOAD_PROGRESS_PATH),
                cmd,
                GetDownloadFileProgressResponse.class, TimeUnit.MINUTES, 1);
        if (ret == null) {
            reply.setError(operr("No response"));
            bus.reply(msg, reply);
            return;
        }

        if (!ret.isSuccess()) {
            reply.setError(operr("operation error, because:%s", ret.getError()));
        } else {
            reply.setCompleted(ret.completed);
            reply.setProgress(ret.progress);
            reply.setActualSize(ret.actualSize);
            reply.setSize(ret.size);
            reply.setInstallPath(ret.installPath);
            reply.setDownloadSize(ret.downloadSize);
            reply.setLastOpTime(ret.lastOpTime);
            reply.setMd5sum(ret.md5sum);
            reply.setSupportSuspend(ret.supportSuspend);
            reply.setFormat(ret.format);
        }
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(SoftwareUpgradePackageDeployMsg msg) {
        SoftwareUpgradePackageDeployReply reply = new SoftwareUpgradePackageDeployReply();

        if (msg.getUpgradePackagePath() == null || msg.getUpgradePackagePath().isEmpty()) {
            reply.setError(operr("upgradePackagePath cannot be null or empty"));
            bus.reply(msg, reply);
            return;
        }

        if (msg.getTargetHostIp() == null || msg.getTargetHostIp().isEmpty()) {
            reply.setError(operr("targetHostIp cannot be null or empty"));
            bus.reply(msg, reply);
            return;
        }

        if (msg.getTargetHostSshPort() <= 0 || msg.getTargetHostSshPort() > 65535) {
            reply.setError(operr("targetHostSshPort must be in range 1-65535, but got [%d]", msg.getTargetHostSshPort()));
            bus.reply(msg, reply);
            return;
        }

        if (!NetworkUtils.isValidIPAddress(msg.getTargetHostIp())) {
            reply.setError(operr("targetHostIp [%s] is not a valid IPv4 or IPv6 address", msg.getTargetHostIp()));
            bus.reply(msg, reply);
            return;
        }

        String usernameErr = RemotePathValidator.validateSshUsername(msg.getTargetHostSshUsername());
        if (usernameErr != null) {
            reply.setError(operr(usernameErr));
            bus.reply(msg, reply);
            return;
        }

        // Validate all paths to prevent path traversal and injection attacks.
        // upgradePackageTargetPath and upgradeScriptPath are required by the agent,
        // so they must not be null or empty.
        String pathErr = RemotePathValidator.validateRemotePath(msg.getUpgradePackagePath(), "upgradePackagePath");
        if (pathErr != null) {
            reply.setError(operr(pathErr));
            bus.reply(msg, reply);
            return;
        }
        pathErr = RemotePathValidator.validateRemotePath(msg.getUpgradePackageTargetPath(), "upgradePackageTargetPath");
        if (pathErr != null) {
            reply.setError(operr(pathErr));
            bus.reply(msg, reply);
            return;
        }
        pathErr = RemotePathValidator.validateRemotePath(msg.getUpgradeScriptPath(), "upgradeScriptPath");
        if (pathErr != null) {
            reply.setError(operr(pathErr));
            bus.reply(msg, reply);
            return;
        }

        SoftwareUpgradePackageCmd cmd = new SoftwareUpgradePackageCmd();
        cmd.upgradePackagePath = msg.getUpgradePackagePath();
        cmd.upgradePackageTargetPath = msg.getUpgradePackageTargetPath();
        cmd.upgradeScriptPath = msg.getUpgradeScriptPath();
        cmd.targetHostSshPort = msg.getTargetHostSshPort();
        cmd.targetHostSshUsername = msg.getTargetHostSshUsername();
        cmd.targetHostSshPassword = msg.getTargetHostSshPassword();
        cmd.targetHostIp = msg.getTargetHostIp();
        cmd.softwareType = msg.getSoftwareType();

        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.SOFTWARE_UPGRADE_PACKAGE_DEPLOY_PATH), cmd,
                new JsonAsyncRESTCallback<SoftwareUpgradePackageResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(SoftwareUpgradePackageResponse ret) {
                if (!ret.isSuccess()) {
                    reply.setError(operr("operation error, because:%s", ret.getError()));
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<SoftwareUpgradePackageResponse> getReturnClass() {
                return SoftwareUpgradePackageResponse.class;
            }
        });
    }

    @Override
    protected void handle(CancelDownloadFileOnBackupStorageHostMsg msg) {
        CancelDownloadFileOnBackupStorageHostReply reply = new CancelDownloadFileOnBackupStorageHostReply();

        CancelDownloadFileCmd cmd = new CancelDownloadFileCmd();
        cmd.cancellationApiId = msg.getCancellationApiId();

        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.FILE_CANCEL_PATH), cmd,
                new JsonAsyncRESTCallback<CancelDownloadFileRsp>(msg) {
                    @Override
                    public void fail(ErrorCode err) {
                        // Fallback to old /canceljob-sync/ path for backward compatibility
                        // with older agents that don't have /imagestore/file/cancel endpoint.
                        logger.warn(String.format("cancel via new path failed (agent may be old version), " +
                                "falling back to %s: %s", ImageStoreBackupStorageConstant.SYNC_CANCEL_JOB, err));
                        try {
                            CancelJobCmd fallbackCmd = new CancelJobCmd();
                            fallbackCmd.cancellationApiId = msg.getCancellationApiId();
                            CancelJobRsp rsp = restf.syncJsonPost(
                                    buildUrl(ImageStoreBackupStorageConstant.SYNC_CANCEL_JOB),
                                    fallbackCmd, CancelJobRsp.class);
                            if (!rsp.isSuccess()) {
                                reply.setError(operr("operation error, because:%s", rsp.getError()));
                            }
                        } catch (Exception e) {
                            reply.setError(operr("cancel fallback also failed: %s", e.getMessage()));
                        }
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void success(CancelDownloadFileRsp ret) {
                        if (!ret.isSuccess()) {
                            reply.setError(operr("operation error, because:%s", ret.getError()));
                        }
                        bus.reply(msg, reply);
                    }

                    @Override
                    public Class<CancelDownloadFileRsp> getReturnClass() {
                        return CancelDownloadFileRsp.class;
                    }
                });
    }
}
