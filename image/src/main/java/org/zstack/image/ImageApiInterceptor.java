package org.zstack.image;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.image.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotState;
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
@InterceptorForService(ImageConstant.SERVICE_ID)
public class ImageApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    private static Map<String,PathValidator> pathValidators = new HashMap<>();

    private static final String[] allowedProtocols = new String[]{
            "http://",
            "https://",
            "file:///",
            "upload://",
            "zstore://",
            "ftp://",
            "sftp://"
    };

    static {
        initPathValidatorsFromConfig();
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof ImageMessage) {
            ImageMessage imsg = (ImageMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, imsg.getImageUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddImageMsg) {
            validate((APIAddImageMsg)msg);
        } else if (msg instanceof APICreateRootVolumeTemplateFromRootVolumeMsg) {
            validate((APICreateRootVolumeTemplateFromRootVolumeMsg) msg);
        } else if (msg instanceof APICreateRootVolumeTemplateFromVolumeSnapshotMsg) {
            validate((APICreateRootVolumeTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeMsg) {
            validate((APICreateDataVolumeTemplateFromVolumeMsg) msg);
        } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeSnapshotMsg) {
            validate((APICreateDataVolumeTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APISetImageBootModeMsg) {
            validate((APISetImageBootModeMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APISetImageBootModeMsg msg){
        ImageVO vo = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        if (ImageBootMode.Legacy.toString().equals(msg.getBootMode())
                && ImageArchitecture.aarch64.toString().equals(vo.getArchitecture())) {
            throw new ApiMessageInterceptionException(argerr("The aarch64 architecture does not support legacy."));
        }
    }

    private void validate(APICreateDataVolumeTemplateFromVolumeMsg msg) {
        VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (VolumeStatus.Ready != vol.getStatus()) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is not Ready, it's %s", vol.getUuid(), vol.getStatus()));
        }

        if (VolumeState.Enabled != vol.getState()) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] is not Enabled, it's %s", vol.getUuid(), vol.getState()));
        }
    }

    private void validate(APICreateDataVolumeTemplateFromVolumeSnapshotMsg msg) {
        VolumeSnapshotVO vsvo = dbf.findByUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class);
        if (VolumeSnapshotStatus.Ready != vsvo.getStatus()) {
            throw new ApiMessageInterceptionException(operr("volume snapshot[uuid:%s] is not Ready, it's %s", vsvo.getUuid(), vsvo.getStatus()));
        }

        if (VolumeSnapshotState.Enabled != vsvo.getState()) {
            throw new ApiMessageInterceptionException(operr("volume snapshot[uuid:%s] is not Enabled, it's %s", vsvo.getUuid(), vsvo.getState()));
        }
    }

    protected void validate(APICreateRootVolumeTemplateFromVolumeSnapshotMsg msg) {
        ImageMessageFiller.fillFromSnapshot(msg, msg.getSnapshotUuid());
    }

    @Transactional(readOnly = true)
    protected void validate(APICreateRootVolumeTemplateFromRootVolumeMsg msg) {
        ImageMessageFiller.fillFromVolume(msg, msg.getRootVolumeUuid());
    }

    private void validate(APIAddImageMsg msg) {
        if (ImageMediaType.ISO.toString().equals(msg.getMediaType())) {
            msg.setFormat(ImageConstant.ISO_FORMAT_STRING);
        }

        if (msg.isSystem() && (ImageMediaType.ISO.toString().equals(msg.getMediaType()) || ImageConstant.ISO_FORMAT_STRING.equals(msg.getFormat()))) {
            throw new ApiMessageInterceptionException(argerr(
                    "ISO cannot be used as system image"
            ));
        }

        if (!VolumeFormat.hasType(msg.getFormat())) {
            throw new ApiMessageInterceptionException(argerr("unknown format[%s]", msg.getFormat()));
        }

        if (msg.getType() != null && !ImageType.hasType(msg.getType())) {
            throw new ApiMessageInterceptionException(argerr("unsupported image type[%s]", msg.getType()));
        }

        if (msg.getMediaType() == null) {
            msg.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        }

        ImageMessageFiller.fillDefault(msg);

        if (msg.getBackupStorageUuids() != null) {
            SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
            q.select(BackupStorageVO_.uuid);
            q.add(BackupStorageVO_.status, Op.EQ, BackupStorageStatus.Connected);
            q.add(BackupStorageVO_.availableCapacity, Op.GT, 0);
            q.add(BackupStorageVO_.uuid, Op.IN, msg.getBackupStorageUuids());
            List<String> bsUuids = q.listValue();
            if (bsUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(operr("no backup storage specified in uuids%s is available for adding this image; they are not in status %s or not in state %s, or the uuid is invalid backup storage uuid",
                                msg.getBackupStorageUuids(), BackupStorageStatus.Connected, BackupStorageState.Enabled));
            }
            isValidBS(bsUuids);
            msg.setBackupStorageUuids(bsUuids);
        }

        // compatible with file:/// and /
        if (msg.getUrl().startsWith("/")) {
            msg.setUrl(String.format("file://%s", msg.getUrl()));
        } else if (!isValidProtocol(msg.getUrl())) {
            throw new ApiMessageInterceptionException(argerr("url must starts with 'file:///', 'http://', 'https://'， 'ftp://', 'sftp://' or '/'"));
        }

        if (msg.getUrl().startsWith("file://")) {
            validateLocalPath(msg.getUrl());
        }
    }

    private void validateLocalPath(String url) {
        String path = url.substring("file://".length());
        if (!path.startsWith("/")) {
            throw new ApiMessageInterceptionException(argerr("absolute path must be used", path));
        }

        for (String filterName : ImageGlobalConfig.DOWNLOAD_LOCALPATH_CUSTOMFILTER.value().split(";")) {
            pathValidators.get(filterName).validate(path);
        }
    }

    abstract static class PathValidator {
        abstract void validate(String path);
    }

    private static void initPathValidatorsFromConfig() {
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        String blacklistName = "blacklist";
        String whitelistName = "whitelist";
        PathValidator blackPathValidator = new PathValidator() {
            @Override
            void validate(String path) {
                GlobalConfig blackList = ImageGlobalConfig.DOWNLOAD_LOCALPATH_BLACKLIST;
                String[] bl = blackList.value().split(";");
                boolean inBlackList = Arrays.stream(bl).anyMatch(pattern -> antPathMatcher.match(pattern, path));
                if (inBlackList) {
                    throw new ApiMessageInterceptionException(argerr("image path [%s] is in black list %s", path, blackList.value()));
                }
            }
        };
        PathValidator whitePathValidator = new PathValidator() {
            @Override
            void validate(String path) {
                GlobalConfig whiteList = ImageGlobalConfig.DOWNLOAD_LOCALPATH_WHITELIST;
                if (StringUtils.isBlank(whiteList.value())) {
                    throw new ApiMessageInterceptionException(argerr("all images on this server cannot be used"));
                }
                String[] wl = whiteList.value().split(";");
                boolean inWhiteList = Arrays.stream(wl).anyMatch(pattern -> antPathMatcher.match(pattern, path));
                if (!inWhiteList) {
                    throw new ApiMessageInterceptionException(argerr("image path is not in white list: %s", whiteList.value()));
                }
            }
        };
        pathValidators.put(blacklistName, blackPathValidator);
        pathValidators.put(whitelistName, whitePathValidator);
    }

    private void isValidBS(List<String> bsUuids) {
        for (AddImageExtensionPoint ext : pluginRgty.getExtensionList(AddImageExtensionPoint.class)) {
            ext.validateAddImage(bsUuids);
        }
    }

    private static boolean isValidProtocol(String url) {
        for (String p : allowedProtocols) {
            if (url.startsWith(p)) {
                return true;
            }
        }

        return false;
    }

}
