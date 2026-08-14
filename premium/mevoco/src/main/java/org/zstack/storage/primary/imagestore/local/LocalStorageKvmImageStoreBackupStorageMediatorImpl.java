package org.zstack.storage.primary.imagestore.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.HasThreadContext;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.image.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.backup.imagestore.*;
import org.zstack.storage.primary.local.LocalStorageBackupStorageMediator;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStorageKvmBackend;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.i18n;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class LocalStorageKvmImageStoreBackupStorageMediatorImpl implements LocalStorageBackupStorageMediator {
    private static final CLogger logger = Utils.getLogger(LocalStorageKvmImageStoreBackupStorageMediatorImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ResourceConfigFacade rcf;

    // c.f. corresponding handlers in image store plugin for the kvmagent.
    public static final String UPLOAD_BIT_PATH = "/localstorage/imagestore/upload";
    public static final String DOWNLOAD_BIT_PATH = "/localstorage/imagestore/download";

    public static class ImageStoreCmd extends LocalStorageKvmBackend.AgentCommand {
        private String privateKey;
        private String trustedHosts;
        private String username;
        private String hostname;
        private int servicePort;

        private String backupStorageInstallPath;
        private String primaryStorageInstallPath;

        public String getTrustedHosts() {
            return trustedHosts;
        }

        public void setTrustedHosts(String hosts) {
            this.trustedHosts = hosts;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getServicePort() {
            return servicePort;
        }

        public void setServicePort(int servicePort) {
            this.servicePort = servicePort;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }
    }

    public static class ImageStoreDownloadBitsCmd extends ImageStoreCmd {
        private int isData;
        private int concurrency = ImageStoreGlobalConfig.BLOB_DOWNLOAD_CONCURRENCY.value(Integer.class);

        public int getIsData() {
            return isData;
        }

        public void setIsData(int isData) {
            this.isData = isData;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public int getConcurrency() {
            return concurrency;
        }
    }

    public static class ImageStoreDownloadBitsRsp extends LocalStorageKvmBackend.AgentResponse {
    }

    public static class ImageStoreUploadBitsCmd extends ImageStoreCmd implements HasThreadContext {
        String imageUuid;
        String description;
        int concurrency = ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY.value(Integer.class);

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public int getConcurrency() {
            return concurrency;
        }
    }

    public static class ImageStoreUploadBitsRsp extends LocalStorageKvmBackend.AgentResponse {
        public String backupStorageInstallPath;
    }

    public void downloadBits(final PrimaryStorageInventory pinv, BackupStorageInventory bsinv, final String backupStorageInstallPath, final String primaryStorageInstallPath, final String hostUuid, boolean isData, final Completion completion) {
        GetImageStoreBackupStorageDownloadCredentialMsg gmsg = new GetImageStoreBackupStorageDownloadCredentialMsg();
        gmsg.setBackupStorageUuid(bsinv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsinv.getUuid());
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                final GetImageStoreBackupStorageDownloadCredentialReply greply = reply.castReply();
                ImageStoreDownloadBitsCmd cmd = new ImageStoreDownloadBitsCmd();
                cmd.setHostname(greply.getHostname());
                cmd.setUsername(greply.getUsername());
                cmd.setBackupStorageInstallPath(backupStorageInstallPath);
                cmd.setPrimaryStorageInstallPath(primaryStorageInstallPath);
                cmd.storagePath = pinv.getUrl();
                if (isData) {
                    cmd.setIsData(1);
                }
                cmd.concurrency = rcf.getResourceConfigValue(ImageStoreGlobalConfig.BLOB_DOWNLOAD_CONCURRENCY, bsinv.getUuid(), Integer.class);

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(hostUuid);
                msg.setPath(DOWNLOAD_BIT_PATH);
                msg.setCommand(cmd);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply kr = reply.castReply();
                        ImageStoreDownloadBitsRsp rsp = kr.toResponse(ImageStoreDownloadBitsRsp.class);
                        if (!rsp.isSuccess()) {

                            // fix issues 1706
                            String operationSuggestion = (greply.getHostname() != null && greply.getHostname().startsWith("127")) ?
                                    i18n("System can't find imagestore backup Storage. Please do not set imagestore backup Storage server IP to localhost(127.*.*.*),") :
                                    "";

                            completion.fail(operr("%s failed to download bits from the imagestore backup storage[hostname:%s, path: %s] to the local primary storage[uuid:%s, path: %s], %s",
                                            operationSuggestion, greply.getHostname(), backupStorageInstallPath, pinv.getUuid(), primaryStorageInstallPath, rsp.getError()));
                            return;
                        }

                        completion.success();
                    }
                });
            }
        });
    }

    @Override
    public void uploadBits(final String imageUuid, final PrimaryStorageInventory pinv, BackupStorageInventory bsinv, final String backupStorageInstallPath, final String primaryStorageInstallPath, final String hostUuid, final ReturnValueCompletion<String> completion) {
        GetImageStoreBackupStorageDownloadCredentialMsg gmsg = new GetImageStoreBackupStorageDownloadCredentialMsg();
        gmsg.setBackupStorageUuid(bsinv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsinv.getUuid());
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                final GetImageStoreBackupStorageDownloadCredentialReply r = reply.castReply();
                ImageStoreUploadBitsCmd cmd = new ImageStoreUploadBitsCmd();
                cmd.setPrimaryStorageInstallPath(primaryStorageInstallPath);
                cmd.setBackupStorageInstallPath(backupStorageInstallPath);
                cmd.setHostname(r.getHostname());
                cmd.setUsername(r.getUsername());
                ImageVO ivo = dbf.findByUuid(imageUuid, ImageVO.class);
                cmd.setImageUuid(ivo.getUuid());
                cmd.storagePath = pinv.getUrl();
                cmd.concurrency = rcf.getResourceConfigValue(ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY, bsinv.getUuid(), Integer.class);

                StringBuilder desc = new StringBuilder();
                for (CreateImageExtensionPoint ext : pluginRgty.getExtensionList(CreateImageExtensionPoint.class)) {
                    String tmp = ext.getImageDescription(ImageInventory.valueOf(ivo));
                    if (tmp != null && !tmp.trim().equals("")) {
                        desc.append(tmp);
                    }
                }
                cmd.setDescription(desc.toString());

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setCommand(cmd);
                msg.setPath(UPLOAD_BIT_PATH);
                msg.setHostUuid(hostUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply kr = reply.castReply();
                        ImageStoreUploadBitsRsp rsp = kr.toResponse(ImageStoreUploadBitsRsp.class);
                        if (!rsp.isSuccess()) {
                            completion.fail(operr("failed to upload bits from the local storage[uuid:%s, path:%s] to image store [hostname:%s], %s",
                                            pinv.getUuid(), primaryStorageInstallPath, r.getHostname(), rsp.getError()));
                            return;
                        }

                        completion.success(rsp.backupStorageInstallPath);
                    }
                });
            }
        });
    }

    @Override
    public String getSupportedPrimaryStorageType() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE;
    }

    @Override
    public String getSupportedBackupStorageType() {
        return ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE;
    }

    @Override
    public List<String> getSupportedHypervisorTypes() {
        return list(KVMConstant.KVM_HYPERVISOR_TYPE);
    }
}
