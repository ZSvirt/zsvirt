package org.zstack.storage.primary.block;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.image.CreateImageExtensionPoint;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.ResizeVolumeOnPrimaryStorageReply;
import org.zstack.kvm.*;
import org.zstack.sdk.IscsiServerInventory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.*;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/3/31 13:51
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class BlockPrimaryStorageKvmCommandDispatcher {
    private static final CLogger logger = Utils.getLogger(BlockPrimaryStorageKvmCommandDispatcher.class);

    @Autowired
    protected CloudBus bus;

    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    protected PluginRegistry pluginRegistry;


    public static class AgentRsp {
        public boolean success = true;
        public String error;
        public long totalCapacity;
        public long availableCapacity;
    }

    public static class AgentCmd {
        public String uuid;
    }

    public static class GetInitiatorNameRsp extends AgentRsp {
        public String hostUuid;
        public String initiatorName;
    }

    public static class DiscoverLunCmd extends AgentCmd {
        public String wwn;
        public String target;
        public String iscsiServerIp;
        public Integer iscsiServerPort;
        public String iscsiServerChapUserName;
        public String iscsiServerChapUserPassword;

        public DiscoverLunCmd() {}

        public DiscoverLunCmd(BlockScsiLunVO blockScsiLunVO) {
            this.wwn = blockScsiLunVO.getWwn();
            this.target = blockScsiLunVO.getTarget();
        }

        public void setIscsiServer(IscsiServerInventory isciServer) {
            this.iscsiServerIp = isciServer.getIp();
            this.iscsiServerPort = isciServer.getPort();
            this.iscsiServerChapUserName = isciServer.getChapUserName();
            this.iscsiServerChapUserPassword = isciServer.getChapUserPassword();
        }

        public void setTarget(String target) {
            this.target = target;
        }
    }

    public static class LogoutTargetCmd extends DiscoverLunCmd {
        public LogoutTargetCmd(BlockScsiLunVO blockScsiLunVO){
            super(blockScsiLunVO);
        }
    }

    public static class CreateHeartbeatCmd extends DiscoverLunCmd {
        public String heartbeatInstallPath;

        public CreateHeartbeatCmd(BlockScsiLunVO other) {
            super(other);
            this.heartbeatInstallPath = other.getInstallPath();
        }
        public void setHeartbeatInstallPath(String heartbeatInstallPath) {
            this.heartbeatInstallPath = heartbeatInstallPath;
        }
    }

    public static class DeleteHeartbeatCmd extends AgentCmd {
        public String heartbeatPath;

        public void setHeartbeatPath(String heartbeatPath) {
            this.heartbeatPath = heartbeatPath;
        }
    }

    public static class KvmSetupSelfFencerCmd extends AgentCmd {
        public long interval;
        public int maxAttempts;
        public String mountPath;
        public String installPath;
        public int storageCheckerTimeout;
        public String heartbeat;
        public String strategy;
    }

    public static class KvmCancelSelfFencerCmd extends AgentCmd {
    }

    public static class CommitVolumeAsImageCmd extends AgentCmd {
        private String primaryStorageInstallPath;
        private String description;
        private String hostname; // the hostname of the image store backup storage
        private String imageUuid;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class RescanLunCmd extends AgentCmd {
        private String installPath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class ResizeVolumeCmd extends RescanLunCmd{
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class PingAgentNoFailureCmd extends AgentCmd {
        private List<String> psHeartbeatLunInstallPath;

        public List<String> getPsHeartbeatInstallPath() {
            return psHeartbeatLunInstallPath;
        }

        public void setPsHeartbeatInstallPath(List<String> psHeartbeatInstallPath) {
            this.psHeartbeatLunInstallPath = psHeartbeatInstallPath;
        }
    }

    public static class ResizeVolumeRsp extends AgentRsp {
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class PingAgentNoFailureRsp extends AgentRsp {
        public List<String> disconnectedPSInstallPath;

        public List<String> getDisconnectedPSInstallPath() {
            return disconnectedPSInstallPath;
        }

        public void setDisconnectedPSInstallPath(List<String> disconnectedPSInstallPath) {
            this.disconnectedPSInstallPath = disconnectedPSInstallPath;
        }
    }

    public static class CommitVolumeAsImageRsp extends AgentRsp {
        private String backupStorageInstallPath;
        private long size;
        private long actualSize;

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class MountImageCacheLunCmd extends AgentCmd {
        private String lunInstallPath;
        private String mountPath;

        public void setLunInstallPath(String lunInstallPath) {
            this.lunInstallPath = lunInstallPath;
        }

        public void setMountPath(String mountPath) {
            this.mountPath = mountPath;
        }
    }

    public static class UmountPathCmd extends AgentCmd {
        private String path;

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class ConvertImageToLunCmd extends AgentCmd {
        private String imagePath;
        private String lunInstallPath;

        public void setLunInstallPath(String lunInstallPath) {
            this.lunInstallPath = lunInstallPath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }
    }

    public static final String GET_INITIATOR_NAME_PATH = "/block/primarystorage/getinitiatorname";
    public static final String CREATE_HEARTBEAT_PATH = "/block/primarystorage/createheartbeat";
    public static final String DELETE_HEARTBEAT_PATH = "/block/primarystorage/deleteheartbeat";
    public static final String DISCOVER_LUN_PATH = "/block/primarystorage/discoverlun";
    public static final String LOGOUT_TARGET_PATH = "/block/primarystorage/logouttarget";
    public static final String COMMIT_VOLUME_AS_IMAGE_PATH = "/block/imagestore/commit";
    public static final String KVM_HA_SETUP_SELF_FENCER = "/ha/block/setupselffencer";
    public static final String KVM_HA_CANCEL_SELF_FENCER = "/ha/block/cancelselffencer";
    public static final String RESIZE_VOLUME_PATH = "/block/primarystorage/volume/resize";
    public static final String PING_AGENT_NO_FAILURE_PATH = "/block/primarystorage/ping";
    public static final String RESCAN_LUN_PATH = "/block/primarystorage/lun/rescan";
    public static final String MOUNT_TEMP_IMAGE_CACHE_LUN = "/block/primarystorage/imagecache/lun/mount";
    public static final String CONVERT_IMAGE_CACHE_TO_LUN = "/block/primarystorage/imagecache/convert";
    public static final String UMOUNT_PATH = "/block/primarystorage/unmount";

    protected <T extends AgentRsp> void httpCall(String path, final String hostUuid, AgentCmd cmd, final Class<T> respType, final ReturnValueCompletion<T> completion) {
        httpCall(path, hostUuid, cmd, false, respType, completion);
    }

    protected <T extends AgentRsp> void httpCall(String path, final String hostUuid, AgentCmd cmd, boolean noCheckStatus, final Class<T> respType, final ReturnValueCompletion<T> completion) {
        DebugUtils.Assert(hostUuid != null, "Host must be set here");
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                KVMHostAsyncHttpCallReply rep = reply.castReply();
                final T rsp = rep.toResponse(respType);
                if (!rsp.success) {
                    completion.fail(operr("operation error, because:%s", rsp.error));
                    return;
                }
                completion.success(rsp);
            }
        });
    }

    public void umountPath(String path, String hostUuid, final Completion completion) {
        UmountPathCmd cmd = new UmountPathCmd();
        cmd.setPath(path);
        httpCall(UMOUNT_PATH, hostUuid, cmd, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void mountTempImageCacheLun(String lunInstallPath, String mountPath, String hostUuid, final Completion completion) {
        MountImageCacheLunCmd cmd = new MountImageCacheLunCmd();
        cmd.setLunInstallPath(lunInstallPath);
        cmd.setMountPath(mountPath);
        httpCall(MOUNT_TEMP_IMAGE_CACHE_LUN, hostUuid, cmd, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void convertImageToLun(String imagePath, String lunInstallPath, String hostuuid, final Completion completion) {
        ConvertImageToLunCmd cmd = new ConvertImageToLunCmd();
        cmd.setImagePath(imagePath);
        cmd.setLunInstallPath(lunInstallPath);
        httpCall(CONVERT_IMAGE_CACHE_TO_LUN, hostuuid, cmd, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void setKvmHaCancelSelfFencer(KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam param, final Completion completion) {
        KvmCancelSelfFencerCmd cmd = new KvmCancelSelfFencerCmd();
        cmd.uuid = param.getPrimaryStorage().getUuid();

        httpCall(KVM_HA_CANCEL_SELF_FENCER, param.getHostUuid(), cmd, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void setKvmHaSetupSelfFencer(KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam param, String installPath, final Completion completion) {
        KvmSetupSelfFencerCmd cmd = new KvmSetupSelfFencerCmd();
        cmd.interval = param.getInterval();
        cmd.uuid = param.getPrimaryStorage().getUuid();
        cmd.maxAttempts = param.getMaxAttempts();
        cmd.heartbeat = "heartbeat";
        cmd.mountPath = param.getPrimaryStorage().getMountPath();
        cmd.storageCheckerTimeout = param.getStorageCheckerTimeout();
        cmd.strategy = param.getStrategy();
        cmd.installPath = installPath;

        httpCall(KVM_HA_SETUP_SELF_FENCER, param.getHostUuid(), cmd, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void rescanLun(String hostUuid, String installPath, final Completion completion) {
        RescanLunCmd cmd = new RescanLunCmd();
        cmd.setInstallPath(installPath);

        httpCall(RESCAN_LUN_PATH, hostUuid, cmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                logger.debug(String.format("successfully rescan lun[installPath:%s]", installPath));
                completion.success();

            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void getInitiatorName(String hostUuid, final ReturnValueCompletion<String> completion) {
        AgentCmd cmd = new AgentCmd();
        cmd.uuid = hostUuid;
        httpCall(GET_INITIATOR_NAME_PATH, hostUuid, cmd, true, GetInitiatorNameRsp.class, new ReturnValueCompletion<GetInitiatorNameRsp>(completion) {
            @Override
            public void success(GetInitiatorNameRsp returnValue) {
                completion.success(returnValue.initiatorName);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void createHeartbeat(String hostUuid, BlockScsiLunVO heartbeatLun, IscsiServerInventory iscsiServer, final Completion completion) {
        logger.debug(String.format("Start to create heart beat on host%s, heart beat info:%s, iscsi server info %s",
                hostUuid, JSONObjectUtil.toJsonString(heartbeatLun), JSONObjectUtil.toJsonString(iscsiServer)));
        CreateHeartbeatCmd createHeartbeatCmd = new CreateHeartbeatCmd(heartbeatLun);
        createHeartbeatCmd.setIscsiServer(iscsiServer);
        createHeartbeatCmd.setHeartbeatInstallPath(heartbeatLun.getInstallPath());
        httpCall(CREATE_HEARTBEAT_PATH, hostUuid, createHeartbeatCmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }

        });
    }

    public void deleteHeartbeat(String hostUuid, String psUuid, final Completion completion) {
        logger.debug(String.format("Start to delete ps %s heartbeat on host:%s  ", hostUuid, psUuid));
        DeleteHeartbeatCmd deleteHeartbeatCmd = new DeleteHeartbeatCmd();
        deleteHeartbeatCmd.setHeartbeatPath(BlockPrimaryStorageConstants.HEART_BEAT_PATH_PREFIX + psUuid);
        httpCall(DELETE_HEARTBEAT_PATH, hostUuid, deleteHeartbeatCmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void resizeVolume(String hostUuid, BlockScsiLunVO blockScsiLunVO, final ReturnValueCompletion<ResizeVolumeRsp> completion) {
        final ResizeVolumeOnPrimaryStorageReply reply = new ResizeVolumeOnPrimaryStorageReply();
        ResizeVolumeCmd cmd = new ResizeVolumeCmd();
        cmd.setInstallPath(blockScsiLunVO.getInstallPath());
        cmd.setSize(blockScsiLunVO.getSize());

        httpCall(RESIZE_VOLUME_PATH, hostUuid, cmd, true, ResizeVolumeRsp.class, new ReturnValueCompletion<ResizeVolumeRsp>(completion) {
            @Override
            public void success(ResizeVolumeRsp returnValue) {
                logger.debug(String.format("successfully resize the volume[uuid:%s] to %d", blockScsiLunVO.getUuid(), returnValue.getSize()));
                completion.success(returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("fail to resize volume[uuid:%s] to %d", blockScsiLunVO.getUuid(), blockScsiLunVO.getSize()));
                completion.fail(errorCode);
            }
        });

    }

    public void discoverLun(String hostUuid, BlockScsiLunVO blockScsiLunVO, IscsiServerInventory iscsiServer, final Completion completion) {
        logger.debug(String.format("Start to discover lun:%s on host:%s, iscsi server info:%s",
                JSONObjectUtil.toJsonString(blockScsiLunVO), hostUuid, JSONObjectUtil.toJsonString(iscsiServer)));
        DiscoverLunCmd discoverLunCmd = new DiscoverLunCmd(blockScsiLunVO);
        discoverLunCmd.setIscsiServer(iscsiServer);
        httpCall(DISCOVER_LUN_PATH, hostUuid, discoverLunCmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void logoutTarget(String hostUuid, BlockScsiLunVO blockScsiLunVO, IscsiServerInventory iscsiServer, final Completion completion) {
        logger.debug(String.format("Start to logout lun:%s on host%s, iscsi server info %s",
                JSONObjectUtil.toJsonString(blockScsiLunVO), hostUuid, JSONObjectUtil.toJsonString(iscsiServer)));
        LogoutTargetCmd logoutTargetCmd = new LogoutTargetCmd(blockScsiLunVO);
        logoutTargetCmd.setIscsiServer(iscsiServer);
        httpCall(LOGOUT_TARGET_PATH, hostUuid, logoutTargetCmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
        return;
    }

    public void pingAgentNoFailure(String hostUuid, List<String> psHeartbeatInstallPath, final ReturnValueCompletion<PingAgentNoFailureRsp> completion) {
        logger.debug(String.format("No failure ping agent to check whether host %s can access to ps path %s.", hostUuid, psHeartbeatInstallPath));
        PingAgentNoFailureCmd pingAgentNoFailureCmd = new PingAgentNoFailureCmd();
        pingAgentNoFailureCmd.setPsHeartbeatInstallPath(psHeartbeatInstallPath);
        httpCall(PING_AGENT_NO_FAILURE_PATH, hostUuid, pingAgentNoFailureCmd, true, PingAgentNoFailureRsp.class, new ReturnValueCompletion<PingAgentNoFailureRsp>(completion) {
            @Override
            public void success(PingAgentNoFailureRsp returnValue) {
                completion.success(returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public void uploadImage(String uploadBitPATH, String hostUuid, ImageStoreBackupStorageBlockKvmUploader.UploadToImageStoreCmd cmd, Class<ImageStoreBackupStorageBlockKvmUploader.UploadToImageStoreResponse> uploadToImageStoreResponseClass, final ReturnValueCompletion<ImageStoreBackupStorageBlockKvmUploader.UploadToImageStoreResponse> completion) {
        httpCall(uploadBitPATH, hostUuid, cmd, true, ImageStoreBackupStorageBlockKvmUploader.UploadToImageStoreResponse.class, new ReturnValueCompletion<ImageStoreBackupStorageBlockKvmUploader.UploadToImageStoreResponse>(completion){
            @Override
            public void success(ImageStoreBackupStorageBlockKvmUploader.UploadToImageStoreResponse returnValue) {
                logger.debug(String.format("upload image cmd return %s", returnValue.backupStorageInstallPath));
                completion.success(returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
        return;
    }

    public void commitVolumeAsImage(String imageUuid, BlockScsiLunVO blockScsiLunVO, String bsHostname,String hostUuid, ReturnValueCompletion<CommitVolumeAsImageRsp> completion) {
        CommitVolumeAsImageCmd cmd = new CommitVolumeAsImageCmd();
        cmd.setImageUuid(imageUuid);
        cmd.setPrimaryStorageInstallPath(blockScsiLunVO.getInstallPath().replaceAll("/dev/disk/by-id/wwn-0x", "block://"));
        cmd.setHostname(bsHostname);
        StringBuilder desc = new StringBuilder();

        if (imageUuid != null) {
            ImageInventory inv = ImageInventory.valueOf(dbf.findByUuid(imageUuid, ImageVO.class));
            for (CreateImageExtensionPoint ext : pluginRegistry.getExtensionList(CreateImageExtensionPoint.class)) {
                String tmp = ext.getImageDescription(inv);
                if(tmp != null && !tmp.trim().equals("")) {
                    desc.append(tmp);
                }
            }
            cmd.setDescription(desc.toString());
        }

        httpCall(COMMIT_VOLUME_AS_IMAGE_PATH, hostUuid, cmd, true, CommitVolumeAsImageRsp.class, new ReturnValueCompletion<CommitVolumeAsImageRsp>(completion) {
            @Override
            public void success(CommitVolumeAsImageRsp returnValue) {
                completion.success(returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
        return;
    }
}