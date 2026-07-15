package org.zstack.storage.zbs;

import org.apache.commons.lang.StringUtils;
import org.zstack.cbd.AddonInfo;
import org.zstack.cbd.Config;
import org.zstack.cbd.LogicalPoolInfo;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.addon.primary.CreateVolumeSpec;
import org.zstack.header.storage.addon.primary.ZbsVolumeEncryptionBackend;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeStats;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.UUID;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.zbs.ZbsHelper.alignSizeTo;
import static org.zstack.storage.zbs.ZbsHelper.buildVolumePath;
import static org.zstack.storage.zbs.ZbsHelper.getSizeUnit;

class ZbsVolumeEncryptionBackendImpl implements ZbsVolumeEncryptionBackend {
    private static final CLogger logger = Utils.getLogger(ZbsVolumeEncryptionBackendImpl.class);
    private static final String QUERY_SNAPSHOT_PATH = "/zbs/primarystorage/snapshot/query";
    private static final long LUKS_PAYLOAD_OFFSET = 8L * 1024 * 1024;

    private final ZbsStorageController controller;
    private final String primaryStorageUuid;
    private final AddonInfo addonInfo;
    private final Config config;

    ZbsVolumeEncryptionBackendImpl(ZbsStorageController controller, String primaryStorageUuid,
                                   AddonInfo addonInfo, Config config) {
        this.controller = controller;
        this.primaryStorageUuid = primaryStorageUuid;
        this.addonInfo = addonInfo;
        this.config = config;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    @Override
    public String buildConfiguredVolumePath(String volumeName) {
        String logicalPoolName = config.getLogicalPoolName();
        String physicalPoolName = addonInfo.getLogicalPoolInfos().stream()
                .filter(it -> logicalPoolName.equals(it.getLogicalPoolName()))
                .map(LogicalPoolInfo::getPhysicalPoolName)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(logicalPoolName + "_physical");
        return buildVolumePath(physicalPoolName, logicalPoolName, volumeName);
    }

    @Override
    public String buildEncryptedTargetPath(String installPath) {
        CbdPath path = parseActiveCbdPath(installPath);
        String targetVolume = String.format("%s-encrypted-%s", path.volume,
                UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        return buildVolumePath(path.physicalPool, path.logicalPool, targetVolume);
    }

    @Override
    public void createLuksBackingVolume(String installPath, long virtualSize, ReturnValueCompletion<String> completion) {
        CbdPath path;
        try {
            path = parseActiveCbdPath(installPath);
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        }

        ZbsStorageController.CreateVolumeCmd cmd = new ZbsStorageController.CreateVolumeCmd();
        cmd.setLogicalPool(path.logicalPool);
        cmd.setVolume(path.volume);
        cmd.setUnit(getSizeUnit(addonInfo.getClusterInfo().getVersion()));
        cmd.setSize(alignSizeTo(luksBackingSize(virtualSize), cmd.getUnit()));
        cmd.setSkipIfExisting(false);

        controller.httpCall(ZbsStorageController.CREATE_VOLUME_PATH, cmd, ZbsStorageController.CreateVolumeRsp.class,
                new ReturnValueCompletion<ZbsStorageController.CreateVolumeRsp>(completion) {
                    @Override
                    public void success(ZbsStorageController.CreateVolumeRsp returnValue) {
                        completion.success(returnValue.getInstallPath());
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    public void deleteLuksBackingVolume(String installPath) {
        controller.doDeleteVolume(installPath, true, new NopeCompletion());
    }

    @Override
    public void cloneVolumeAsBacking(String srcInstallPath, CreateVolumeSpec dst,
                                     ReturnValueCompletion<VolumeStats> completion) {
        ZbsStorageController.CloneVolumeCmd cmd = new ZbsStorageController.CloneVolumeCmd();
        cmd.setPath(srcInstallPath);
        cmd.setDstVolume(dst.getName());

        controller.httpCall(ZbsStorageController.CLONE_VOLUME_PATH, cmd, ZbsStorageController.CloneVolumeRsp.class,
                new ReturnValueCompletion<ZbsStorageController.CloneVolumeRsp>(completion) {
                    @Override
                    public void success(ZbsStorageController.CloneVolumeRsp returnValue) {
                        VolumeStats stats = new VolumeStats();
                        stats.setInstallPath(returnValue.getInstallPath());
                        stats.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                        stats.setSize(returnValue.getSize());
                        stats.setActualSize(returnValue.getActualSize());
                        stats.setParentUri(srcInstallPath);
                        completion.success(stats);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    public void resolveSnapshotPathForQemu(String installPath, ReturnValueCompletion<String> completion) {
        if (!installPath.contains("@")) {
            completion.success(installPath);
            return;
        }

        QuerySnapshotCmd cmd = new QuerySnapshotCmd();
        cmd.setPath(installPath);
        controller.httpCall(QUERY_SNAPSHOT_PATH, cmd, QuerySnapshotRsp.class,
                new ReturnValueCompletion<QuerySnapshotRsp>(completion) {
                    @Override
                    public void success(QuerySnapshotRsp returnValue) {
                        completion.success(returnValue.getInstallPath());
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    public void checkNoSnapshots(String installPath, Completion completion) {
        try {
            parseActiveCbdPath(installPath);
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        }

        QuerySnapshotCmd cmd = new QuerySnapshotCmd();
        cmd.setPath(installPath);
        controller.httpCall(QUERY_SNAPSHOT_PATH, cmd, QuerySnapshotRsp.class,
                new ReturnValueCompletion<QuerySnapshotRsp>(completion) {
                    @Override
                    public void success(QuerySnapshotRsp returnValue) {
                        if (returnValue.isHasSnapshot()) {
                            completion.fail(operr("cannot encrypt ZBS volume[%s] in place because it has snapshots", installPath));
                            return;
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
    public void validateConversionPaths(String sourceInstallPath, String targetInstallPath) {
        String version = addonInfo.getClusterInfo() == null ? null : addonInfo.getClusterInfo().getVersion();
        if (StringUtils.isBlank(version) || !ZbsConstants.MEGABYTE_UNIT.equals(getSizeUnit(version))) {
            throw new OperationFailureException(operr(
                    "ZBS volume encryption conversion requires ZBS version[%s] or later, but current version is[%s]",
                    ZbsConstants.MEGABYTE_SUPPORTED_VERSION, version));
        }

        CbdPath source = parseConversionCbdPath(sourceInstallPath);
        CbdPath target = parseConversionCbdPath(targetInstallPath);
        if (!source.physicalPool.equals(target.physicalPool) || !source.logicalPool.equals(target.logicalPool)) {
            throw new OperationFailureException(operr(
                    "ZBS volume encryption conversion requires source[%s] and target[%s] in the same physical and logical pool",
                    sourceInstallPath, targetInstallPath));
        }
        if (source.volume.equals(target.volume)) {
            throw new OperationFailureException(operr(
                    "ZBS volume encryption conversion requires different source and target volumes, but both are[%s]",
                    sourceInstallPath));
        }
    }

    @Override
    public void createConversionTarget(String targetInstallPath, long virtualSize, boolean targetEncrypted,
                                       ReturnValueCompletion<String> completion) {
        CbdPath target;
        try {
            target = parseConversionCbdPath(targetInstallPath);
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        }

        ZbsStorageController.CreateVolumeCmd cmd = new ZbsStorageController.CreateVolumeCmd();
        cmd.setLogicalPool(target.logicalPool);
        cmd.setVolume(target.volume);
        cmd.setUnit(getSizeUnit(addonInfo.getClusterInfo().getVersion()));
        long allocatedSize = targetEncrypted ? luksBackingSize(virtualSize) : virtualSize;
        cmd.setSize(alignSizeTo(allocatedSize, cmd.getUnit()));
        cmd.setSkipIfExisting(false);

        controller.httpCall(ZbsStorageController.CREATE_VOLUME_PATH, cmd, ZbsStorageController.CreateVolumeRsp.class,
                new ReturnValueCompletion<ZbsStorageController.CreateVolumeRsp>(completion) {
                    @Override
                    public void success(ZbsStorageController.CreateVolumeRsp returnValue) {
                        String createdInstallPath = returnValue.getInstallPath();
                        if (targetInstallPath.equals(createdInstallPath)) {
                            completion.success(createdInstallPath);
                            return;
                        }

                        ErrorCode error = operr(
                                "ZBS volume encryption conversion target[%s] was created at unexpected path[%s]",
                                targetInstallPath, createdInstallPath);
                        String cleanupInstallPath = targetInstallPath;
                        try {
                            deleteConversionTarget(cleanupInstallPath, new Completion(completion) {
                                @Override
                                public void success() {
                                    completion.fail(error);
                                }

                                @Override
                                public void fail(ErrorCode cleanupError) {
                                    logger.warn(String.format(
                                            "failed to cleanup unexpected ZBS conversion target[installPath:%s] after create path mismatch: %s",
                                            cleanupInstallPath, cleanupError));
                                    completion.fail(error);
                                }
                            });
                        } catch (Exception e) {
                            logger.warn(String.format(
                                    "failed to cleanup unexpected ZBS conversion target[installPath:%s] after create path mismatch: %s",
                                    cleanupInstallPath, e.getMessage()));
                            completion.fail(error);
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    public void deleteConversionTarget(String targetInstallPath, Completion completion) {
        controller.doDeleteVolume(targetInstallPath, true, completion);
    }

    @Override
    public void stats(String installPath, ReturnValueCompletion<VolumeStats> completion) {
        controller.stats(installPath, completion);
    }

    private CbdPath parseCbdPath(String installPath) {
        String path = installPath.startsWith("cbd:") ? installPath.substring("cbd:".length()) : installPath;
        String[] parts = path.split("/", 3);
        if (parts.length != 3) {
            throw new OperationFailureException(operr("invalid ZBS CBD path[%s]", installPath));
        }

        CbdPath ret = new CbdPath();
        ret.physicalPool = parts[0];
        ret.logicalPool = parts[1];
        String[] volumeAndSnapshot = parts[2].split("@", 2);
        ret.volume = volumeAndSnapshot[0];
        ret.snapshot = volumeAndSnapshot.length > 1 ? volumeAndSnapshot[1] : null;
        return ret;
    }

    private CbdPath parseActiveCbdPath(String installPath) {
        CbdPath path = parseCbdPath(installPath);
        if (StringUtils.isNotBlank(path.snapshot)) {
            throw new OperationFailureException(operr("ZBS operation requires active volume path, but got[%s]", installPath));
        }
        return path;
    }

    private CbdPath parseConversionCbdPath(String installPath) {
        if (StringUtils.isBlank(installPath) || !installPath.startsWith("cbd:") || installPath.contains("@")) {
            throw new OperationFailureException(operr(
                    "ZBS volume encryption conversion requires an active CBD path, but got[%s]", installPath));
        }

        String[] parts = installPath.substring("cbd:".length()).split("/", -1);
        if (parts.length != 3 || StringUtils.isBlank(parts[0]) || StringUtils.isBlank(parts[1]) ||
                StringUtils.isBlank(parts[2])) {
            throw new OperationFailureException(operr("invalid ZBS CBD path[%s]", installPath));
        }

        CbdPath path = new CbdPath();
        path.physicalPool = parts[0];
        path.logicalPool = parts[1];
        path.volume = parts[2];
        return path;
    }

    private long luksBackingSize(long virtualSize) {
        return virtualSize + LUKS_PAYLOAD_OFFSET;
    }

    public static class QuerySnapshotRsp extends ZbsStorageController.AgentResponse {
        private boolean hasSnapshot;
        private String installPath;

        public boolean isHasSnapshot() {
            return hasSnapshot;
        }

        public void setHasSnapshot(boolean hasSnapshot) {
            this.hasSnapshot = hasSnapshot;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class QuerySnapshotCmd extends ZbsStorageController.AgentCommand {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    private static class CbdPath {
        private String physicalPool;
        private String logicalPool;
        private String volume;
        private String snapshot;
    }
}
