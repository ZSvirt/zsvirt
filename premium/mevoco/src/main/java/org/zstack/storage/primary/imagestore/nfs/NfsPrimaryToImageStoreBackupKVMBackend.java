package org.zstack.storage.primary.imagestore.nfs;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.backup.imagestore.GetImageStoreBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.imagestore.GetImageStoreBackupStorageDownloadCredentialReply;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;
import org.zstack.storage.backup.imagestore.ImageStoreGlobalConfig;
import org.zstack.storage.primary.imagestore.local.LocalStorageKvmImageStoreBackupStorageMediatorImpl;
import org.zstack.storage.primary.nfs.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

public class NfsPrimaryToImageStoreBackupKVMBackend implements NfsPrimaryToBackupStorageMediator {
    private static final CLogger logger = Utils.getLogger(NfsPrimaryToImageStoreBackupKVMBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private NfsPrimaryStorageFactory primaryStorageFactory;
    @Autowired
    private NfsPrimaryStorageManager nfsMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ResourceConfigFacade rcf;

    public static final String UPLOAD_TO_IMAGESTORE_PATH = "/nfsprimarystorage/imagestore/upload";
    public static final String DOWNLOAD_FROM_IMAGESTORE_PATH = "/nfsprimarystorage/imagestore/download";

    public static class ImageStoreCmd extends NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentCommand {
        protected String username;
        protected String hostname;

        protected String backupStorageInstallPath;
        protected String primaryStorageInstallPath;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
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


    public static class DownloadFromImageStoreCmd extends ImageStoreCmd {
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

    public static class DownloadFromImageStoreRsp extends NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse {
    }

    public static class ImageStoreUploadBitsCmd extends ImageStoreCmd implements HasThreadContext {
        private String imageUuid;
        private String description;
        private int concurrency = ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY.value(Integer.class);

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public int getConcurrency() {
            return concurrency;
        }
    }

    public static class ImageStoreUploadBitsRsp extends NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse {
        public String backupStorageInstallPath;
    }

    @Override
    public String getSupportedPrimaryStorageType() {
        return NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public String getSupportedBackupStorageType() {
        return ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE;
    }

    @Override
    public List<String> getSupportedHypervisorTypes() {
        return list(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public String makeVolumeSnapshotInstallPath(String bsUuid, String snapshotUuid) {
        return null;
    }

    @Override
    public String makeRootVolumeTemplateInstallPath(String bsUuid, String imageUuid) {
        return null;
    }

    @Override
    public String makeDataVolumeTemplateInstallPath(String backupStorageUuid, String volumeUuid) {
        return null;
    }

    @Override
    public void downloadBits(final PrimaryStorageInventory pinv, final BackupStorageInventory bsinv, final String backupStorageInstallPath, final String primaryStorageInstallPath, boolean isData, final Completion completion) {
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
                final HostInventory host = primaryStorageFactory.getConnectedHostForOperation(pinv).get(0);
                final String hostUuid = host.getUuid();
                DownloadFromImageStoreCmd cmd = new DownloadFromImageStoreCmd();
                cmd.setUuid(pinv.getUuid());
                cmd.setHostname(greply.getHostname());
                cmd.setUsername(greply.getUsername());
                cmd.setBackupStorageInstallPath(backupStorageInstallPath);
                cmd.setPrimaryStorageInstallPath(primaryStorageInstallPath);
                if (isData) {
                    cmd.setIsData(1);
                }
                cmd.concurrency = rcf.getResourceConfigValue(ImageStoreGlobalConfig.BLOB_DOWNLOAD_CONCURRENCY, bsinv.getUuid(), Integer.class);

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(hostUuid);
                msg.setPath(DOWNLOAD_FROM_IMAGESTORE_PATH);
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
                        DownloadFromImageStoreRsp rsp = kr.toResponse(DownloadFromImageStoreRsp.class);
                        if (!rsp.isSuccess()) {
                            completion.fail(operr("failed to download bits from the imagestore backup storage[hostname:%s, path: %s] to the nfs primary storage[uuid:%s, path: %s], %s",
                                            greply.getHostname(), backupStorageInstallPath, pinv.getUuid(), primaryStorageInstallPath, rsp.getError()));
                            return;
                        }

                        completion.success();
                    }
                });
            }
        });
    }


    @Override
    public void uploadBits(final String imageUuid, final PrimaryStorageInventory pinv, BackupStorageInventory bsinv, final String backupStorageInstallPath, final String primaryStorageInstallPath, final ReturnValueCompletion<String> completion) {
        ImageVO ivo = dbf.findByUuid(imageUuid, ImageVO.class);
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
                final HostInventory host = primaryStorageFactory.getConnectedHostForOperation(pinv).get(0);
                final String hostUuid = host.getUuid();
                ImageStoreUploadBitsCmd cmd = new ImageStoreUploadBitsCmd();
                cmd.setPrimaryStorageInstallPath(primaryStorageInstallPath);
                cmd.setBackupStorageInstallPath(backupStorageInstallPath);
                cmd.setHostname(r.getHostname());
                cmd.setUsername(r.getUsername());
                cmd.setImageUuid(imageUuid);
                StringBuilder desc = new StringBuilder();
                for (CreateImageExtensionPoint ext : pluginRgty.getExtensionList(CreateImageExtensionPoint.class)) {
                    String tmp = ext.getImageDescription(ImageInventory.valueOf(ivo));
                    if (tmp != null && !tmp.trim().equals("")) {
                        desc.append(tmp);
                    }
                }
                cmd.setDescription(desc.toString());
                cmd.concurrency = rcf.getResourceConfigValue(ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY, bsinv.getUuid(), Integer.class);

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setCommand(cmd);
                msg.setPath(UPLOAD_TO_IMAGESTORE_PATH);
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
                            completion.fail(operr("failed to upload bits from the NFS[uuid:%s, path:%s] to image store [hostname:%s], %s",
                                    pinv.getUuid(), primaryStorageInstallPath, r.getHostname(), rsp.getError()));
                            return;
                        }

                        completion.success(rsp.backupStorageInstallPath);
                    }
                });
            }
        });
    }
}
