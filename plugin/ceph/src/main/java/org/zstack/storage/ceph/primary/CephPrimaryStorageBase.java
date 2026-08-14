package org.zstack.storage.ceph.primary;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.agent.AgentConstant;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.*;
import org.zstack.core.trash.StorageTrash;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.Constants;
import org.zstack.header.HasThreadContext;
import org.zstack.header.agent.CancelCommand;
import org.zstack.header.agent.ReloadableCommand;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.*;
import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.core.trash.TrashCleanupResult;
import org.zstack.header.core.validation.Validation;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.*;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg;
import org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialReply;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.storage.ceph.*;
import org.zstack.storage.ceph.CephMonBase.PingResult;
import org.zstack.storage.ceph.backup.CephBackupStorageVO;
import org.zstack.storage.ceph.backup.CephBackupStorageVO_;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase.PingOperationFailure;
import org.zstack.storage.ceph.primary.capacity.CephOsdGroupCapacityHelper;
import org.zstack.storage.primary.*;
import org.zstack.storage.volume.VolumeErrors;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.SystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.longjob.LongJobUtils.buildErrIfCanceled;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.CollectionUtils.transform;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by frank on 7/28/2015.
 */
public class CephPrimaryStorageBase extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(CephPrimaryStorageBase.class);

    @Autowired
    private RESTFacade restf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CephImageCacheCleaner imageCacheCleaner;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private StorageTrash trash;
    @Autowired
    private PoolUsageReport poolUsageCollector;


    public CephPrimaryStorageBase() {
    }

    static class ReconnectMonLock {
        AtomicBoolean hold = new AtomicBoolean(false);

        boolean lock() {
            return hold.compareAndSet(false, true);
        }

        void unlock() {
            hold.set(false);
        }
    }

    ReconnectMonLock reconnectMonLock = new ReconnectMonLock();

    private final String queueId = "Ceph-" + self.getUuid();

    private CephOsdGroupCapacityHelper osdHelper;

    protected RunInQueue inQueue() {
        return new RunInQueue(queueId, thdf, getCephSyncLevel());
    }

    protected int getCephSyncLevel() {
        return CephGlobalConfig.CEPH_SYNC_LEVEL.value(Integer.class);
    }

    public static class AgentCommand {
        String fsId;
        String uuid;
        public String monUuid;
        String token;
        String tpTimeout;
        String monIp;

        public String getMonIp() {
            return monIp;
        }

        public void setMonIp(String monIp) {
            this.monIp = monIp;
        }

        public String getTpTimeout() {
            return tpTimeout;
        }

        public void setTpTimeout(String tpTimeout) {
            this.tpTimeout = tpTimeout;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getMonUuid() {
            return monUuid;
        }

        public void setMonUuid(String monUuid) {
            this.monUuid = monUuid;
        }

        public String getFsId() {
            return fsId;
        }

        public void setFsId(String fsId) {
            this.fsId = fsId;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class AgentResponse extends CephMonBase.AgentResponse {
        Long totalCapacity;
        Long availableCapacity;
        List<CephPoolCapacity> poolCapacities;
        String type;

        public AgentResponse() {
            if (CoreGlobalProperty.UNIT_TEST_ON) {
                type = CephConstants.CEPH_MANUFACTURER_OPENSOURCE;
            }
        }

        public Long getTotalCapacity() {
            return totalCapacity;
        }

        public void setTotalCapacity(Long totalCapacity) {
            this.totalCapacity = totalCapacity;
        }

        public Long getAvailableCapacity() {
            return availableCapacity;
        }

        public void setAvailableCapacity(Long availableCapacity) {
            this.availableCapacity = availableCapacity;
        }

        public List<CephPoolCapacity> getPoolCapacities() {
            return poolCapacities;
        }

        public void setPoolCapacities(List<CephPoolCapacity> poolCapacities) {
            this.poolCapacities = poolCapacities;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class AddPoolCmd extends AgentCommand {
        public String poolName;
        public boolean isCreate;
    }

    public static class AddPoolRsp extends AgentResponse {
    }

    public static class DeletePoolRsp extends AgentResponse {
    }

    public static class GetVolumeSizeCmd extends AgentCommand {
        public String volumeUuid;
        public String installPath;
    }

    public static class GetBatchVolumeSizeCmd extends AgentCommand {
        public Map<String, String> volumeUuidInstallPaths;
    }

    public static class GetVolumeSnapshotSizeCmd extends AgentCommand {
        public String volumeSnapshotUuid;
        public String installPath;
    }

    public static class GetVolumeSizeRsp extends AgentResponse {
        public Long size;
        public Long actualSize;
    }

    public static class GetBatchVolumeSizeRsp extends AgentResponse {
        public Map<String, Long> actualSizes;
    }

    public static class GetVolumeSnapshotSizeRsp extends AgentResponse {
        public Long size;
        public Long actualSize;
    }

    public static class Pool {
        String name;
        boolean predefined;
    }

    public static class InitCmd extends AgentCommand {
        List<Pool> pools;
        Boolean nocephx = false;

        public List<Pool> getPools() {
            return pools;
        }

        public void setPools(List<Pool> pools) {
            this.pools = pools;
        }

        public Boolean getNocephx() {
            return nocephx;
        }

        public void setNocephx(Boolean nocephx) {
            this.nocephx = nocephx;
        }
    }

    public static class InitRsp extends AgentResponse {
        String fsid;
        String userKey;
        String manufacturer;

        public String getUserKey() {
            return userKey;
        }

        public void setUserKey(String userKey) {
            this.userKey = userKey;
        }

        public String getFsid() {
            return fsid;
        }

        public void setFsid(String fsid) {
            this.fsid = fsid;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }
    }

    public static class CheckCmd extends AgentCommand {
        List<Pool> pools;

        public List<Pool> getPools() {
            return pools;
        }

        public void setPools(List<Pool> pools) {
            this.pools = pools;
        }
    }

    public static class CheckRsp extends AgentResponse{

    }

    public static class CreateEmptyVolumeCmd extends AgentCommand {
        String installPath;
        long size;
        boolean shareable;
        boolean skipIfExisting;
        String format = VolumeConstant.VOLUME_FORMAT_RAW;

        public boolean isShareable() {
            return shareable;
        }

        public void setShareable(boolean shareable) {
            this.shareable = shareable;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public void setSkipIfExisting(boolean skipIfExisting) {
            this.skipIfExisting = skipIfExisting;
        }

        public boolean isSkipIfExisting() {
            return skipIfExisting;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    public static class CreateEmptyVolumeRsp extends AgentResponse {
        public String installPath;
        public Long actualSize;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DeleteCmd extends AgentCommand {
        String installPath;
        long expirationTime;

        public long getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(long expirationTime) {
            this.expirationTime = expirationTime;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DeleteRsp extends AgentResponse {
        public boolean inUse;
        public ErrorCode buildErrorCode() {
            if (inUse) {
                return Platform.err(VolumeErrors.VOLUME_IN_USE, getError());
            }
            return super.buildErrorCode();
        }
    }

    public static class CloneCmd extends AgentCommand {
        String srcPath;
        String dstPath;

        public String getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(String srcPath) {
            this.srcPath = srcPath;
        }

        public String getDstPath() {
            return dstPath;
        }

        public void setDstPath(String dstPath) {
            this.dstPath = dstPath;
        }
    }

    public static class CloneRsp extends AgentResponse {
        public Long size;
        public Long actualSize;
        public String installPath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class FlattenCmd extends AgentCommand implements HasThreadContext {
        String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class FlattenRsp extends AgentResponse {

    }

    public static class SftpDownloadCmd extends AgentCommand {
        String sshKey;
        String hostname;
        String username;
        int sshPort;
        String backupStorageInstallPath;
        String primaryStorageInstallPath;
        boolean shareable;

        public boolean isShareable() {
            return shareable;
        }

        public void setShareable(boolean shareable) {
            this.shareable = shareable;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getSshPort() {
            return sshPort;
        }

        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
        }

        public String getSshKey() {
            return sshKey;
        }

        public void setSshKey(String sshKey) {
            this.sshKey = sshKey;
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

    public static class SftpDownloadRsp extends AgentResponse {
    }

    public static class SftpUpLoadCmd extends AgentCommand implements HasThreadContext{
        String sendCommandUrl;
        String primaryStorageInstallPath;
        String backupStorageInstallPath;
        String hostname;
        String username;
        String sshKey;
        int sshPort;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getSshPort() {
            return sshPort;
        }

        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
        }

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getSshKey() {
            return sshKey;
        }

        public void setSshKey(String sshKey) {
            this.sshKey = sshKey;
        }

        public void setSendCommandUrl(String sendCommandUrl) {
            this.sendCommandUrl = sendCommandUrl;
        }

        public String getSendCommandUrl() {
            return sendCommandUrl;
        }
    }

    public static class SftpUploadRsp extends AgentResponse {
    }

    public static class CreateSnapshotCmd extends AgentCommand {
        boolean skipOnExisting;
        String snapshotPath;
        String volumeUuid;
        String name;
        String description;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public boolean isSkipOnExisting() {
            return skipOnExisting;
        }

        public void setSkipOnExisting(boolean skipOnExisting) {
            this.skipOnExisting = skipOnExisting;
        }

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class CreateSnapshotRsp extends AgentResponse {
        Long size;
        Long actualSize;
        String installPath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public Long getActualSize() {
            return actualSize;
        }

        public void setActualSize(Long actualSize) {
            this.actualSize = actualSize;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class UpdateSnapshotCmd extends AgentCommand {
        String snapshotPath;
        String name;
        String description;

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class UpdateSnapshotRsp extends AgentResponse {
    }

    public static class DeleteSnapshotCmd extends AgentCommand {
        String snapshotPath;

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class DeleteSnapshotRsp extends AgentResponse {
    }

    public static class ProtectSnapshotCmd extends AgentCommand {
        String snapshotPath;
        boolean ignoreError;

        public boolean isIgnoreError() {
            return ignoreError;
        }

        public void setIgnoreError(boolean ignoreError) {
            this.ignoreError = ignoreError;
        }

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class ProtectSnapshotRsp extends AgentResponse {
    }

    public static class UnprotectedSnapshotCmd extends AgentCommand {
        String snapshotPath;

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class UnprotectedSnapshotRsp extends AgentResponse {
    }

    public static class CpCmd extends AgentCommand implements HasThreadContext {
        String sendCommandUrl;
        String resourceUuid;
        String srcPath;
        String dstPath;
        boolean shareable;
    }

    public static class CpRsp extends AgentResponse {
        public Long size;
        public Long actualSize;
        public String installPath;
    }

    public static class RollbackSnapshotCmd extends AgentCommand implements HasThreadContext {
        String snapshotPath;

        public String getSnapshotPath() {
            return snapshotPath;
        }

        public void setSnapshotPath(String snapshotPath) {
            this.snapshotPath = snapshotPath;
        }
    }

    public static class RollbackSnapshotRsp extends AgentResponse {
        @Validation
        long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class CheckIsBitsExistingCmd extends AgentCommand {
        String installPath;

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getInstallPath() {
            return installPath;
        }
    }

    public static class CheckHostStorageConnectionCmd extends AgentCommand implements Serializable {
        public List<String> poolNames;
        public String hostUuid;
        public String userKey;
        int storageCheckerTimeout;
        @NoLogging(type = NoLogging.Type.Uri)
        public List<String> monUrls;

        public List<String> getPoolNames() {
            return poolNames;
        }

        public void setPoolNames(List<String> poolNames) {
            this.poolNames = poolNames;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getUserKey() {
            return userKey;
        }

        public void setUserKey(String userKey) {
            this.userKey = userKey;
        }

        public int getStorageCheckerTimeout() {
            return storageCheckerTimeout;
        }

        public void setStorageCheckerTimeout(int storageCheckerTimeout) {
            this.storageCheckerTimeout = storageCheckerTimeout;
        }

        public List<String> getMonUrls() {
            return monUrls;
        }

        public void setMonUrls(List<String> monUrls) {
            this.monUrls = monUrls;
        }
    }

    public static class CheckHostStorageConnectionRsp extends AgentResponse {

    }

    public static class CreateKvmSecretCmd extends KVMAgentCommands.AgentCommand {
        String userKey;
        String uuid;

        public String getUserKey() {
            return userKey;
        }

        public void setUserKey(String userKey) {
            this.userKey = userKey;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class CreateKvmSecretRsp extends AgentResponse {

    }

    public static class DeletePoolCmd extends AgentCommand {
        List<String> poolNames;

        public List<String> getPoolNames() {
            return poolNames;
        }

        public void setPoolNames(List<String> poolNames) {
            this.poolNames = poolNames;
        }
    }

    public static class KvmSetupSelfFencerCmd extends AgentCommand implements Serializable {
        public List<String> poolNames;
        public String hostUuid;
        public long interval;
        public int maxAttempts;
        public int storageCheckerTimeout;
        public String userKey;
        @NoLogging(type = NoLogging.Type.Uri)
        public List<String> monUrls;
        public String strategy;
        public String manufacturer;
        public List<String> fencers;
    }

    public static class KvmCancelSelfFencerCmd extends AgentCommand {
        public String hostUuid;
    }

    public static class GetFactsCmd extends AgentCommand {
    }

    public static class GetFactsRsp extends AgentResponse {
        public String fsid;
        public String monAddr;
        public List<String> ipAddresses;
    }

    public static class DeleteImageCacheCmd extends AgentCommand {
        public String imagePath;
        public String snapshotPath;
    }

    public static class PurgeSnapshotCmd extends AgentCommand {
        public String volumePath;
    }

    public static class PurgeSnapshotRsp extends AgentResponse {

    }

    public static class GetVolumeWatchersCmd extends AgentCommand {
        public String volumePath;
    }

    public static class GetVolumeWatchersRsp extends AgentResponse {
        public List<String> watchers;
    }

    public static class GetBackingChainCmd extends AgentCommand {
        public String volumePath;
    }

    public static class GetBackingChainRsp extends AgentResponse {
        public List<String> backingChain;

        public long totalSize;
    }

    public static class DeleteVolumeChainCmd extends AgentCommand {
        public List<String> installPaths;
    }

    public static class DeleteVolumeChainRsp extends AgentResponse {
        public List<String> undeletedInstallPaths;
    }

    public static class CleanTrashCmd extends AgentCommand {
        public List<String> pools;
        public boolean force;
    }

    public static class CleanTrashRsp extends AgentResponse {
        public Map<String, List<String>> pool2TrashResult;
    }

    public static class CephToCephMigrateVolumeSegmentCmd extends AgentCommand implements HasThreadContext, Serializable {
        String parentUuid;
        String resourceUuid;
        String srcInstallPath;
        String dstInstallPath;
        String dstMonHostname;
        String dstMonSshUsername;
        @NoLogging
        String dstMonSshPassword;
        int dstMonSshPort;

        public String getParentUuid() {
            return parentUuid;
        }

        public void setParentUuid(String parentUuid) {
            this.parentUuid = parentUuid;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public String getSrcInstallPath() {
            return srcInstallPath;
        }

        public void setSrcInstallPath(String srcInstallPath) {
            this.srcInstallPath = srcInstallPath;
        }

        public String getDstInstallPath() {
            return dstInstallPath;
        }

        public void setDstInstallPath(String dstInstallPath) {
            this.dstInstallPath = dstInstallPath;
        }

        public String getDstMonHostname() {
            return dstMonHostname;
        }

        public void setDstMonHostname(String dstMonHostname) {
            this.dstMonHostname = dstMonHostname;
        }

        public String getDstMonSshUsername() {
            return dstMonSshUsername;
        }

        public void setDstMonSshUsername(String dstMonSshUsername) {
            this.dstMonSshUsername = dstMonSshUsername;
        }

        public String getDstMonSshPassword() {
            return dstMonSshPassword;
        }

        public void setDstMonSshPassword(String dstMonSshPassword) {
            this.dstMonSshPassword = dstMonSshPassword;
        }

        public int getDstMonSshPort() {
            return dstMonSshPort;
        }

        public void setDstMonSshPort(int dstMonSshPort) {
            this.dstMonSshPort = dstMonSshPort;
        }
    }

    // common response of storage migration
    public static class StorageMigrationRsp extends AgentResponse {
    }

    public static class GetVolumeSnapInfosCmd extends AgentCommand {
        private String volumePath;

        public String getVolumePath() {
            return volumePath;
        }

        public void setVolumePath(String volumePath) {
            this.volumePath = volumePath;
        }
    }

    public static class GetVolumeSnapInfosRsp extends AgentResponse {
        private List<SnapInfo> snapInfos;

        public List<SnapInfo> getSnapInfos() {
            return snapInfos;
        }

        public void setSnapInfos(List<SnapInfo> snapInfos) {
            this.snapInfos = snapInfos;
        }
    }

    public static class GetDownloadBitsFromKVMHostProgressCmd extends AgentCommand {
        public List<String> volumePaths;
    }

    public static class GetDownloadBitsFromKVMHostProgressRsp extends AgentResponse {
        public long totalSize;

        public long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }
    }

    public static class DownloadBitsFromKVMHostRsp extends AgentResponse {
        public String format;
    }

    public static class DownloadBitsFromNbdRsp extends AgentResponse {
        public long diskSize;
    }


    public static class DownloadBitsFromNbdCmd extends AgentCommand implements HasThreadContext, Serializable {
        @NoLogging
        private String nbdExportUrl;
        private String primaryStorageInstallPath;
        private long bandwidth;
        private String sendCommandUrl;

        public String getNbdExportUrl() {
            return nbdExportUrl;
        }

        public void setNbdExportUrl(String nbdExportUrl) {
            this.nbdExportUrl = nbdExportUrl;
        }

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }

        public long getBandwidth() {
            return bandwidth;
        }

        public void setBandwidth(long bandwidth) {
            this.bandwidth = bandwidth;
        }

        public void setSendCommandUrl(String sendCommandUrl) {
            this.sendCommandUrl = sendCommandUrl;
        }

        public String getSendCommandUrl() {
            return sendCommandUrl;
        }
    }

    public static class DownloadBitsFromRemoteTargetRsp extends AgentResponse {
        public long diskSize;
    }

    public static class DownloadBitsFromRemoteTargetCmd extends AgentCommand implements HasThreadContext, Serializable {
        @NoLogging
        private String remoteTargetUri;
        private String primaryStorageInstallPath;
        private String sendCommandUrl;

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }

        public String getRemoteTargetUri() {
            return remoteTargetUri;
        }

        public void setRemoteTargetUri(String remoteTargetUri) {
            this.remoteTargetUri = remoteTargetUri;
        }

        public void setSendCommandUrl(String sendCommandUrl) {
            this.sendCommandUrl = sendCommandUrl;
        }

        public String getSendCommandUrl() {
            return sendCommandUrl;
        }
    }

    public static class DownloadBitsFromKVMHostCmd extends AgentCommand implements ReloadableCommand {
        private String hostname;
        private String username;
        private String sshKey;
        private int sshPort;
        // it's file path on kvm host actually
        private String backupStorageInstallPath;
        private String primaryStorageInstallPath;
        private Long bandWidth;
        private String identificationCode;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getSshKey() {
            return sshKey;
        }

        public void setSshKey(String sshKey) {
            this.sshKey = sshKey;
        }

        public int getSshPort() {
            return sshPort;
        }

        public void setSshPort(int sshPort) {
            this.sshPort = sshPort;
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

        public Long getBandWidth() {
            return bandWidth;
        }

        public void setBandWidth(Long bandWidth) {
            this.bandWidth = bandWidth;
        }

        @Override
        public void setIdentificationCode(String identificationCode) {
            this.identificationCode = identificationCode;
        }
    }

    public static class CancelDownloadBitsFromKVMHostCmd extends AgentCommand {
        private String primaryStorageInstallPath;

        public String getPrimaryStorageInstallPath() {
            return primaryStorageInstallPath;
        }

        public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
            this.primaryStorageInstallPath = primaryStorageInstallPath;
        }
    }

    public static class CancelCmd extends AgentCommand implements CancelCommand {
        private String cancellationApiId;

        @Override
        public void setCancellationApiId(String cancellationApiId) {
            this.cancellationApiId = cancellationApiId;
        }
    }

    public static class SnapInfo implements Comparable<SnapInfo> {
        long id;
        String name;
        long size;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        @Override
        public int compareTo(SnapInfo snapInfo) {
            return Long.compare(this.getId(), snapInfo.getId());
        }
    }

    public static final String INIT_PATH = "/ceph/primarystorage/init";
    public static final String CREATE_VOLUME_PATH = "/ceph/primarystorage/volume/createempty";
    public static final String DELETE_PATH = "/ceph/primarystorage/delete";
    public static final String CLONE_PATH = "/ceph/primarystorage/volume/clone";
    public static final String FLATTEN_PATH = "/ceph/primarystorage/volume/flatten";
    public static final String SFTP_DOWNLOAD_PATH = "/ceph/primarystorage/sftpbackupstorage/download";
    public static final String SFTP_UPLOAD_PATH = "/ceph/primarystorage/sftpbackupstorage/upload";
    public static final String CREATE_SNAPSHOT_PATH = "/ceph/primarystorage/snapshot/create";
    public static final String DELETE_SNAPSHOT_PATH = "/ceph/primarystorage/snapshot/delete";
    public static final String PURGE_SNAPSHOT_PATH = "/ceph/primarystorage/volume/purgesnapshots";
    public static final String PROTECT_SNAPSHOT_PATH = "/ceph/primarystorage/snapshot/protect";
    public static final String ROLLBACK_SNAPSHOT_PATH = "/ceph/primarystorage/snapshot/rollback";
    public static final String UNPROTECT_SNAPSHOT_PATH = "/ceph/primarystorage/snapshot/unprotect";
    public static final String CP_PATH = "/ceph/primarystorage/volume/cp";
    public static final String KVM_CREATE_SECRET_PATH = "/vm/createcephsecret";
    public static final String CHECK_HOST_STORAGE_CONNECTION_PATH = "/ceph/primarystorage/check/host/connection";
    public static final String DELETE_POOL_PATH = "/ceph/primarystorage/deletepool";
    public static final String GET_VOLUME_SIZE_PATH = "/ceph/primarystorage/getvolumesize";
    public static final String BATCH_GET_VOLUME_SIZE_PATH = "/ceph/primarystorage/batchgetvolumesize";
    public static final String GET_VOLUME_SNAPSHOT_SIZE_PATH = "/ceph/primarystorage/getvolumesnapshotsize";
    public static final String KVM_HA_SETUP_SELF_FENCER = "/ha/ceph/setupselffencer";
    public static final String KVM_HA_CANCEL_SELF_FENCER = "/ha/ceph/cancelselffencer";
    public static final String GET_FACTS = "/ceph/primarystorage/facts";
    public static final String DELETE_IMAGE_CACHE = "/ceph/primarystorage/deleteimagecache";
    public static final String ADD_POOL_PATH = "/ceph/primarystorage/addpool";
    public static final String CHECK_POOL_PATH = "/ceph/primarystorage/checkpool";
    public static final String CHECK_BITS_PATH = "/ceph/primarystorage/snapshot/checkbits";
    public static final String CEPH_TO_CEPH_MIGRATE_VOLUME_SEGMENT_PATH = "/ceph/primarystorage/volume/migratesegment";
    public static final String GET_VOLUME_SNAPINFOS_PATH = "/ceph/primarystorage/volume/getsnapinfos";
    public static final String DOWNLOAD_BITS_FROM_KVM_HOST_PATH = "/ceph/primarystorage/kvmhost/download";
    public static final String DOWNLOAD_BITS_FROM_NBD_EXPT_PATH = "/ceph/primarystorage/nbd/download";
    public static final String DOWNLOAD_BITS_FROM_REMOTE_TARGET_PATH = "/ceph/primarystorage/remotetarget/download";
    public static final String CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH = "/ceph/primarystorage/kvmhost/download/cancel";
    public static final String CHECK_SNAPSHOT_COMPLETED = "/ceph/primarystorage/check/snapshot";
    public static final String GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH = "/ceph/primarystorage/kvmhost/download/progress";
    public static final String GET_IMAGE_WATCHERS_PATH = "/ceph/primarystorage/getvolumewatchers";
    public static final String GET_BACKING_CHAIN_PATH = "/ceph/primarystorage/volume/getbackingchain";
    public static final String DELETE_VOLUME_CHAIN_PATH = "/ceph/primarystorage/volume/deletechain";
    public static final String CLAEN_TRASH_PATH = "/ceph/primarystorage/trash/clean";


    private final Map<String, BackupStorageMediator> backupStorageMediators = new HashMap<String, BackupStorageMediator>();
    List<PrimaryStorageLicenseInfoFactory> licenseExts;

    {
        backupStorageMediators.put(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE, new SftpBackupStorageMediator());
        backupStorageMediators.put(CephConstants.CEPH_BACKUP_STORAGE_TYPE, new CephBackupStorageMediator());
        List<PrimaryStorageToBackupStorageMediatorExtensionPoint> exts = pluginRgty.getExtensionList(PrimaryStorageToBackupStorageMediatorExtensionPoint.class);
        exts.forEach(ext -> backupStorageMediators.putAll(ext.getBackupStorageMediators()));
        licenseExts = pluginRgty.getExtensionList(PrimaryStorageLicenseInfoFactory.class);
    }

    class SftpBackupStorageMediator extends BackupStorageMediator {
        private void getSftpCredentials(final ReturnValueCompletion<GetSftpBackupStorageDownloadCredentialReply> completion) {
            GetSftpBackupStorageDownloadCredentialMsg gmsg = new GetSftpBackupStorageDownloadCredentialMsg();
            gmsg.setBackupStorageUuid(backupStorage.getUuid());
            bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, backupStorage.getUuid());
            bus.send(gmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                    } else {
                        completion.success((GetSftpBackupStorageDownloadCredentialReply) reply);
                    }
                }
            });
        }

        @Override
        public void download(final ReturnValueCompletion<String> completion) {
            checkParam();
            final MediatorDownloadParam dparam = (MediatorDownloadParam) param;

            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("download-image-from-sftp-%s-to-ceph-%s", backupStorage.getUuid(), dparam.getPrimaryStorageUuid()));
            chain.then(new ShareFlow() {
                String sshkey;
                int sshport;
                String sftpHostname;
                String username;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = "get-sftp-credentials";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            getSftpCredentials(new ReturnValueCompletion<GetSftpBackupStorageDownloadCredentialReply>(trigger) {
                                @Override
                                public void success(GetSftpBackupStorageDownloadCredentialReply greply) {
                                    sshkey = greply.getSshKey();
                                    sshport = greply.getSshPort();
                                    sftpHostname = greply.getHostname();
                                    username = greply.getUsername();
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "download-image";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            SftpDownloadCmd cmd = new SftpDownloadCmd();
                            cmd.backupStorageInstallPath = dparam.getImage().getSelectedBackupStorage().getInstallPath();
                            cmd.hostname = sftpHostname;
                            cmd.username = username;
                            cmd.sshKey = sshkey;
                            cmd.sshPort = sshport;
                            cmd.primaryStorageInstallPath = dparam.getInstallPath();
                            cmd.shareable = dparam.isShareable();

                            httpCall(SFTP_DOWNLOAD_PATH, cmd, SftpDownloadRsp.class, new ReturnValueCompletion<SftpDownloadRsp>(trigger) {
                                @Override
                                public void success(SftpDownloadRsp returnValue) {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    done(new FlowDoneHandler(completion) {
                        @Override
                        public void handle(Map data) {
                            completion.success(dparam.getInstallPath());
                        }
                    });

                    error(new FlowErrorHandler(completion) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            completion.fail(errCode);
                        }
                    });
                }
            }).start();
        }

        @Override
        public void upload(final ReturnValueCompletion<String> completion) {
            checkParam();

            final MediatorUploadParam uparam = (MediatorUploadParam) param;

            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.enableProgressReport();
            chain.setName(String.format("upload-image-ceph-%s-to-sftp-%s", self.getUuid(), backupStorage.getUuid()));
            chain.then(new ShareFlow() {
                String sshKey;
                String hostname;
                String username;
                int sshPort;
                String backupStorageInstallPath;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = "get-sftp-credentials";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            getSftpCredentials(new ReturnValueCompletion<GetSftpBackupStorageDownloadCredentialReply>(trigger) {
                                @Override
                                public void success(GetSftpBackupStorageDownloadCredentialReply returnValue) {
                                    sshKey = returnValue.getSshKey();
                                    hostname = returnValue.getHostname();
                                    username = returnValue.getUsername();
                                    sshPort = returnValue.getSshPort();
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "get-backup-storage-install-path";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            BackupStorageAskInstallPathMsg msg = new BackupStorageAskInstallPathMsg();
                            msg.setBackupStorageUuid(backupStorage.getUuid());
                            msg.setImageUuid(uparam.image.getUuid());
                            msg.setImageMediaType(uparam.image.getMediaType());
                            bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, backupStorage.getUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                    } else {
                                        backupStorageInstallPath = ((BackupStorageAskInstallPathReply) reply).getInstallPath();
                                        trigger.next();
                                    }
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "upload-to-backup-storage";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            SftpUpLoadCmd cmd = new SftpUpLoadCmd();
                            cmd.setSendCommandUrl(restf.getSendCommandUrl());
                            cmd.setBackupStorageInstallPath(backupStorageInstallPath);
                            cmd.setHostname(hostname);
                            cmd.setUsername(username);
                            cmd.setSshKey(sshKey);
                            cmd.setSshPort(sshPort);
                            cmd.setPrimaryStorageInstallPath(uparam.primaryStorageInstallPath);

                            httpCall(SFTP_UPLOAD_PATH, cmd, SftpUploadRsp.class, new ReturnValueCompletion<SftpUploadRsp>(trigger) {
                                @Override
                                public void success(SftpUploadRsp returnValue) {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    done(new FlowDoneHandler(completion) {
                        @Override
                        public void handle(Map data) {
                            completion.success(backupStorageInstallPath);
                        }
                    });

                    error(new FlowErrorHandler(completion) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            completion.fail(errCode);
                        }
                    });
                }
            }).start();
        }

        @Override
        public boolean deleteWhenRollbackDownload() {
            return true;
        }
    }

    class CephBackupStorageMediator extends BackupStorageMediator {
        public void checkParam() {
            super.checkParam();

            SimpleQuery<CephBackupStorageVO> q = dbf.createQuery(CephBackupStorageVO.class);
            q.select(CephBackupStorageVO_.fsid);
            q.add(CephBackupStorageVO_.uuid, Op.EQ, backupStorage.getUuid());
            String bsFsid = q.findValue();
            if (!getSelf().getFsid().equals(bsFsid)) {
                throw new OperationFailureException(operr(
                        "the backup storage[uuid:%s, name:%s, fsid:%s] is not in the same ceph cluster" +
                                " with the primary storage[uuid:%s, name:%s, fsid:%s]", backupStorage.getUuid(),
                        backupStorage.getName(), bsFsid, self.getUuid(), self.getName(), getSelf().getFsid())
                );
            }
        }

        @Override
        public void download(final ReturnValueCompletion<String> completion) {
            checkParam();

            final MediatorDownloadParam dparam = (MediatorDownloadParam) param;
            if (ImageMediaType.DataVolumeTemplate.toString().equals(dparam.getImage().getInventory().getMediaType())) {
                CpCmd cmd = new CpCmd();
                cmd.srcPath = dparam.getImage().getSelectedBackupStorage().getInstallPath();
                cmd.dstPath = dparam.getInstallPath();
                cmd.shareable = dparam.isShareable();
                httpCall(CP_PATH, cmd, CpRsp.class, new ReturnValueCompletion<CpRsp>(completion) {
                    @Override
                    public void success(CpRsp returnValue) {
                        completion.success(dparam.getInstallPath());
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            } else {
                completion.success(dparam.getImage().getSelectedBackupStorage().getInstallPath());
            }
        }

        @Override
        public void upload(final ReturnValueCompletion<String> completion) {
            checkParam();
            final MediatorUploadParam uparam = (MediatorUploadParam) param;

            CpCmd cmd = new CpCmd();
            cmd.sendCommandUrl = restf.getSendCommandUrl();
            cmd.fsId = getSelf().getFsid();
            cmd.srcPath = uparam.primaryStorageInstallPath;
            cmd.dstPath = uparam.backupStorageInstallPath;

            final String apiId = ThreadContext.get(Constants.THREAD_CONTEXT_API);
            new HttpCaller<>(CP_PATH, cmd, CpRsp.class, new ReturnValueCompletion<CpRsp>(completion) {
                @Override
                public void success(CpRsp rsp) {
                    completion.success(rsp.installPath);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            }).specifyOrder(apiId).call();
        }

        @Override
        public boolean deleteWhenRollbackDownload() {
            return false;
        }
    }

    private BackupStorageMediator getBackupStorageMediator(String bsUuid) {
        BackupStorageVO bsvo = dbf.findByUuid(bsUuid, BackupStorageVO.class);
        BackupStorageMediator mediator = backupStorageMediators.get(bsvo.getType());
        if (mediator == null) {
            throw new CloudRuntimeException(String.format("cannot find BackupStorageMediator for type[%s]", bsvo.getType()));
        }

        mediator.backupStorage = BackupStorageInventory.valueOf(bsvo);
        return mediator;
    }


    public CephPrimaryStorageBase(PrimaryStorageVO self) {
        super(self);
        osdHelper = new CephOsdGroupCapacityHelper(self.getUuid());
    }

    protected CephPrimaryStorageVO getSelf() {
        return (CephPrimaryStorageVO) self;
    }

    protected CephPrimaryStorageInventory getSelfInventory() {
        return CephPrimaryStorageInventory.valueOf(getSelf());
    }

    private void createEmptyVolume(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        String volumeUuid = msg.getVolume().getUuid();
        final String finalPoolName = getTargetPoolNameFromAllocatedUrl(msg.getAllocatedInstallUrl());
        cmd.installPath = makeVolumeInstallPathByTargetPool(volumeUuid, finalPoolName);
        cmd.size = msg.getVolume().getSize();
        cmd.setShareable(msg.getVolume().isShareable());
        cmd.skipIfExisting = msg.isSkipIfExisting();

        VolumeType type = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, volumeUuid)
                .select(VolumeVO_.type)
                .findValue();
        cmd.format = msg.hasSystemTag(VolumeSystemTags.FORMAT_QCOW2.getTagFormat()) ?
                VolumeConstant.VOLUME_FORMAT_QCOW2 :
                VolumeConstant.VOLUME_FORMAT_RAW ;

        final InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();

        httpCall(CREATE_VOLUME_PATH, cmd, CreateEmptyVolumeRsp.class, new ReturnValueCompletion<CreateEmptyVolumeRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CreateEmptyVolumeRsp ret) {
                VolumeInventory vol = msg.getVolume();
                vol.setInstallPath(buildEmptyVolumeInstallPath(finalPoolName, cmd.installPath, ret.getInstallPath()));
                vol.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                vol.setActualSize(ret.actualSize);
                reply.setVolume(vol);
                bus.reply(msg, reply);
            }
        });
    }

    protected String buildEmptyVolumeInstallPath(String targetCephPoolName,String canonicalPath, String installPath) {
        if (StringUtils.isEmpty(installPath)) {
            return canonicalPath;
        }

        return makeVolumeInstallPathByTargetPool(installPath, targetCephPoolName);
    }

    private void cleanTrash(Long trashId, final ReturnValueCompletion<TrashCleanupResult> completion) {
        InstallPathRecycleInventory inv = trash.getTrash(trashId);
        if (inv == null) {
            completion.success(new TrashCleanupResult(trashId));
            return;
        }

        String details = trash.makeSureInstallPathNotUsed(inv);
        if (details != null) {
            completion.success(new TrashCleanupResult(inv.getResourceUuid(), inv.getTrashId(), operr(details)));
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("clean-trash-on-volume-%s", inv.getInstallPath()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                PurgeSnapshotOnPrimaryStorageMsg msg = new PurgeSnapshotOnPrimaryStorageMsg();
                msg.setPrimaryStorageUuid(self.getUuid());
                msg.setVolumePath(inv.getInstallPath());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            logger.info(String.format("Purged all snapshots of volume %s.", inv.getInstallPath()));
                        } else {
                            logger.warn(String.format("Failed to purge snapshots of volume %s.", inv.getInstallPath()));
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                msg.setPrimaryStorageUuid(self.getUuid());
                msg.setInstallPath(inv.getInstallPath());
                msg.setSize(inv.getSize());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            logger.info(String.format("Deleted volume %s in Trash.", inv.getInstallPath()));
                        } else {
                            logger.warn(String.format("Failed to delete volume %s in Trash.", inv.getInstallPath()));
                        }
                        trigger.next();
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                imsg.setPrimaryStorageUuid(self.getUuid());
                imsg.setDiskSize(inv.getSize());
                bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                bus.send(imsg);
                logger.info(String.format("Returned space[size:%s] to PS %s after volume migration", inv.getSize(), self.getUuid()));
                trash.removeFromDb(trashId);

                completion.success(new TrashCleanupResult(inv.getResourceUuid(), inv.getTrashId(), inv.getSize()));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void cleanUpTrash(Long trashId, final ReturnValueCompletion<List<TrashCleanupResult>> completion) {
        if (trashId != null) {
            cleanTrash(trashId, new ReturnValueCompletion<TrashCleanupResult>(completion) {
                @Override
                public void success(TrashCleanupResult res) {
                    completion.success(CollectionDSL.list(res));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
            return;
        }

        List<TrashCleanupResult> results = Collections.synchronizedList(new ArrayList<>());
        List<InstallPathRecycleInventory> trashs = trash.getTrashList(self.getUuid(), trashLists);
        if (trashs.isEmpty()) {
            completion.success(results);
            return;
        }

        new While<>(trashs).step((inv, coml) -> {
            String details = trash.makeSureInstallPathNotUsed(inv);
            if (details != null) {
                results.add(new TrashCleanupResult(inv.getResourceUuid(), inv.getTrashId(), operr(details)));
                coml.done();
                return;
            }

            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
            chain.setName(String.format("clean-trash-on-volume-%s", inv.getInstallPath()));
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    PurgeSnapshotOnPrimaryStorageMsg msg = new PurgeSnapshotOnPrimaryStorageMsg();
                    msg.setPrimaryStorageUuid(self.getUuid());
                    msg.setVolumePath(inv.getInstallPath());
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                    bus.send(msg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.info(String.format("Purged all snapshots of volume %s.", inv.getInstallPath()));
                            } else {
                                logger.warn(String.format("Failed to purge snapshots of volume %s.", inv.getInstallPath()));
                            }
                            trigger.next();
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                    msg.setPrimaryStorageUuid(self.getUuid());
                    msg.setInstallPath(inv.getInstallPath());
                    msg.setSize(inv.getSize());
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                    bus.send(msg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.info(String.format("Deleted volume %s in Trash.", inv.getInstallPath()));
                            } else {
                                logger.warn(String.format("Failed to delete volume %s in Trash.", inv.getInstallPath()));
                            }
                            trigger.next();
                        }
                    });
                }
            });

            chain.done(new FlowDoneHandler(coml) {
                @Override
                public void handle(Map data) {
                    IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                    imsg.setPrimaryStorageUuid(self.getUuid());
                    imsg.setDiskSize(inv.getSize());
                    bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                    bus.send(imsg);
                    logger.info(String.format("Returned space[size:%s] to PS %s after volume migration", inv.getSize(), self.getUuid()));

                    results.add(new TrashCleanupResult(inv.getResourceUuid(), inv.getTrashId(), inv.getSize()));
                    trash.removeFromDb(inv.getTrashId());
                    coml.done();
                }
            }).error(new FlowErrorHandler(coml) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    coml.addError(errCode);
                    coml.done();
                }
            }).start();
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success(results);
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    protected void handle(final CleanUpTrashOnPrimaryStroageMsg msg) {
        MessageReply reply = new MessageReply();
        thdf.chainSubmit(new ChainTask(msg) {
            private String name = String.format("cleanup-trash-on-%s", self.getUuid());

            @Override
            public String getSyncSignature() {
                return name;
            }

            @Override
            public void run(SyncTaskChain chain) {
                cleanUpTrash(msg.getTrashId(), new ReturnValueCompletion<List<TrashCleanupResult>>(msg) {
                    @Override
                    public void success(List<TrashCleanupResult> returnValues) {
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

    private PrimaryStorageLicenseInfoFactory getPrimaryStorageLicenseInfoFactory(String vendor) {
        for(PrimaryStorageLicenseInfoFactory ext: licenseExts) {
            if (ext.getPrimaryStorageVendor().equals(vendor)) {
                return ext;
            }
        }
        return null;
    }

    private void createPrimaryStorageLicenseVendor(String type) {
        for(PrimaryStorageLicenseInfoFactory ext: licenseExts) {
            ext.createPrimaryStorageVendorSystemTag(self.getUuid(), type);
        }
    }

    private void handle(GetPrimaryStorageLicenseInfoMsg msg) {
        GetPrimaryStorageLicenseInfoReply reply = new GetPrimaryStorageLicenseInfoReply();

        if (!PrimaryStorageSystemTags.PRIMARY_STORAGE_VENDOR.hasTag(msg.getPrimaryStorageUuid())) {
            bus.reply(msg, reply);
            return;
        }
        String vendor = PrimaryStorageSystemTags.PRIMARY_STORAGE_VENDOR.getTokenByResourceUuid(msg.getPrimaryStorageUuid(), PrimaryStorageSystemTags.PRIMARY_STORAGE_VENDOR_TOKEN);
        final PrimaryStorageLicenseInfoFactory factory = getPrimaryStorageLicenseInfoFactory(vendor);
        if (factory == null) {
            bus.reply(msg, reply);
            return;
        }
        factory.getPrimaryStorageLicenseInfo(msg.getPrimaryStorageUuid(), new ReturnValueCompletion<PrimaryStorageLicenseInfo>(msg) {
            @Override
            public void success(PrimaryStorageLicenseInfo primaryStorageLicenseInfo) {
                reply.setPrimaryStorageLicenseInfo(primaryStorageLicenseInfo);
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
    protected void handle(final APICleanUpTrashOnPrimaryStorageMsg msg) {
        APICleanUpTrashOnPrimaryStorageEvent evt = new APICleanUpTrashOnPrimaryStorageEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            private String name = String.format("cleanup-trash-on-%s", self.getUuid());

            @Override
            public String getSyncSignature() {
                return name;
            }

            @Override
            public void run(SyncTaskChain chain) {
                cleanUpTrash(msg.getTrashId(), new ReturnValueCompletion<List<TrashCleanupResult>>(chain) {
                    @Override
                    public void success(List<TrashCleanupResult> results) {
                        evt.setResult(TrashCleanupResult.buildCleanTrashResult(results));
                        evt.setResults(results);
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
                return name;
            }
        });
    }

    protected void handle(final APICleanUpStorageTrashOnPrimaryStorageMsg msg) {
        thdf.singleFlightSubmit(new SingleFlightTask(msg)
            .setSyncSignature("cleanup-ceph-trash-for-ps-" + msg.getPrimaryStorageUuid())
            .run(comp -> {
                cleanUpStorageTrash(new ReturnValueCompletion<Map>(comp) {
                    @Override
                    public void success(Map result) {
                        comp.success(result);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        comp.fail(errorCode);
                    }
                }, msg.isForce());
            })
            .done(result -> {
                APICleanUpStorageTrashOnPrimaryStorageEvent event = new APICleanUpStorageTrashOnPrimaryStorageEvent(msg.getId());
                if (result.isSuccess()) {
                    Map<String, List<String>> res = (Map<String, List<String>>) result.getResult();
                    event.setResult(res);
                    event.setTotal(res.values().stream().mapToInt(List::size).sum());
                    bus.publish(event);
                } else {
                    event.setError(result.getErrorCode());
                    bus.publish(event);
                }
            }));
    }

    @Override
    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
        APICleanUpImageCacheOnPrimaryStorageEvent evt = new APICleanUpImageCacheOnPrimaryStorageEvent(msg.getId());
        imageCacheCleaner.cleanup(msg.getUuid(), false);
        bus.publish(evt);
    }

    @Override
    protected void handle(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        if (msg instanceof InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) {
            createVolumeFromTemplate((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
        } else {
            createEmptyVolume(msg);
        }
    }

    class DownloadToCache {
        ImageSpec image;
        VolumeSnapshotInventory snapshot;
        private void doDownload(final ReturnValueCompletion<ImageCacheVO> completion) {
            ImageCacheVO cache = Q.New(ImageCacheVO.class)
                    .eq(ImageCacheVO_.primaryStorageUuid, self.getUuid())
                    .eq(ImageCacheVO_.imageUuid, image.getInventory().getUuid())
                    .find();
            if (cache != null) {
                completion.success(cache);
                return;
            }
            final FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("prepare-image-cache-ceph-%s", self.getUuid()));
            chain.then(new ShareFlow() {
                String cachePath;
                String snapshotPath;
                long actualSize = image.getInventory().getActualSize();
                String allocatedInstall;
                ImageCacheVO cvo = new ImageCacheVO();

                @Override
                public void setup() {
                    flow(new Flow() {
                        String __name__ = "allocate-primary-storage-capacity-for-image-cache";

                        boolean s = false;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                            amsg.setRequiredPrimaryStorageUuid(self.getUuid());
                            amsg.setSize(image.getInventory().getActualSize());
                            amsg.setPurpose(PrimaryStorageAllocationPurpose.DownloadImage.toString());
                            amsg.setImageUuid(image.getInventory().getUuid());
                            amsg.setNoOverProvisioning(true);
                            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                    } else {
                                        s = true;
                                        AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                                        allocatedInstall = ar.getAllocatedInstallUrl();
                                        trigger.next();
                                    }
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (s) {
                                ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                                rmsg.setNoOverProvisioning(true);
                                rmsg.setAllocatedInstallUrl(allocatedInstall);
                                rmsg.setPrimaryStorageUuid(self.getUuid());
                                rmsg.setDiskSize(image.getInventory().getActualSize());
                                bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
                                bus.send(rmsg);
                            }

                            trigger.rollback();
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "download-from-" + (snapshot != null ? "volume" : "backup-storage");

                        boolean deleteOnRollback;
                        String dstPath;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            dstPath = makeVolumeInstallPathByTargetPool(image.getInventory().getUuid(),
                                    getTargetPoolNameFromAllocatedUrl(allocatedInstall));
                            if (snapshot != null) {
                                createFromVolumeSnapshot(trigger);
                            } else {
                                downloadFromBackupStorage(trigger);
                            }
                        }

                        private void createFromVolumeSnapshot(FlowTrigger trigger) {
                            deleteOnRollback = true;
                            CpCmd cmd = new CpCmd();
                            cmd.srcPath = snapshot.getPrimaryStorageInstallPath();
                            cmd.dstPath = dstPath;
                            cmd.shareable = false;
                            httpCall(CP_PATH, cmd, CpRsp.class, new ReturnValueCompletion<CpRsp>(completion) {
                                @Override
                                public void success(CpRsp rsp) {
                                    if (rsp.actualSize != null) {
                                        actualSize = rsp.actualSize;
                                    }
                                    cachePath = rsp.installPath;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        private void downloadFromBackupStorage(FlowTrigger trigger) {
                            MediatorDownloadParam param = new MediatorDownloadParam();
                            param.setImage(image);
                            param.setInstallPath(dstPath);
                            param.setPrimaryStorageUuid(self.getUuid());
                            BackupStorageMediator mediator = getBackupStorageMediator(image.getSelectedBackupStorage().getBackupStorageUuid());
                            mediator.param = param;

                            deleteOnRollback = mediator.deleteWhenRollbackDownload();
                            mediator.download(new ReturnValueCompletion<String>(trigger) {
                                @Override
                                public void success(String path) {
                                    cachePath = path;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (deleteOnRollback && cachePath != null) {
                                DeleteCmd cmd = new DeleteCmd();
                                cmd.installPath = cachePath;
                                httpCall(DELETE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(null) {
                                    @Override
                                    public void success(DeleteRsp returnValue) {
                                        logger.debug(String.format("successfully deleted %s", cachePath));
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        //TODO GC
                                        logger.warn(String.format("unable to delete %s, %s. Need a cleanup", cachePath, errorCode));
                                    }
                                });
                            }

                            trigger.rollback();
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "create-snapshot";

                        boolean needCleanup = false;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            snapshotPath = String.format("%s@%s", cachePath, image.getInventory().getUuid());
                            CreateSnapshotCmd cmd = new CreateSnapshotCmd();
                            cmd.skipOnExisting = true;
                            cmd.snapshotPath = snapshotPath;
                            httpCall(CREATE_SNAPSHOT_PATH, cmd, CreateSnapshotRsp.class, new ReturnValueCompletion<CreateSnapshotRsp>(trigger) {
                                @Override
                                public void success(CreateSnapshotRsp rsp) {
                                    needCleanup = true;
                                    snapshotPath = rsp.getInstallPath();
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (needCleanup) {
                                DeleteSnapshotCmd cmd = new DeleteSnapshotCmd();
                                cmd.snapshotPath = snapshotPath;
                                httpCall(DELETE_SNAPSHOT_PATH, cmd, DeleteSnapshotRsp.class, new ReturnValueCompletion<DeleteSnapshotRsp>(null) {
                                    @Override
                                    public void success(DeleteSnapshotRsp returnValue) {
                                        logger.debug(String.format("successfully deleted the snapshot %s", snapshotPath));
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        //TODO
                                        logger.warn(String.format("unable to delete the snapshot %s, %s. Need a cleanup", snapshotPath, errorCode));
                                    }
                                });
                            }

                            trigger.rollback();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "protect-snapshot";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            ProtectSnapshotCmd cmd = new ProtectSnapshotCmd();
                            cmd.snapshotPath = snapshotPath;
                            cmd.ignoreError = true;
                            httpCall(PROTECT_SNAPSHOT_PATH, cmd, ProtectSnapshotRsp.class, new ReturnValueCompletion<ProtectSnapshotRsp>(trigger) {
                                @Override
                                public void success(ProtectSnapshotRsp returnValue) {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "save-db";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            cvo.setMd5sum("not calculated");
                            cvo.setSize(image.getInventory().getActualSize());
                            cvo.setInstallUrl(snapshotPath);
                            cvo.setImageUuid(image.getInventory().getUuid());
                            cvo.setPrimaryStorageUuid(self.getUuid());
                            cvo.setMediaType(ImageMediaType.valueOf(image.getInventory().getMediaType()));
                            cvo.setState(ImageCacheState.ready);
                            cvo.setSize(actualSize);
                            cvo = dbf.persistAndRefresh(cvo);
                            trigger.next();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "invoke-after-create-image-cache-extensions";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            ImageCacheInventory inventory = ImageCacheInventory.valueOf(cvo);
                            new While<>(pluginRgty.getExtensionList(AfterCreateImageCacheExtensionPoint.class)).each((ext, whileCompletion) -> {
                                ext.saveEncryptAfterCreateImageCache(null, inventory, new Completion(whileCompletion) {
                                    @Override
                                    public void success() {
                                        whileCompletion.done();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        whileCompletion.addError(errorCode);
                                        whileCompletion.done();
                                    }
                                });
                            }).run(new WhileDoneCompletion(trigger) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    if (!errorCodeList.getCauses().isEmpty()) {
                                        String details = multiErr(errorCodeList.getCauses()).getReadableDetails();
                                        logger.warn(String.format(
                                                "failed to invoke after create image cache extensions (but still continue): %s",
                                                details));
                                    }
                                    trigger.next();
                                }
                            });
                        }
                    });

                    done(new FlowDoneHandler(completion) {
                        @Override
                        public void handle(Map data) {
                            completion.success(cvo);
                        }
                    });

                    error(new FlowErrorHandler(completion) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            completion.fail(errCode);
                        }
                    });
                }
            }).start();
        }

        void download(final ReturnValueCompletion<ImageCacheVO> completion) {
            thdf.chainSubmit(new ChainTask(completion) {
                @Override
                public String getSyncSignature() {
                    return String.format("ceph-p-%s-download-image-%s", self.getUuid(), image.getInventory().getUuid());
                }

                private void checkEncryptImageCache(ImageCacheVO cacheVO, final SyncTaskChain chain) {
                    List<AfterCreateImageCacheExtensionPoint> extensionList = pluginRgty.getExtensionList(AfterCreateImageCacheExtensionPoint.class);

                    if (extensionList.isEmpty()) {
                        completion.success(cacheVO);
                        chain.next();
                        return;
                    }

                    extensionList.forEach(ext -> ext.checkEncryptImageCache(null, ImageCacheInventory.valueOf(cacheVO), new Completion(chain) {
                        @Override
                        public void success() {
                            completion.success(cacheVO);
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    }));
                }


                @Override
                public void run(final SyncTaskChain chain) {
                    ImageCacheVO cache = Q.New(ImageCacheVO.class)
                            .eq(ImageCacheVO_.primaryStorageUuid, self.getUuid())
                            .eq(ImageCacheVO_.imageUuid, image.getInventory().getUuid())
                            .find();

                    if (cache != null) {
                        final CheckIsBitsExistingCmd cmd = new CheckIsBitsExistingCmd();
                        cmd.setInstallPath(ImageCacheUtil.getImageCachePath(cache.toInventory()));
                        httpCall(CHECK_BITS_PATH, cmd, CheckIsBitsExistingRsp.class, new ReturnValueCompletion<CheckIsBitsExistingRsp>(chain) {
                            @Override
                            public void success(CheckIsBitsExistingRsp returnValue) {
                                if (returnValue.isExisting()) {
                                    logger.debug("image has been existing");
                                    checkEncryptImageCache(cache, chain);
                                    return;
                                } else {
                                    logger.debug("image not found, remove vo and re-download");
                                    SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
                                    q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                                    q.add(ImageCacheVO_.imageUuid, Op.EQ, image.getInventory().getUuid());
                                    ImageCacheVO cvo = q.find();

                                    IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                                    imsg.setDiskSize(cvo.getSize());
                                    imsg.setPrimaryStorageUuid(cvo.getPrimaryStorageUuid());
                                    bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, cvo.getPrimaryStorageUuid());
                                    bus.send(imsg);
                                    dbf.remove(cvo);

                                    doDownload(new ReturnValueCompletion<ImageCacheVO>(chain) {
                                        @Override
                                        public void success(ImageCacheVO returnValue) {
                                            completion.success(returnValue);
                                            chain.next();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            completion.fail(errorCode);
                                            chain.next();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                completion.fail(errorCode);
                                chain.next();
                            }
                        });

                    } else {
                        doDownload(new ReturnValueCompletion<ImageCacheVO>(chain) {
                            @Override
                            public void success(ImageCacheVO returnValue) {
                                completion.success(returnValue);
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                completion.fail(errorCode);
                                chain.next();
                            }
                        });
                    }
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }
            });
        }
    }

    private void createVolumeFromTemplate(final InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        final VmInstanceSpec.ImageSpec ispec = msg.getTemplateSpec();
        String targetCephPoolName = getTargetPoolNameFromAllocatedUrl(msg.getAllocatedInstallUrl());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-root-volume-%s", msg.getVolume().getUuid()));
        chain.then(new ShareFlow() {
            String cloneInstallPath;
            String volumePath = makeVolumeInstallPathByTargetPool(msg.getVolume().getUuid(), targetCephPoolName);
            ImageCacheInventory cache;
            Long actualSize;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-image-to-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        DownloadVolumeTemplateToPrimaryStorageMsg dmsg = new DownloadVolumeTemplateToPrimaryStorageMsg();
                        dmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                        dmsg.setHostUuid(msg.getDestHost().getUuid());
                        dmsg.setTemplateSpec(ispec);
                        bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, dmsg.getPrimaryStorageUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                cache = ((DownloadVolumeTemplateToPrimaryStorageReply) reply).getImageCache();
                                cloneInstallPath = ImageCacheUtil.getImageCachePath(cache);
                                trigger.next();
                            }
                        });
                    }
                });


                flow(new NoRollbackFlow() {
                    String __name__ = "clone-image";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CloneCmd cmd = new CloneCmd();
                        cmd.srcPath = cloneInstallPath;
                        cmd.dstPath = volumePath;

                        httpCall(CLONE_PATH, cmd, CloneRsp.class, new ReturnValueCompletion<CloneRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(CloneRsp ret) {
                                actualSize = ret.actualSize;
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "resize-volume-on-primary-storage";

                    @Override
                    public boolean skip(Map data) {
                        ImageInventory image = ispec.getInventory();
                        return image.getSize() >= msg.getVolume().getSize();
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ResizeVolumeOnPrimaryStorageMsg rmsg = new ResizeVolumeOnPrimaryStorageMsg();
                        rmsg.setVolume(msg.getVolume());
                        rmsg.setSize(msg.getVolume().getSize());
                        rmsg.setPrimaryStorageUuid(msg.getVolume().getPrimaryStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, msg.getVolume().getPrimaryStorageUuid());
                        bus.send(rmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to resize volume[%s] on ps[%s] but still continue: %s",
                                            msg.getVolume().getUuid(),
                                            msg.getVolume().getPrimaryStorageUuid(),
                                            reply.getError().getReadableDetails()));
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        VolumeInventory vol = msg.getVolume();
                        vol.setInstallPath(volumePath);
                        vol.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                        vol.setActualSize(actualSize);
                        reply.setVolume(vol);

                        ImageCacheVolumeRefVO ref = new ImageCacheVolumeRefVO();
                        ref.setImageCacheId(cache.getId());
                        ref.setPrimaryStorageUuid(self.getUuid());
                        ref.setVolumeUuid(vol.getUuid());
                        dbf.persist(ref);

                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        inQueue().name(String.format("delete-volume-on-primarystorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> deleteVolumeOnPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    protected void deleteVolumeOnPrimaryStorage(final DeleteVolumeOnPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        msg.getVolume().getUuid();
        DeleteCmd cmd = new DeleteCmd();
        cmd.installPath = msg.getVolume().getInstallPath();

        final DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();

        httpCall(DELETE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                if (err.isError(VolumeErrors.VOLUME_IN_USE)) {
                    logger.debug(String.format("unable to delete volume[uuid:%s] right now, skip this GC job because it's in use", msg.getVolume().getUuid()));
                    reply.setError(err);
                    bus.reply(msg, reply);
                    completion.done();
                    return;
                }
                CephDeleteVolumeGC gc = new CephDeleteVolumeGC();
                gc.NAME = String.format("gc-ceph-%s-volume-%s", self.getUuid(), msg.getVolume().getUuid());
                gc.primaryStorageUuid = self.getUuid();
                gc.volume = msg.getVolume();
                gc.deduplicateSubmit(CephGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);

                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(DeleteRsp ret) {
                osdHelper.releaseAvailableCapWithRatio(msg.getVolume().getInstallPath(), msg.getVolume().getSize());
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    @Override
    protected void handle(DownloadVolumeTemplateToPrimaryStorageMsg msg) {
        final DownloadVolumeTemplateToPrimaryStorageReply reply = new DownloadVolumeTemplateToPrimaryStorageReply();
        DownloadToCache downloadToCache = new DownloadToCache();
        downloadToCache.image = msg.getTemplateSpec();
        downloadToCache.download(new ReturnValueCompletion<ImageCacheVO>(msg) {
            @Override
            public void success(ImageCacheVO cache) {
                reply.setImageCache(ImageCacheInventory.valueOf(cache));
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
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {
        inQueue().name(String.format("delete-bits-on-primarystorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> deleteBitsOnPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    protected void deleteBitsOnPrimaryStorage(final DeleteBitsOnPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        DeleteCmd cmd = new DeleteCmd();
        cmd.installPath = msg.getInstallPath();

        final DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();

        httpCall(DELETE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(DeleteRsp ret) {
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    private void checkCephFsId(String psUuid, String bsUuid) {
        CephPrimaryStorageVO cephPS = dbf.findByUuid(psUuid, CephPrimaryStorageVO.class);
        DebugUtils.Assert(cephPS != null && cephPS.getFsid() != null, String.format("ceph ps: [%s] and its fsid cannot be null", psUuid));
        CephBackupStorageVO cephBS = dbf.findByUuid(bsUuid, CephBackupStorageVO.class);
        if (cephBS != null) {
            DebugUtils.Assert(cephBS.getFsid() != null, String.format("fsid cannot be null in ceph bs:[%s]", bsUuid));
            if (!cephPS.getFsid().equals(cephBS.getFsid())) {
                throw new OperationFailureException(operr(
                        "fsid is not same between ps[%s] and bs[%s], create template is forbidden.", psUuid, bsUuid));
            }
        }
    }

    @Override
    protected void check(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {}

    @Override
    protected void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
        final CreateImageCacheFromVolumeOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeOnPrimaryStorageReply();

        String volumeUuid = msg.getVolumeInventory().getUuid();
        String imageUuid = msg.getImageInventory().getUuid();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-snapshot-and-image-from-volume-%s", volumeUuid));
        chain.enableProgressReport();
        chain.preCheck(data -> buildErrIfCanceled());
        chain.then(new ShareFlow() {
            VolumeSnapshotInventory snapshot;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "create-volume-snapshot";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        String volumeAccountUuid = acntMgr.getOwnerAccountUuidOfResource(volumeUuid);

                        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                        cmsg.setName("Snapshot-" + volumeUuid);
                        cmsg.setDescription("Take snapshot for " + volumeUuid);
                        cmsg.setVolumeUuid(volumeUuid);
                        cmsg.setAccountUuid(volumeAccountUuid);

                        bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply r) {
                                if (!r.isSuccess()) {
                                    trigger.fail(r.getError());
                                    return;
                                }

                                CreateVolumeSnapshotReply createVolumeSnapshotReply = (CreateVolumeSnapshotReply)r;
                                snapshot = createVolumeSnapshotReply.getInventory();
                                trigger.next();
                            }
                        });

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-image-cache-from-volume-snapshot";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        DownloadToCache cache = new DownloadToCache();
                        cache.image = new ImageSpec();
                        cache.image.setInventory(msg.getImageInventory());
                        cache.snapshot = snapshot;
                        cache.download(new ReturnValueCompletion<ImageCacheVO>(trigger) {
                            @Override
                            public void success(ImageCacheVO cache) {
                                reply.setActualSize(cache.getSize());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        logger.debug(String.format("successfully create template[uuid:%s] from volume[uuid:%s]", imageUuid, volumeUuid));
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.warn(String.format("failed to create template from volume[uuid:%s], because %s", volumeUuid, errCode));
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply();

        boolean useSnapshotAsCache = msg.hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat());
        if (useSnapshotAsCache) {
            ProtectSnapshotCmd cmd = new ProtectSnapshotCmd();
            cmd.snapshotPath = msg.getVolumeSnapshot().getPrimaryStorageInstallPath();
            cmd.ignoreError = true;
            httpCall(PROTECT_SNAPSHOT_PATH, cmd, ProtectSnapshotRsp.class, new ReturnValueCompletion<ProtectSnapshotRsp>(msg) {
                @Override
                public void success(ProtectSnapshotRsp returnValue) {
                    ImageCacheVO cache = createTemporaryImageCacheFromVolumeSnapshot(msg.getImageInventory(), msg.getVolumeSnapshot());
                    dbf.persist(cache);
                    reply.setInventory(cache.toInventory());
                    reply.setIncremental(true);
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
            return;
        }

        DownloadToCache cache = new DownloadToCache();
        cache.image = new ImageSpec();
        cache.image.setInventory(msg.getImageInventory());
        cache.snapshot = msg.getVolumeSnapshot();
        cache.download(new ReturnValueCompletion<ImageCacheVO>(msg) {
            @Override
            public void success(ImageCacheVO inv) {
                reply.setInventory(inv.toInventory());
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
    protected void check(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {}

    @Override
    protected void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        final CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
        checkCephFsId(msg.getPrimaryStorageUuid(), msg.getBackupStorageUuid());

        String volumeUuid = msg.getVolumeInventory().getUuid();
        String imageUuid = msg.getImageInventory().getUuid();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-snapshot-and-image-from-volume-%s", volumeUuid));
        chain.enableProgressReport();
        chain.preCheck(data -> buildErrIfCanceled());
        chain.then(new ShareFlow() {

            VolumeSnapshotInventory snapshot;
            CreateTemplateFromVolumeSnapshotReply imageReply;

            @Override
            public void setup() {

                flow(new Flow() {
                    String __name__ = "create-volume-snapshot";

                    @Override
                    public boolean skip(Map data) {
                        if (msg instanceof CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) {
                            CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg vsmsg = (CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) msg;
                            VolumeSnapshotVO snapshotVO = dbf.findByUuid(vsmsg.getSnapshotUuid(), VolumeSnapshotVO.class);
                            snapshot = VolumeSnapshotInventory.valueOf(snapshotVO);
                            return true;
                        }
                        return false;
                    }
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        String volumeAccountUuid = acntMgr.getOwnerAccountUuidOfResource(volumeUuid);

                        // 1. create snapshot
                        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                        cmsg.setName("Snapshot-" + volumeUuid);
                        cmsg.setDescription("Take snapshot for " + volumeUuid);
                        cmsg.setVolumeUuid(volumeUuid);
                        cmsg.setAccountUuid(volumeAccountUuid);

                        bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply r) {
                                if (!r.isSuccess()) {
                                    trigger.fail(r.getError());
                                    return;
                                }

                                CreateVolumeSnapshotReply createVolumeSnapshotReply = (CreateVolumeSnapshotReply)r;
                                snapshot = createVolumeSnapshotReply.getInventory();

                                CephPrimaryStorageCanonicalEvents.ImageInnerSnapshotCreated data = new CephPrimaryStorageCanonicalEvents.ImageInnerSnapshotCreated();
                                data.imageUuid = imageUuid;
                                data.primaryStorageUuid = self.getUuid();
                                data.snapshot = snapshot;
                                data.fire();

                                trigger.next();
                            }
                        });

                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (msg instanceof CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg ||
                                !PrimaryStorageGlobalConfig.UNDO_TEMP_SNAPSHOT.value(Boolean.class)) {
                            trigger.rollback();
                            return;
                        }
                        VolumeSnapshotDeletionMsg dmsg = new VolumeSnapshotDeletionMsg();
                        dmsg.setTreeUuid(snapshot.getTreeUuid());
                        dmsg.setVolumeUuid(snapshot.getVolumeUuid());
                        dmsg.setSnapshotUuid(snapshot.getUuid());
                        dmsg.setDirection(DeleteVolumeSnapshotDirection.Commit.toString());
                        dmsg.setScope(DeleteVolumeSnapshotScope.Single.toString());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeSnapshotConstant.SERVICE_ID, snapshot.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(msg) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-template-from-volume-snapshot";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        VolumeSnapshotVO vo = dbf.findByUuid(snapshot.getUuid(), VolumeSnapshotVO.class);
                        String treeUuid = vo.getTreeUuid();

                        CreateTemplateFromVolumeSnapshotMsg cmsg = new CreateTemplateFromVolumeSnapshotMsg();
                        cmsg.setSnapshotUuid(snapshot.getUuid());
                        cmsg.setImageUuid(imageUuid);
                        cmsg.setVolumeUuid(snapshot.getVolumeUuid());
                        cmsg.setTreeUuid(treeUuid);
                        cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());

                        String resourceUuid = volumeUuid != null ? volumeUuid : treeUuid;
                        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeSnapshotConstant.SERVICE_ID, resourceUuid);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply r) {
                                if (!r.isSuccess()) {
                                    trigger.fail(r.getError());
                                    return;
                                }

                                imageReply = (CreateTemplateFromVolumeSnapshotReply)r;
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-temp-snapshot";

                    @Override
                    public boolean skip(Map data) {
                        return msg instanceof CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg ||
                                !PrimaryStorageGlobalConfig.UNDO_TEMP_SNAPSHOT.value(Boolean.class);
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VolumeSnapshotDeletionMsg dmsg = new VolumeSnapshotDeletionMsg();
                        dmsg.setTreeUuid(snapshot.getTreeUuid());
                        dmsg.setVolumeUuid(snapshot.getVolumeUuid());
                        dmsg.setSnapshotUuid(snapshot.getUuid());
                        dmsg.setScope(DeleteVolumeSnapshotScope.Single.toString());
                        dmsg.setDirection(DeleteVolumeSnapshotDirection.Commit.toString());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeSnapshotConstant.SERVICE_ID, snapshot.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(msg) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        logger.debug(String.format("successfully create template[uuid:%s] from volume[uuid:%s]", imageUuid, volumeUuid));
                        reply.setTemplateBackupStorageInstallPath(imageReply.getBackupStorageInstallPath());
                        reply.setFormat(snapshot.getFormat());
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.warn(String.format("failed to create template from volume[uuid:%s], because %s", volumeUuid, errCode));
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(CancelJobOnPrimaryStorageMsg msg) {
        inQueue().name(String.format("cancel-job-on-primarystorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> cancelJobOnPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    protected void cancelJobOnPrimaryStorage(CancelJobOnPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        final CancelJobOnPrimaryStorageReply reply = new CancelJobOnPrimaryStorageReply();
        CancelCmd cmd = new CancelCmd();
        cmd.setCancellationApiId(msg.getCancellationApiId());
        new HttpCaller<>(AgentConstant.CANCEL_JOB, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(msg) {
            @Override
            public void success(AgentResponse rsp) {
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        }).specifyOrder(msg.getCancellationApiId()).tryNext().call();
    }

    @Override
    protected void handle(DownloadDataVolumeToPrimaryStorageMsg msg) {
        inQueue().name(String.format("download-data-volume-to-primarystorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> downloadDataVolumeToPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    protected void downloadDataVolumeToPrimaryStorage(final DownloadDataVolumeToPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        final DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();

        BackupStorageMediator mediator = getBackupStorageMediator(msg.getBackupStorageRef().getBackupStorageUuid());
        ImageSpec spec = new ImageSpec();
        spec.setInventory(msg.getImage());
        spec.setSelectedBackupStorage(msg.getBackupStorageRef());
        MediatorDownloadParam param = new MediatorDownloadParam();
        param.setImage(spec);
        param.setInstallPath(makeVolumeInstallPathByTargetPool(msg.getVolumeUuid(),
                getTargetPoolNameFromAllocatedUrl(msg.getAllocatedInstallUrl())));
        param.setPrimaryStorageUuid(self.getUuid());
        param.setShareable(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class).isShareable());
        mediator.param = param;
        mediator.download(new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String returnValue) {
                reply.setInstallPath(returnValue);
                reply.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    @Override
    protected void handle(GetInstallPathForDataVolumeDownloadMsg msg) {
        GetInstallPathForDataVolumeDownloadReply reply = new GetInstallPathForDataVolumeDownloadReply();
        reply.setInstallPath(makeVolumeInstallPathByTargetPool(msg.getVolumeUuid(),
                getTargetPoolNameFromAllocatedUrl(msg.getAllocatedInstallUrl())));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(DeleteVolumeBitsOnPrimaryStorageMsg msg) {
        inQueue().name(String.format("delete-volume-bits-on-primarystorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> deleteVolumeBitsOnPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    protected void deleteVolumeBitsOnPrimaryStorage(final DeleteVolumeBitsOnPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        DeleteCmd cmd = new DeleteCmd();
        cmd.installPath = msg.getInstallPath();

        final DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();

        httpCall(DELETE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(DeleteRsp ret) {
                osdHelper.releaseAvailableCapWithRatio(msg.getInstallPath(), msg.getSize());
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    @Override
    protected void handle(final DownloadIsoToPrimaryStorageMsg msg) {
        final DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
        DownloadToCache downloadToCache = new DownloadToCache();
        downloadToCache.image = msg.getIsoSpec();
        downloadToCache.download(new ReturnValueCompletion<ImageCacheVO>(msg) {
            @Override
            public void success(ImageCacheVO returnValue) {
                reply.setInstallPath(returnValue.getInstallUrl());
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
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {
        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        VolumeSnapshotCapability cap = new VolumeSnapshotCapability();

        String volumeType = msg.getVolume().getType();
        if (VolumeType.Data.toString().equals(volumeType) || VolumeType.Root.toString().equals(volumeType)) {
            cap.setSupport(true);
            cap.setArrangementType(VolumeSnapshotArrangementType.INDIVIDUAL);
            cap.setVolumePathFromInnerSnapshotRegex("^[^@]+");
        } else if (VolumeType.Memory.toString().equals(volumeType)) {
            cap.setSupport(false);
        } else {
            throw new CloudRuntimeException(String.format("unknown volume type %s", volumeType));
        }

        reply.setCapability(cap);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(SyncVolumeSizeOnPrimaryStorageMsg msg) {
        SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
        syncVolumeSize(msg.getVolumeUuid(), msg.getInstallPath(), new ReturnValueCompletion<GetVolumeSizeRsp>(msg) {
            @Override
            public void success(GetVolumeSizeRsp rsp) {
                markVolumeActualSize(msg.getVolumeUuid(), rsp.actualSize);

                // some ceph version has no way to get actual size
                long asize = rsp.actualSize != null ? rsp.actualSize : Q.New(VolumeVO.class)
                        .select(VolumeVO_.actualSize)
                        .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                        .findValue();
                reply.setActualSize(asize);
                reply.setSize(rsp.size);
                reply.setWithInternalSnapshot(true);
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
    protected void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg) {
        EstimateVolumeTemplateSizeOnPrimaryStorageReply reply = new EstimateVolumeTemplateSizeOnPrimaryStorageReply();
        // TODO: estimate template size
        syncVolumeSize(msg.getVolumeUuid(), msg.getInstallPath(), new ReturnValueCompletion<GetVolumeSizeRsp>(msg) {
            @Override
            public void success(GetVolumeSizeRsp rsp) {
                markVolumeActualSize(msg.getVolumeUuid(), rsp.actualSize);

                // some ceph version has no way to get actual size
                long asize = rsp.actualSize != null ? rsp.actualSize : Q.New(VolumeVO.class)
                        .select(VolumeVO_.actualSize)
                        .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                        .findValue();
                reply.setActualSize(asize);
                reply.setSize(rsp.size);
                reply.setWithInternalSnapshot(true);
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
    protected void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg) {
        inQueue().name(String.format("batch-sync-volume-size-on-primarystorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> BatchSyncVolumeSizeOnPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    protected void syncVolumeSize(String volumeUuid, String installPath, final ReturnValueCompletion<GetVolumeSizeRsp> completion) {
        final SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
        GetVolumeSizeCmd cmd = new GetVolumeSizeCmd();
        cmd.fsId = getSelf().getFsid();
        cmd.uuid = self.getUuid();
        cmd.volumeUuid = volumeUuid;
        cmd.installPath = installPath;

        httpCall(GET_VOLUME_SIZE_PATH, cmd, GetVolumeSizeRsp.class, completion);
    }

    private void BatchSyncVolumeSizeOnPrimaryStorage(BatchSyncVolumeSizeOnPrimaryStorageMsg msg, NoErrorCompletion completion) {
        BatchSyncVolumeSizeOnPrimaryStorageReply reply = new BatchSyncVolumeSizeOnPrimaryStorageReply();

        GetBatchVolumeSizeCmd cmd = new GetBatchVolumeSizeCmd();
        cmd.volumeUuidInstallPaths = msg.getVolumeUuidInstallPaths();
        cmd.fsId = getSelf().getFsid();
        cmd.uuid = self.getUuid();

        httpCall(BATCH_GET_VOLUME_SIZE_PATH, cmd, GetBatchVolumeSizeRsp.class, new ReturnValueCompletion<GetBatchVolumeSizeRsp>(msg) {
            @Override
            public void success(GetBatchVolumeSizeRsp rsp) {
                for (Map.Entry<String, Long> e : rsp.actualSizes.entrySet()) {
                    String volumeUuid = e.getKey();
                    Long actualSize = e.getValue();
                    markVolumeActualSize(volumeUuid, actualSize);
                }

                reply.setActualSizes(rsp.actualSizes);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });

    }

    private void markVolumeActualSize(String volumeUuid, Long actualSize) {
        boolean flag = VolumeSystemTags.NOT_SUPPORT_ACTUAL_SIZE_FLAG.hasTag(volumeUuid);

        if (actualSize == null && flag) {
            return;
        }

        if (actualSize == null) {
            SystemTagCreator creator = VolumeSystemTags.NOT_SUPPORT_ACTUAL_SIZE_FLAG.newSystemTagCreator(volumeUuid);
            creator.setTagByTokens(map(e( VolumeSystemTags.NOT_SUPPORT_ACTUAL_SIZE_FLAG_TOKEN, true)));
            creator.unique = true;
            creator.create();
            return;
        }

        if (flag) {
            VolumeSystemTags.NOT_SUPPORT_ACTUAL_SIZE_FLAG.delete(volumeUuid);
        }
    }

    protected <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback) {
        httpCall(path, cmd, retClass, callback, null, 0);
    }

    protected <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback, TimeUnit unit, long timeout) {
        new HttpCaller<>(path, cmd, retClass, callback, unit, timeout).call();
    }

    public class HttpCaller<T extends AgentResponse> {
        private Iterator<CephPrimaryStorageMonBase> it;
        private final List<CephPrimaryStorageMonVO> monVOs;
        private final ErrorCodeList errorCodes = new ErrorCodeList();

        private final String path;
        private final AgentCommand cmd;
        private final Class<T> retClass;
        private final ReturnValueCompletion<T> callback;
        private final TimeUnit unit;
        private final long timeout;

        private boolean tryNext = false;

        public HttpCaller(String path, AgentCommand cmd, Class<T> retClass, ReturnValueCompletion<T> callback) {
            this(path, cmd, retClass, callback, null, 0);
        }

        public HttpCaller(String path, AgentCommand cmd, Class<T> retClass, ReturnValueCompletion<T> callback, TimeUnit unit, long timeout) {
            this.path = path;
            this.cmd = cmd;
            this.retClass = retClass;
            this.callback = callback;
            this.unit = unit;
            this.timeout = timeout;
            this.monVOs = prepareMons();
        }

        public void call() {
            prepareMonsIterator();
            prepareCmd();
            doCall();
        }

        // specify mons order by randomFactor to ensure that the same mon receive cmd every time.
        HttpCaller<T> specifyOrder(String randomFactor) {
            CollectionUtils.shuffleByKeySeed(monVOs, randomFactor, ResourceVO::getUuid);
            return this;
        }

        HttpCaller<T> specifyOrder(Comparator<? super CephPrimaryStorageMonVO> c) {
            monVOs.sort(c);
            return this;
        }

        HttpCaller<T> tryNext() {
            this.tryNext = true;
            return this;
        }

        HttpCaller<T> setAvoidMonUuids(List<String> avoidMonUuids) {
            if (monVOs.size() > 1 && avoidMonUuids != null) {
                monVOs.removeIf(it -> avoidMonUuids.contains(it.getUuid()));
            }
            return this;
        }

        private void prepareCmd() {
            if (cmd instanceof DeleteCmd) {
                ((DeleteCmd) cmd).setExpirationTime(CephGlobalConfig.IMAGE_EXPIRATION_TIME.value(Long.class));
            }
            cmd.setUuid(self.getUuid());
            cmd.setFsId(getSelf().getFsid());
        }

        private List<CephPrimaryStorageMonVO> prepareMons() {
            final List<CephPrimaryStorageMonVO> mons = new ArrayList<>(getSelf().getMons());

            Collections.shuffle(mons);

            mons.removeIf(it -> it.getStatus() != MonStatus.Connected);
            if (mons.isEmpty()) {
                throw new OperationFailureException(operr(
                        "all ceph mons of primary storage[uuid:%s] are not in Connected state", self.getUuid())
                );
            }

            return mons;
        }

        private void prepareMonsIterator() {
            it = monVOs.stream().map(CephPrimaryStorageMonBase::new).collect(Collectors.toList()).iterator();
        }

        private void doCall() {
            if (!it.hasNext()) {
                callback.fail(multiErr(errorCodes, "all monitors cannot execute http call[%s]", path)
                );

                return;
            }

            CephPrimaryStorageMonBase base = it.next();
            cmd.monUuid = base.getSelf().getUuid();
            cmd.monIp = base.getSelf().getHostname();

            ReturnValueCompletion<T> completion = new ReturnValueCompletion<T>(callback) {
                @Override
                public void success(T ret) {
                    if (!(cmd instanceof InitCmd)) {
                        updateCapacityIfNeeded(ret);
                    }

                    callback.success(ret);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    if (!errorCode.isError(SysErrors.OPERATION_ERROR) && !errorCode.isError(VolumeErrors.VOLUME_IN_USE)
                            && !errorCode.isError(SysErrors.TIMEOUT)) {
                        logger.warn(String.format("mon[%s] failed to execute http call[%s], error is: %s",
                                base.getSelf().getHostname(), path, JSONObjectUtil.toJsonString(errorCode)));
                        errorCodes.add(errorCode);
                        doCall();
                        return;
                    }

                    if (tryNext) {
                        doCall();
                    } else {
                        callback.fail(errorCode);
                    }
                }
            };

            if (unit == null) {
                base.httpCall(path, cmd, retClass, completion);
            } else {
                base.httpCall(path, cmd, retClass, completion, unit, timeout);
            }
        }

        private void updateCapacityIfNeeded(AgentResponse rsp) {
            if (rsp.totalCapacity != null && rsp.availableCapacity != null) {
                CephCapacity cephCapacity = new CephCapacity(getSelf().getFsid(), rsp);
                new CephCapacityUpdater().update(cephCapacity);
            }
        }
    }

    private void connect(final boolean newAdded, final Completion completion) {
        final List<CephPrimaryStorageMonBase> mons = transform(getSelf().getMons(), CephPrimaryStorageMonBase::new);

        class Connector {
            private final ErrorCodeList errorCodes = new ErrorCodeList();
            private final Iterator<CephPrimaryStorageMonBase> it = mons.iterator();

            void connect(final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    if (errorCodes.size() == mons.size()) {
                        if (errorCodes.hasError()) {
                            trigger.fail(operr("unable to connect to the ceph primary storage[uuid:%s]," +
                                    " failed to connect all ceph monitors.", self.getUuid()));
                        } else {
                            trigger.fail(multiErr(errorCodes, "unable to connect to the ceph primary storage[uuid:%s]," +
                                            " failed to connect all ceph monitors.",
                                    self.getUuid()));
                        }
                    } else {
                        // reload because mon status changed
                        PrimaryStorageVO vo = dbf.reload(self);
                        if (vo == null) {
                            if (newAdded) {
                                if (!getSelf().getMons().isEmpty()) {
                                    dbf.removeCollection(getSelf().getMons(), CephPrimaryStorageMonVO.class);
                                }
                            }
                            trigger.fail(operr("ceph primary storage[uuid:%s] may have been deleted.", self.getUuid()));
                        } else {
                            self = vo;
                            trigger.next();
                        }
                    }

                    return;
                }

                final CephPrimaryStorageMonBase base = it.next();
                base.connect(new Completion(trigger) {
                    @Override
                    public void success() {
                        connect(trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        errorCodes.add(errorCode);

                        if (newAdded) {
                            // the mon fails to connect, remove it
                            dbf.remove(base.getSelf());
                        }

                        connect(trigger);
                    }
                });
            }
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-ceph-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "connect-monitor";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new Connector().connect(trigger);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "check-mon-integrity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final Map<String, String> fsids = new HashMap<>();

                        final List<CephPrimaryStorageMonBase> mons = transformAndRemoveNull(getSelf().getMons(),
                                arg -> arg.getStatus() == MonStatus.Connected ? new CephPrimaryStorageMonBase(arg) : null);

                        DebugUtils.Assert(!mons.isEmpty(), "how can be no connected MON !!!???");

                        List<ErrorCode> errors = new ArrayList<>();
                        new While<>(mons).each((mon, compl) -> {
                            GetFactsCmd cmd = new GetFactsCmd();
                            cmd.uuid = self.getUuid();
                            cmd.monUuid = mon.getSelf().getUuid();
                            mon.httpCall(GET_FACTS, cmd, GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(compl) {
                                @Override
                                public void success(GetFactsRsp rsp) {
                                    CephPrimaryStorageMonVO monVO = dbf.reload(mon.getSelf());
                                    if (monVO != null) {
                                        fsids.put(monVO.getUuid(), rsp.fsid);
                                        monVO.setMonAddr(rsp.monAddr == null ? monVO.getHostname() : rsp.monAddr);
                                        dbf.update(monVO);

                                        if (rsp.ipAddresses != null) {
                                            updateMonExtraIps(monVO, rsp.ipAddresses);
                                        }
                                    }
                                    compl.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    // one mon cannot get the facts, directly error out
                                    errors.add(errorCode);
                                    compl.allDone();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errors.isEmpty()) {
                                    trigger.fail(errors.get(0));
                                    return;
                                }

                                Set<String> set = new HashSet<>(fsids.values());

                                if (set.size() != 1) {
                                    StringBuilder sb = new StringBuilder(i18n("the fsid returned by mons are mismatching, it seems the mons belong to different ceph clusters:\n"));
                                    for (CephPrimaryStorageMonBase mon : mons) {
                                        String fsid = fsids.get(mon.getSelf().getUuid());
                                        sb.append(String.format("%s (mon ip) --> %s (fsid)\n", mon.getSelf().getHostname(), fsid));
                                    }

                                    throw new OperationFailureException(operr(sb.toString()));
                                }

                                // check if there is another ceph setup having the same fsid
                                String fsId = set.iterator().next();

                                SimpleQuery<CephPrimaryStorageVO> q = dbf.createQuery(CephPrimaryStorageVO.class);
                                q.add(CephPrimaryStorageVO_.fsid, Op.EQ, fsId);
                                q.add(CephPrimaryStorageVO_.uuid, Op.NOT_EQ, self.getUuid());
                                CephPrimaryStorageVO otherCeph = q.find();
                                if (otherCeph != null) {
                                    throw new OperationFailureException(
                                            operr("there is another CEPH primary storage[name:%s, uuid:%s] with the same" +
                                                            " FSID[%s], you cannot add the same CEPH setup as two different primary storage",
                                                    otherCeph.getName(), otherCeph.getUuid(), fsId)
                                    );
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "check_pool";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<Pool> pools = new ArrayList<Pool>();
                        String primaryStorageUuid = self.getUuid();

                        Pool p = new Pool();
                        p.name = getDefaultImageCachePoolName();
                        p.predefined = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.hasTag(primaryStorageUuid);
                        pools.add(p);

                        p = new Pool();
                        p.name = getDefaultRootVolumePoolName();
                        p.predefined = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.hasTag(primaryStorageUuid);
                        pools.add(p);

                        p = new Pool();
                        p.name = getDefaultDataVolumePoolName();
                        p.predefined = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.hasTag(primaryStorageUuid);
                        pools.add(p);

                        if(!newAdded){
                            CheckCmd check = new CheckCmd();
                            check.setPools(pools);
                            httpCall(CHECK_POOL_PATH, check, CheckRsp.class, new ReturnValueCompletion<CheckRsp>(trigger) {
                                @Override
                                public void fail(ErrorCode err) {
                                    trigger.fail(err);
                                }

                                @Override
                                public void success(CheckRsp ret) {
                                    trigger.next();
                                }
                            });
                        }else {
                            trigger.next();
                        }
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = "init";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<Pool> pools = new ArrayList<Pool>();
                        String primaryStorageUuid = self.getUuid();

                        Pool p = new Pool();
                        p.name = getDefaultImageCachePoolName();
                        p.predefined = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL.hasTag(primaryStorageUuid);
                        pools.add(p);

                        p = new Pool();
                        p.name = getDefaultRootVolumePoolName();
                        p.predefined = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL.hasTag(primaryStorageUuid);
                        pools.add(p);

                        p = new Pool();
                        p.name = getDefaultDataVolumePoolName();
                        p.predefined = CephSystemTags.PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL.hasTag(primaryStorageUuid);
                        pools.add(p);

                        InitCmd cmd = new InitCmd();
                        if (CephSystemTags.NO_CEPHX.hasTag(primaryStorageUuid)) {
                            cmd.nocephx = true;
                        }
                        cmd.pools = pools;
                        httpCall(INIT_PATH, cmd, InitRsp.class, new ReturnValueCompletion<InitRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(InitRsp ret) {
                                if (getSelf().getFsid() == null) {
                                    getSelf().setFsid(ret.fsid);
                                }

                                getSelf().setUserKey(ret.userKey);
                                self = dbf.updateAndRefresh(self);

                                if (!Strings.isEmpty(ret.manufacturer)) {
                                    SystemTagCreator creator = CephSystemTags.CEPH_MANUFACTURER.newSystemTagCreator(self.getUuid());
                                    creator.setTagByTokens(map(e(CephSystemTags.CEPH_MANUFACTURER_TOKEN, ret.manufacturer)));
                                    creator.inherent = true;
                                    creator.ignoreIfExisting = true;
                                    creator.recreate = true;
                                    creator.create();
                                }

                                CephCapacityUpdater updater = new CephCapacityUpdater();
                                CephCapacity cephCapacity = new CephCapacity(ret.fsid, ret);
                                updater.update(cephCapacity, true);
                                osdHelper.recalculateAvailableCapacity();
                                createPrimaryStorageLicenseVendor(ret.getType());
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        if (newAdded) {
                            PrimaryStorageVO vo = dbf.reload(self);
                            if (vo != null) {
                                self = vo;
                            }
                            if (!getSelf().getMons().isEmpty()) {
                                dbf.removeCollection(getSelf().getMons(), CephPrimaryStorageMonVO.class);
                            }

                            if (!getSelf().getPools().isEmpty()) {
                                dbf.removeCollection(getSelf().getPools(), CephPrimaryStoragePoolVO.class);
                            }
                        }

                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void updateMonExtraIps(CephPrimaryStorageMonVO mon, List<String> ips) {
        ips.remove(mon.getHostname());
        if (CoreGlobalProperty.MN_VIP != null) {
            ips.remove(CoreGlobalProperty.MN_VIP);
        }
        if (ips.isEmpty()) {
            CephMonSystemTags.EXTRA_IPS.delete(mon.getUuid());
            return;
        }
        recreateNonInherentTag(
                CephMonSystemTags.EXTRA_IPS, CephMonSystemTags.EXTRA_IPS_TOKEN,
                StringUtils.join(ips, ","), mon.getUuid());
    }

    @Override
    protected void connectHook(ConnectParam param, final Completion completion) {
        connect(param.isNewAdded(), completion);
    }

    @Override
    protected void pingHook(final Completion completion) {
        final List<CephPrimaryStorageMonBase> mons = getSelf().getMons().stream()
                .filter(mon -> !mon.getStatus().equals(MonStatus.Connecting)).map(CephPrimaryStorageMonBase::new).collect(Collectors.toList());
        final List<ErrorCode> errors = new ArrayList<ErrorCode>();

        class Ping {
            final private AtomicBoolean replied = new AtomicBoolean(false);

            @AsyncThread
            private void reconnectMon(final CephPrimaryStorageMonBase mon, boolean delay) {
                if (!CephGlobalConfig.PRIMARY_STORAGE_MON_AUTO_RECONNECT.value(Boolean.class)) {
                    logger.debug(String.format("do not reconnect the ceph primary storage mon[uuid:%s] as the global config[%s] is set to false",
                            self.getUuid(), CephGlobalConfig.PRIMARY_STORAGE_MON_AUTO_RECONNECT.getCanonicalName()));
                    return;
                }

                // there has been a reconnection in process
                if (!reconnectMonLock.lock()) {
                    logger.debug(String.format("ignore this call, reconnect ceph primary storage mon[uuid:%s] is in process", self.getUuid()));
                    return;
                }

                final NoErrorCompletion releaseLock = new NoErrorCompletion() {
                    @Override
                    public void done() {
                        reconnectMonLock.unlock();
                    }
                };

                try {
                    if (delay) {
                        try {
                            TimeUnit.SECONDS.sleep(CephGlobalConfig.PRIMARY_STORAGE_MON_RECONNECT_DELAY.value(Long.class));
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    mon.connect(new Completion(releaseLock) {
                        @Override
                        public void success() {
                            logger.debug(String.format("successfully reconnected the mon[uuid:%s] of the ceph primary" +
                                    " storage[uuid:%s, name:%s]", mon.getSelf().getUuid(), self.getUuid(), self.getName()));
                            releaseLock.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("failed to reconnect the mon[uuid:%s] server of the ceph primary" +
                                    " storage[uuid:%s, name:%s], %s", mon.getSelf().getUuid(), self.getUuid(), self.getName(), errorCode));
                            releaseLock.done();
                        }
                    });
                } catch (Throwable t) {
                    releaseLock.done();
                    logger.warn(t.getMessage(), t);
                    Thread.currentThread().interrupt();
                }
            }

            private void ping() {
                final List<String> monUuids = mons.stream()
                        .map(m -> m.getSelf().getMonAddr())
                        .collect(Collectors.toList());
                logger.info(String.format("ceph-ps-ping-mon: %s", String.join(",", monUuids)));

                // this is only called when all mons are disconnected
                final AsyncLatch latch = new AsyncLatch(mons.size(), new NoErrorCompletion() {
                    @Override
                    public void done() {
                        if (!replied.compareAndSet(false, true)) {
                            return;
                        }

                        ErrorCode err = errf.stringToOperationError(String.format("failed to ping the ceph primary storage[uuid:%s, name:%s]",
                                self.getUuid(), self.getName()), errors);
                        completion.fail(err);
                    }
                });

                for (final CephPrimaryStorageMonBase mon : mons) {
                    mon.ping(new ReturnValueCompletion<PingResult>(latch) {
                        private void thisMonIsDown(ErrorCode err) {
                            //TODO
                            logger.warn(String.format("cannot ping mon[uuid:%s] of the ceph primary storage[uuid:%s, name:%s], %s",
                                    mon.getSelf().getUuid(), self.getUuid(), self.getName(), err));
                            errors.add(err);
                            mon.changeStatus(MonStatus.Disconnected);
                            reconnectMon(mon, true);
                            latch.ack();
                        }

                        @Override
                        public void success(PingResult res) {
                            if (res.success) {
                                // as long as there is one mon working, the primary storage works
                                pingSuccess();

                                if (mon.getSelf().getStatus() == MonStatus.Disconnected) {
                                    reconnectMon(mon, false);
                                }

                            } else if (PingOperationFailure.UnableToCreateFile.toString().equals(res.failure)) {
                                // as long as there is one mon saying the ceph not working, the primary storage goes down
                                ErrorCode err = operr("the ceph primary storage[uuid:%s, name:%s] is down, as one mon[uuid:%s] reports" +
                                        " an operation failure[%s]", self.getUuid(), self.getName(), mon.getSelf().getUuid(), res.error);
                                errors.add(err);
                                primaryStorageDown();
                            } else if (!res.success || PingOperationFailure.MonAddrChanged.toString().equals(res.failure)) {
                                // this mon is down(success == false, operationFailure == false), but the primary storage may still work as other mons may work
                                ErrorCode errorCode = operr("operation error, because:%s", res.error);
                                thisMonIsDown(errorCode);
                            } else {
                                throw new CloudRuntimeException("should not be here");
                            }
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            thisMonIsDown(errorCode);
                        }
                    });
                }
            }

            // this is called once a mon return an operation failure
            private void primaryStorageDown() {
                if (!replied.compareAndSet(false, true)) {
                    return;
                }

                // set all mons to be disconnected
                for (CephPrimaryStorageMonBase base : mons) {
                    base.changeStatus(MonStatus.Disconnected);
                }

                ErrorCode err = errf.stringToOperationError(String.format("failed to ping the ceph primary storage[uuid:%s, name:%s]",
                        self.getUuid(), self.getName()), errors);
                completion.fail(err);
            }


            private void pingSuccess() {
                if (!replied.compareAndSet(false, true)) {
                    return;
                }

                completion.success();
            }
        }

        new Ping().ping();
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        PrimaryStorageCapacityVO cap = dbf.findByUuid(self.getUuid(), PrimaryStorageCapacityVO.class);
        PhysicalCapacityUsage usage = new PhysicalCapacityUsage();
        usage.availablePhysicalSize = cap.getAvailablePhysicalCapacity();
        usage.totalPhysicalSize = cap.getTotalPhysicalCapacity();
        completion.success(usage);
    }

    @Override
    protected void handle(ShrinkVolumeSnapshotOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg) {
        GetVolumeSnapshotEncryptedOnPrimaryStorageReply reply = new GetVolumeSnapshotEncryptedOnPrimaryStorageReply();
        reply.setEncrypt(msg.getPrimaryStorageInstallPath());
        reply.setSnapshotUuid(msg.getSnapshotUuid());
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(APIReconnectPrimaryStorageMsg msg) {
        final APIReconnectPrimaryStorageEvent evt = new APIReconnectPrimaryStorageEvent(msg.getId());

        ReconnectPrimaryStorageMsg rmsg = new ReconnectPrimaryStorageMsg();
        rmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, rmsg.getPrimaryStorageUuid());
        bus.send(rmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                } else {
                    self = dbf.reload(self);
                    evt.setInventory(getSelfInventory());
                }

                bus.publish(evt);
            }
        });
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddMonToCephPrimaryStorageMsg) {
            handle((APIAddMonToCephPrimaryStorageMsg) msg);
        } else if (msg instanceof APIRemoveMonFromCephPrimaryStorageMsg) {
            handle((APIRemoveMonFromCephPrimaryStorageMsg) msg);
        } else if (msg instanceof APIUpdateCephPrimaryStorageMonMsg) {
            handle((APIUpdateCephPrimaryStorageMonMsg) msg);
        } else if (msg instanceof APIAddCephPrimaryStoragePoolMsg) {
            handle((APIAddCephPrimaryStoragePoolMsg) msg);
        } else if (msg instanceof APIDeleteCephPrimaryStoragePoolMsg) {
            handle((APIDeleteCephPrimaryStoragePoolMsg) msg);
        } else if (msg instanceof APIUpdateCephPrimaryStoragePoolMsg) {
            handle((APIUpdateCephPrimaryStoragePoolMsg) msg);
        } else if (msg instanceof APICleanUpTrashOnPrimaryStorageMsg) {
            handle((APICleanUpTrashOnPrimaryStorageMsg) msg);
        } else if (msg instanceof APICleanUpStorageTrashOnPrimaryStorageMsg) {
            handle((APICleanUpStorageTrashOnPrimaryStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIUpdateCephPrimaryStoragePoolMsg msg) {
        APIUpdateCephPrimaryStoragePoolEvent evt = new APIUpdateCephPrimaryStoragePoolEvent(msg.getId());

        CephPrimaryStoragePoolVO vo = dbf.findByUuid(msg.getUuid(), CephPrimaryStoragePoolVO.class);
        if (msg.getAliasName() != null) {
            vo.setAliasName(msg.getAliasName());
        }

        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }

        dbf.update(vo);
        evt.setInventory(CephPrimaryStoragePoolInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIDeleteCephPrimaryStoragePoolMsg msg) {
        APIDeleteCephPrimaryStoragePoolEvent evt = new APIDeleteCephPrimaryStoragePoolEvent(msg.getId());

        CephPrimaryStoragePoolVO vo = dbf.findByUuid(msg.getUuid(), CephPrimaryStoragePoolVO.class);
        dbf.remove(vo);
        osdHelper.recalculateAvailableCapacity();

        for(CephPrimaryStorageMonAfterModifiedExtensionPoint ext : pluginRgty.getExtensionList(CephPrimaryStorageMonAfterModifiedExtensionPoint.class)) {
            ext.afterModified(getSelf());
        }

        bus.publish(evt);
    }

    private void handle(APIAddCephPrimaryStoragePoolMsg msg) {
        inQueue().name(String.format("add-ceph-primarystorage-pool-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> addCephPrimaryStoragePool(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void addCephPrimaryStoragePool(APIAddCephPrimaryStoragePoolMsg msg, final NoErrorCompletion completion) {
        CephPrimaryStoragePoolVO vo = new CephPrimaryStoragePoolVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setDescription(msg.getDescription());
        vo.setPoolName(msg.getPoolName());
        if (msg.getAliasName() != null) {
            vo.setAliasName(msg.getAliasName());
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }
        vo.setType(msg.getType());
        vo.setPrimaryStorageUuid(self.getUuid());
        vo = dbf.persistAndRefresh(vo);

        AddPoolCmd cmd = new AddPoolCmd();
        cmd.isCreate = msg.isCreate();
        cmd.poolName = msg.getPoolName();

        APIAddCephPrimaryStoragePoolEvent evt = new APIAddCephPrimaryStoragePoolEvent(msg.getId());
        CephPrimaryStoragePoolVO finalVo = vo;
        httpCall(ADD_POOL_PATH, cmd, AddPoolRsp.class, new ReturnValueCompletion<AddPoolRsp>(msg) {
            @Override
            public void success(AddPoolRsp rsp) {
                osdHelper.recalculateAvailableCapacity();

                for(CephPrimaryStorageMonAfterModifiedExtensionPoint ext : pluginRgty.getExtensionList(CephPrimaryStorageMonAfterModifiedExtensionPoint.class)) {
                    ext.afterModified(getSelf());
                }

                evt.setInventory(CephPrimaryStoragePoolInventory.valueOf(finalVo));
                bus.publish(evt);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                dbf.remove(finalVo);
                evt.setError(errorCode);
                bus.publish(evt);
                completion.done();
            }
        });
    }

    private void handle(APIRemoveMonFromCephPrimaryStorageMsg msg) {
        APIRemoveMonFromCephPrimaryStorageEvent evt = new APIRemoveMonFromCephPrimaryStorageEvent(msg.getId());

        SimpleQuery<CephPrimaryStorageMonVO> q = dbf.createQuery(CephPrimaryStorageMonVO.class);
        q.add(CephPrimaryStorageMonVO_.hostname, Op.IN, msg.getMonHostnames());
        List<CephPrimaryStorageMonVO> vos = q.list();

        dbf.removeCollection(vos, CephPrimaryStorageMonVO.class);
        evt.setInventory(CephPrimaryStorageInventory.valueOf(dbf.reload(getSelf())));
        bus.publish(evt);
    }

    private void handle(APIUpdateCephPrimaryStorageMonMsg msg) {
        final APIUpdateCephPrimaryStorageMonEvent evt = new APIUpdateCephPrimaryStorageMonEvent(msg.getId());
        CephPrimaryStorageMonVO monvo = dbf.findByUuid(msg.getMonUuid(), CephPrimaryStorageMonVO.class);
        boolean monParameterChanged = false;
        if (msg.getHostname() != null) {
            monvo.setHostname(msg.getHostname());
            monParameterChanged = true;
        }
        if (msg.getMonPort() != null && msg.getMonPort() > 0 && msg.getMonPort() <= 65535) {
            monvo.setMonPort(msg.getMonPort());
            monParameterChanged = true;
        }
        if (msg.getSshPort() != null && msg.getSshPort() > 0 && msg.getSshPort() <= 65535) {
            monvo.setSshPort(msg.getSshPort());
        }
        if (msg.getSshUsername() != null) {
            monvo.setSshUsername(msg.getSshUsername());
        }
        if (msg.getSshPassword() != null) {
            monvo.setSshPassword(msg.getSshPassword());
        }
        dbf.update(monvo);
        evt.setInventory(CephPrimaryStorageInventory.valueOf((dbf.reload(getSelf()))));
        bus.publish(evt);
        if (monParameterChanged) {
            for(CephPrimaryStorageMonAfterModifiedExtensionPoint ext : pluginRgty.getExtensionList(CephPrimaryStorageMonAfterModifiedExtensionPoint.class)) {
                ext.afterModified(getSelf());
            }
        }
    }

    private void handle(final APIAddMonToCephPrimaryStorageMsg msg) {
        final APIAddMonToCephPrimaryStorageEvent evt = new APIAddMonToCephPrimaryStorageEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-mon-ceph-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            final List<CephPrimaryStorageMonVO> monVOs = new ArrayList<CephPrimaryStorageMonVO>();

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-mon-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (String url : msg.getMonUrls()) {
                            CephPrimaryStorageMonVO monvo = new CephPrimaryStorageMonVO();
                            MonUri uri = new MonUri(url);
                            monvo.setUuid(Platform.getUuid());
                            monvo.setStatus(MonStatus.Connecting);
                            monvo.setHostname(uri.getHostname());
                            monvo.setMonAddr(monvo.getHostname());
                            monvo.setMonPort(uri.getMonPort());
                            monvo.setSshPort(uri.getSshPort());
                            monvo.setSshUsername(uri.getSshUsername());
                            monvo.setSshPassword(uri.getSshPassword());
                            monvo.setPrimaryStorageUuid(self.getUuid());
                            monVOs.add(monvo);
                        }

                        dbf.persistCollection(monVOs);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        dbf.removeCollection(monVOs, CephPrimaryStorageMonVO.class);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "connect-mons";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<CephPrimaryStorageMonBase> bases = transform(monVOs, CephPrimaryStorageMonBase::new);

                        final List<ErrorCode> errorCodes = new ArrayList<>();
                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                if (!errorCodes.isEmpty()) {
                                    trigger.fail(multiErr(errorCodes, "unable to connect mons"));
                                } else {
                                    trigger.next();
                                }
                            }
                        });

                        for (CephPrimaryStorageMonBase base : bases) {
                            base.connect(new Completion(trigger) {
                                @Override
                                public void success() {
                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    // one fails, all fail
                                    errorCodes.add(errorCode);
                                    latch.ack();
                                }
                            });
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "check-mon-integrity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<CephPrimaryStorageMonBase> bases = transform(monVOs, CephPrimaryStorageMonBase::new);

                        final List<ErrorCode> errors = new ArrayList<>();

                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                // one fail, all fail
                                if (!errors.isEmpty()) {
                                    trigger.fail(multiErr(errors, "unable to add mon to ceph primary storage"));
                                } else {
                                    trigger.next();
                                }
                            }
                        });

                        for (final CephPrimaryStorageMonBase base : bases) {
                            GetFactsCmd cmd = new GetFactsCmd();
                            cmd.uuid = self.getUuid();
                            cmd.monUuid = base.getSelf().getUuid();
                            base.httpCall(GET_FACTS, cmd, GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(latch) {
                                @Override
                                public void success(GetFactsRsp rsp) {
                                    String fsid = rsp.fsid;
                                    if (!getSelf().getFsid().equals(fsid)) {
                                        errors.add(operr("the mon[ip:%s] returns a fsid[%s] different from the current fsid[%s] of the cep cluster," +
                                                "are you adding a mon not belonging to current cluster mistakenly?", base.getSelf().getHostname(), fsid, getSelf().getFsid())
                                        );
                                    }
                                    CephPrimaryStorageMonVO monVO = dbf.reload(base.getSelf());
                                    if (monVO != null) {
                                        monVO.setMonAddr(rsp.monAddr == null ? monVO.getHostname() : rsp.monAddr);
                                        dbf.update(monVO);

                                        if (rsp.ipAddresses != null) {
                                            updateMonExtraIps(monVO, rsp.ipAddresses);
                                        }
                                    }

                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errors.add(errorCode);
                                    latch.ack();
                                }
                            });
                        }
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(CephPrimaryStorageInventory.valueOf(dbf.reload(getSelf())));
                        bus.publish(evt);

                        for(CephPrimaryStorageMonAfterModifiedExtensionPoint ext : pluginRgty.getExtensionList(CephPrimaryStorageMonAfterModifiedExtensionPoint.class)) {
                            ext.afterModified(getSelf());
                        }
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void recreateNonInherentTag(SystemTag tag, String token, String value, String resourceUuid) {
        recreateTag(tag, token, value, resourceUuid, false);
    }

    private void recreateTag(SystemTag tag, String token, String value, String resourceUuid, boolean inherent) {
        SystemTagCreator creator = tag.newSystemTagCreator(resourceUuid);
        Optional.ofNullable(token).ifPresent(it -> creator.setTagByTokens(Collections.singletonMap(token, value)));
        creator.inherent = inherent;
        creator.recreate = true;
        creator.create();
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof CheckSnapshotMsg) {
            handle((CheckSnapshotMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else if (msg instanceof CreateKvmSecretMsg) {
            handle((CreateKvmSecretMsg) msg);
        } else if (msg instanceof CheckHostStorageConnectionMsg) {
            handle((CheckHostStorageConnectionMsg) msg);
        } else if (msg instanceof UploadBitsToBackupStorageMsg) {
            handle((UploadBitsToBackupStorageMsg) msg);
        } else if (msg instanceof SetupSelfFencerOnKvmHostMsg) {
            handle((SetupSelfFencerOnKvmHostMsg) msg);
        } else if (msg instanceof CancelSelfFencerOnKvmHostMsg) {
            handle((CancelSelfFencerOnKvmHostMsg) msg);
        } else if (msg instanceof DeleteImageCacheOnPrimaryStorageMsg) {
            handle((DeleteImageCacheOnPrimaryStorageMsg) msg);
        } else if (msg instanceof PurgeSnapshotOnPrimaryStorageMsg) {
            handle((PurgeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateEmptyVolumeMsg) {
            handle((CreateEmptyVolumeMsg) msg);
        } else if (msg instanceof CephToCephMigrateVolumeSegmentMsg) {
            handle((CephToCephMigrateVolumeSegmentMsg) msg);
        } else if (msg instanceof GetVolumeSnapshotInfoMsg) {
            handle((GetVolumeSnapshotInfoMsg) msg);
        } else if (msg instanceof DownloadBitsFromKVMHostToPrimaryStorageMsg) {
            handle((DownloadBitsFromKVMHostToPrimaryStorageMsg) msg);
        } else if (msg instanceof DownloadBitsFromNbdToPrimaryStorageMsg) {
            handle((DownloadBitsFromNbdToPrimaryStorageMsg) msg);
        } else if (msg instanceof DownloadBitsFromRemoteTargetOnPrimaryStorageMsg) {
            handle((DownloadBitsFromRemoteTargetOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CancelDownloadBitsFromKVMHostToPrimaryStorageMsg) {
            handle((CancelDownloadBitsFromKVMHostToPrimaryStorageMsg) msg);
        } else if ((msg instanceof CleanUpTrashOnPrimaryStroageMsg)) {
            handle((CleanUpTrashOnPrimaryStroageMsg) msg);
        } else if ((msg instanceof GetPrimaryStorageLicenseInfoMsg)) {
            handle((GetPrimaryStorageLicenseInfoMsg) msg);
        } else if ((msg instanceof GetDownloadBitsFromKVMHostProgressMsg)) {
            handle((GetDownloadBitsFromKVMHostProgressMsg) msg);
        } else if (msg instanceof GetVolumeWatchersMsg) {
            handle((GetVolumeWatchersMsg) msg);
        } else if (msg instanceof GetVolumeBackingChainFromPrimaryStorageMsg) {
            handle((GetVolumeBackingChainFromPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteVolumeChainOnPrimaryStorageMsg) {
            handle((DeleteVolumeChainOnPrimaryStorageMsg) msg);
        } else if (msg instanceof GetPrimaryStorageUsageReportMsg) {
            handle((GetPrimaryStorageUsageReportMsg) msg);
        } else if (msg instanceof CleanUpStorageTrashOnPrimaryStorageMsg) {
            handle((CleanUpStorageTrashOnPrimaryStorageMsg)msg);
        } else {
            super.handleLocalMessage(msg);
        }

    }

    public static class CheckIsBitsExistingRsp extends AgentResponse {
        private boolean existing;

        public void setExisting(boolean existing) {
            this.existing = existing;
        }

        public boolean isExisting() {
            return existing;
        }
    }

    private void handle(DownloadBitsFromKVMHostToPrimaryStorageMsg msg) {
        inQueue().name(String.format("download-bits-from-kvm-host-to-primarystorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> downloadBitsFromKVMHostToPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void downloadBitsFromKVMHostToPrimaryStorage(DownloadBitsFromKVMHostToPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        DownloadBitsFromKVMHostToPrimaryStorageReply reply = new DownloadBitsFromKVMHostToPrimaryStorageReply();

        GetKVMHostDownloadCredentialMsg gmsg = new GetKVMHostDownloadCredentialMsg();
        gmsg.setHostUuid(msg.getSrcHostUuid());

        if (PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.hasTag(self.getUuid())) {
            gmsg.setDataNetworkCidr(PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.getTokenByResourceUuid(self.getUuid(), PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY_TOKEN));
        }

        bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, msg.getSrcHostUuid());
        bus.send(gmsg, new CloudBusCallBack(reply) {
            @Override
            public void run(MessageReply rly) {
                if (!rly.isSuccess()) {
                    reply.setError(rly.getError());
                    bus.reply(msg, reply);
                    return;
                }

                GetKVMHostDownloadCredentialReply grly = rly.castReply();
                DownloadBitsFromKVMHostCmd cmd = new DownloadBitsFromKVMHostCmd();
                cmd.setHostname(grly.getHostname());
                cmd.setUsername(grly.getUsername());
                cmd.setSshKey(grly.getSshKey());
                cmd.setSshPort(grly.getSshPort());
                cmd.setBackupStorageInstallPath(msg.getHostInstallPath());
                cmd.setPrimaryStorageInstallPath(msg.getPrimaryStorageInstallPath());
                cmd.setBandWidth(msg.getBandWidth());
                cmd.setIdentificationCode(msg.getLongJobUuid() + msg.getPrimaryStorageInstallPath());
                String randomFactor = msg.getLongJobUuid();

                new HttpCaller<>(DOWNLOAD_BITS_FROM_KVM_HOST_PATH, cmd, DownloadBitsFromKVMHostRsp.class, new ReturnValueCompletion<DownloadBitsFromKVMHostRsp>(reply) {
                    @Override
                    public void success(DownloadBitsFromKVMHostRsp returnValue) {
                        if (returnValue.isSuccess()) {
                            logger.info(String.format("successfully downloaded bits %s from kvm host %s to primary storage %s", cmd.getBackupStorageInstallPath(), msg.getSrcHostUuid(), msg.getPrimaryStorageUuid()));
                            reply.setFormat(returnValue.format);
                        } else {
                            logger.error(String.format("failed to download bits %s from kvm host %s to primary storage %s", cmd.getBackupStorageInstallPath(), msg.getSrcHostUuid(), msg.getPrimaryStorageUuid()));
                            reply.setError(Platform.operr("operation error, because:%s", returnValue.getError()));
                        }
                        bus.reply(msg, reply);
                        completion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.error(String.format("failed to download bits %s from kvm host %s to primary storage %s", cmd.getBackupStorageInstallPath(), msg.getSrcHostUuid(), msg.getPrimaryStorageUuid()));
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                }).specifyOrder(randomFactor).call();
            }
        });
    }

    private void handle(CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg) {
        inQueue().name(String.format("cancel-download-bits-from-kvm-host-to-primary-storage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> cancelDownloadBitsFromKVMHostToPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void cancelDownloadBitsFromKVMHostToPrimaryStorage(CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        CancelDownloadBitsFromKVMHostToPrimaryStorageReply reply = new CancelDownloadBitsFromKVMHostToPrimaryStorageReply();
        CancelDownloadBitsFromKVMHostCmd cmd = new CancelDownloadBitsFromKVMHostCmd();
        cmd.setPrimaryStorageInstallPath(msg.getPrimaryStorageInstallPath());
        String randomFactor = msg.getLongJobUuid();
        new HttpCaller<>(CANCEL_DOWNLOAD_BITS_FROM_KVM_HOST_PATH, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(reply) {
            @Override
            public void success(AgentResponse returnValue) {
                if (returnValue.isSuccess()) {
                    logger.info(String.format("successfully cancel downloaded bits to primary storage %s", msg.getPrimaryStorageUuid()));
                } else {
                    logger.error(String.format("failed to cancel download bits to primary storage %s",msg.getPrimaryStorageUuid()));
                    reply.setError(Platform.operr("operation error, because:%s", returnValue.getError()));
                }
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to cancel download bits to primary storage %s", msg.getPrimaryStorageUuid()));
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        }).specifyOrder(randomFactor).tryNext().call();
    }

    private void handle(GetDownloadBitsFromKVMHostProgressMsg msg) {
        GetDownloadBitsFromKVMHostProgressReply reply = new GetDownloadBitsFromKVMHostProgressReply();
        GetDownloadBitsFromKVMHostProgressCmd cmd = new GetDownloadBitsFromKVMHostProgressCmd();
        cmd.volumePaths = msg.getVolumePaths();
        final String apiId = ThreadContext.get(Constants.THREAD_CONTEXT_API);
        new HttpCaller<>(GET_DOWNLOAD_BITS_FROM_KVM_HOST_PROGRESS_PATH, cmd, GetDownloadBitsFromKVMHostProgressRsp.class, new ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressRsp>(reply) {
            @Override
            public void success(GetDownloadBitsFromKVMHostProgressRsp rsp) {
                if (rsp.isSuccess()) {
                    logger.info(String.format("successfully get download progress from primary storage %s", msg.getPrimaryStorageUuid()));
                    reply.setTotalSize(rsp.totalSize);
                } else {
                    logger.error(String.format("failed to get download progress from primary storage %s",msg.getPrimaryStorageUuid()));
                    reply.setError(Platform.operr("operation error, because:%s", rsp.getError()));
                }
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to get download progress from primary storage %s", msg.getPrimaryStorageUuid()));
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        }).specifyOrder(apiId).tryNext().call();
    }

    private void handle(DownloadBitsFromNbdToPrimaryStorageMsg msg) {
        DownloadBitsFromNbdToPrimaryStorageReply reply = new DownloadBitsFromNbdToPrimaryStorageReply();
        DownloadBitsFromNbdCmd cmd = new DownloadBitsFromNbdCmd();
        cmd.setSendCommandUrl(restf.getSendCommandUrl());
        cmd.setBandwidth(msg.getBandWidth());
        cmd.setPrimaryStorageInstallPath(msg.getPrimaryStorageInstallPath());
        cmd.setNbdExportUrl(msg.getNbdExportUrl());

        httpCall(DOWNLOAD_BITS_FROM_NBD_EXPT_PATH, cmd, DownloadBitsFromNbdRsp.class, new ReturnValueCompletion<DownloadBitsFromNbdRsp>(msg) {
            @Override
            public void success(DownloadBitsFromNbdRsp rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                } else {
                    reply.setDiskSize(rsp.diskSize);
                }
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(DownloadBitsFromRemoteTargetOnPrimaryStorageMsg msg) {
        DownloadBitsFromNbdToPrimaryStorageReply reply = new DownloadBitsFromNbdToPrimaryStorageReply();
        DownloadBitsFromRemoteTargetCmd cmd = new DownloadBitsFromRemoteTargetCmd();
        cmd.setSendCommandUrl(restf.getSendCommandUrl());
        cmd.setPrimaryStorageInstallPath(msg.getPrimaryStorageInstallPath());
        cmd.setRemoteTargetUri(msg.getRemoteTargetUri());

        httpCall(DOWNLOAD_BITS_FROM_REMOTE_TARGET_PATH, cmd, DownloadBitsFromRemoteTargetRsp.class, new ReturnValueCompletion<DownloadBitsFromRemoteTargetRsp>(msg) {
            @Override
            public void success(DownloadBitsFromRemoteTargetRsp rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("%s", rsp.getError()));
                } else {
                    reply.setDiskSize(rsp.diskSize);
                }
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(DeleteImageCacheOnPrimaryStorageMsg msg) {
        inQueue().name(String.format("delete-image-cache-on-primarystorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> deleteImageCacheOnPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void deleteImageCacheOnPrimaryStorage(DeleteImageCacheOnPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        DeleteImageCacheOnPrimaryStorageReply reply = new DeleteImageCacheOnPrimaryStorageReply();

        DeleteImageCacheCmd cmd = new DeleteImageCacheCmd();
        cmd.setFsId(getSelf().getFsid());
        cmd.setUuid(self.getUuid());
        cmd.imagePath = msg.getInstallPath().split("@")[0];
        cmd.snapshotPath = msg.getInstallPath();
        httpCall(DELETE_IMAGE_CACHE, cmd, AgentResponse.class, new ReturnValueCompletion<AgentResponse>(msg) {
            @Override
            public void success(AgentResponse rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(operr("operation error, because:%s", rsp.getError()));
                }

                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    private void handle(CancelSelfFencerOnKvmHostMsg msg) {
        inQueue().name(String.format("cancel-self-fencer-on-kvmHost-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> cancelSelfFencerOnKvmHost(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void cancelSelfFencerOnKvmHost(CancelSelfFencerOnKvmHostMsg msg, final NoErrorCompletion completion) {
        KvmCancelSelfFencerParam param = msg.getParam();
        KvmCancelSelfFencerCmd cmd = new KvmCancelSelfFencerCmd();
        cmd.uuid = self.getUuid();
        cmd.fsId = getSelf().getFsid();
        cmd.hostUuid = param.getHostUuid();

        CancelSelfFencerOnKvmHostReply reply = new CancelSelfFencerOnKvmHostReply();
        new KvmCommandSender(param.getHostUuid()).send(cmd, KVM_HA_CANCEL_SELF_FENCER, wrapper -> {
            AgentResponse rsp = wrapper.getResponse(AgentResponse.class);
            return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(msg) {
            @Override
            public void success(KvmResponseWrapper w) {
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    private void handle(SetupSelfFencerOnKvmHostMsg msg) {
        inQueue().name(String.format("setup-self-fencer-on-kvm-host-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> setupSelfFencerOnKvmHost(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void setupSelfFencerOnKvmHost(final SetupSelfFencerOnKvmHostMsg msg, final NoErrorCompletion completion) {
        KvmSetupSelfFencerParam param = msg.getParam();
        KvmSetupSelfFencerCmd cmd = new KvmSetupSelfFencerCmd();
        cmd.uuid = self.getUuid();
        cmd.fsId = getSelf().getFsid();
        cmd.hostUuid = param.getHostUuid();
        cmd.interval = param.getInterval();
        cmd.maxAttempts = param.getMaxAttempts();
        cmd.storageCheckerTimeout = param.getStorageCheckerTimeout();
        cmd.userKey = getSelf().getUserKey();
        cmd.poolNames = Q.New(CephPrimaryStoragePoolVO.class)
                .select(CephPrimaryStoragePoolVO_.poolName)
                .eq(CephPrimaryStoragePoolVO_.type, CephPrimaryStoragePoolType.Root.toString())
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, self.getUuid())
                .listValues();
        cmd.monUrls = transform(getSelf().getMons(), arg -> String.format("%s:%s", arg.getMonAddr(), arg.getMonPort()));
        cmd.strategy = param.getStrategy();
        cmd.fencers = param.getFencers();

        if (CephSystemTags.CEPH_MANUFACTURER.hasTag(self.getUuid())) {
            cmd.manufacturer = CephSystemTags.CEPH_MANUFACTURER.getTokenByResourceUuid(self.getUuid(), CephSystemTags.CEPH_MANUFACTURER_TOKEN);
        }

        final SetupSelfFencerOnKvmHostReply reply = new SetupSelfFencerOnKvmHostReply();
        new KvmCommandSender(param.getHostUuid()).send(cmd, KVM_HA_SETUP_SELF_FENCER, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                AgentResponse rsp = wrapper.getResponse(AgentResponse.class);
                return rsp.isSuccess() ? null : operr("%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(msg) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    private void handle(UploadBitsToBackupStorageMsg msg) {
        inQueue().name(String.format("upload-bits-to-backupstorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> uploadBitsToBackupStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void uploadBitsToBackupStorage(final UploadBitsToBackupStorageMsg msg, final NoErrorCompletion completion) {
        UploadBitsToBackupStorageReply reply = new UploadBitsToBackupStorageReply();
        BackupStorageMediator mediator = getBackupStorageMediator(msg.getBackupStorageUuid());

        MediatorUploadParam param = new MediatorUploadParam();
        param.image = dbf.findByUuid(msg.getImageUuid(), ImageVO.class).toInventory();
        param.primaryStorageUuid = msg.getPrimaryStorageUuid();
        param.primaryStorageInstallPath = msg.getPrimaryStorageInstallPath();
        param.backupStorageInstallPath = msg.getBackupStorageInstallPath();
        mediator.param = param;
        mediator.upload(new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String installPath) {
                reply.setInstallPath(installPath);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    private void handle(final CheckHostStorageConnectionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("check-storage-%s-host-connection", msg.getPrimaryStorageUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                CheckHostStorageConnectionReply reply = new CheckHostStorageConnectionReply();
                checkHostStorageConnection(msg.getHostUuids(), new Completion(chain) {
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
                return getSyncSignature();
            }
        });
    }

    private static class CheckHostStorageConnectionCmdBuilder {
        private String uuid;
        private List<String> poolNames;
        private String hostUuid;
        private String userKey;
        private List<String> monUrls;
        private String fsId;

        public CheckHostStorageConnectionCmdBuilder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public CheckHostStorageConnectionCmdBuilder poolNames(List<String> poolNames) {
            this.poolNames = poolNames;
            return this;
        }

        public CheckHostStorageConnectionCmdBuilder hostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
            return this;
        }

        public CheckHostStorageConnectionCmdBuilder userKey(String userKey) {
            this.userKey = userKey;
            return this;
        }

        public CheckHostStorageConnectionCmdBuilder monUrls(List<String> monUrls) {
            this.monUrls = monUrls;
            return this;
        }

        public CheckHostStorageConnectionCmdBuilder fsId(String fsId) {
            this.fsId = fsId;
            return this;
        }

        public CheckHostStorageConnectionCmd build() {
            final CheckHostStorageConnectionCmd cmd = new CheckHostStorageConnectionCmd();
            cmd.uuid = this.uuid;
            cmd.fsId = this.fsId;
            cmd.userKey = this.userKey;
            cmd.poolNames = this.poolNames;
            cmd.monUrls = this.monUrls;
            cmd.hostUuid = this.hostUuid;
            return cmd;
        }
    }

    private void checkHostStorageConnection(List<String> hostUuids, final Completion completion) {
        CheckHostStorageConnectionCmdBuilder builder = new CheckHostStorageConnectionCmdBuilder();
        builder.uuid(self.getUuid())
                .fsId(getSelf().getFsid())
                .userKey(getSelf().getUserKey())
                .poolNames(Q.New(CephPrimaryStoragePoolVO.class)
                        .select(CephPrimaryStoragePoolVO_.poolName)
                        .eq(CephPrimaryStoragePoolVO_.type, CephPrimaryStoragePoolType.Root.toString())
                        .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, self.getUuid())
                        .listValues())
                .monUrls(transform(getSelf().getMons(), arg -> String.format("%s:%s", arg.getMonAddr(), arg.getMonPort())));

        List<KVMHostAsyncHttpCallMsg> msgs = transform(hostUuids, huuid -> {
            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            builder.hostUuid(huuid);
            msg.setCommand(builder.build());
            msg.setPath(CHECK_HOST_STORAGE_CONNECTION_PATH);
            msg.setHostUuid(huuid);
            msg.setNoStatusCheck(true);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid);
            return msg;
        });

        new While<>(msgs).each((msg, wc) -> bus.send(msg, new CloudBusCallBack(wc) {
            @Override
            public void run(MessageReply reply) {
                KVMHostAsyncHttpCallReply kr = reply.castReply();
                CheckHostStorageConnectionRsp rsp = kr.toResponse(CheckHostStorageConnectionRsp.class);

                UpdatePrimaryStorageHostStatusMsg umsg = new UpdatePrimaryStorageHostStatusMsg();
                umsg.setHostUuid(msg.getHostUuid());
                umsg.setPrimaryStorageUuid(self.getUuid());

                if (rsp == null) {
                    wc.addError(operr("operation error, because: failed to get response"));
                    umsg.setStatus(PrimaryStorageHostStatus.Disconnected);
                } else {
                    ErrorCode errorCode = rsp.buildErrorCode();
                    if (errorCode != null) {
                        wc.addError(operr("operation error, because:%s", errorCode));
                        umsg.setStatus(PrimaryStorageHostStatus.Disconnected);
                    } else {
                        umsg.setStatus(PrimaryStorageHostStatus.Connected);
                    }
                }

                bus.makeTargetServiceIdByResourceUuid(umsg, PrimaryStorageConstant.SERVICE_ID, umsg.getPrimaryStorageUuid());
                bus.send(umsg);
                wc.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    logger.warn(String.format("check host storage connection failed, because: %s", errorCodeList.getCauses()));
                }

                completion.success();
            }
        });

    }

    private void handle(final CreateKvmSecretMsg msg) {
        final CreateKvmSecretReply reply = new CreateKvmSecretReply();
        createSecretOnKvmHosts(msg.getHostUuids(), new Completion(msg) {
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

    private void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
        reply.setError(operr("backing up snapshots to backup storage is a depreciated feature, which will be removed in future version"));
        bus.reply(msg, reply);
    }

    private void handle(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        if (msg.hasSystemTag(VolumeSystemTags.FAST_CREATE::isMatch)) {
            inQueue().name(String.format("create-volume-from-volume-snapshot-on-primarystorage-%s", self.getUuid()))
                    .asyncBackup(msg)
                    .run(chain -> fastCreateVolumeFromSnapshot(msg, new NoErrorCompletion(chain) {
                        @Override
                        public void done() {
                            chain.next();
                        }
                    }));
        } else {
            inQueue().name(String.format("create-volume-from-volume-snapshot-on-primarystorage-%s", self.getUuid()))
                    .asyncBackup(msg)
                    .run(chain -> createVolumeFromSnapshot(msg, new NoErrorCompletion(chain) {
                        @Override
                        public void done() {
                            chain.next();
                        }
                    }));
        }
    }

    private void fastCreateVolumeFromSnapshot(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        final CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
        // create volume first, then reserve size for it, so we use snapshot poolName for volume create
        String snapShotPath = msg.getSnapshot().getPrimaryStorageInstallPath();
        final String volPath = makeVolumeInstallPathByTargetPool(msg.getVolumeUuid(), getTargetPoolNameFromAllocatedUrl(snapShotPath));
        VolumeSnapshotInventory sp = msg.getSnapshot();
        cloneAndProtectSnapshot(sp.getPrimaryStorageInstallPath(), volPath, new ReturnValueCompletion<CloneRsp>(completion) {
            @Override
            public void success(CloneRsp rsp) {
                reply.setInstallPath(volPath);
                // current ceph has no way to get the actual size
                long asize = rsp.actualSize == null ? 1 : rsp.actualSize;
                reply.setActualSize(asize);
                reply.setIncremental(true);

                long srcSnapshotVolumeSize = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, sp.getVolumeUuid()).select(VolumeVO_.size).findValue();
                VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();

                if (volume.getSize() <= srcSnapshotVolumeSize) {
                    reply.setSize(rsp.size);
                    bus.reply(msg, reply);
                    completion.done();
                    return;
                }

                ResizeVolumeOnPrimaryStorageMsg rmsg = new ResizeVolumeOnPrimaryStorageMsg();
                rmsg.setVolume(VolumeInventory.valueOf(volume));
                rmsg.setSize(volume.getSize());
                rmsg.setPrimaryStorageUuid(self.getUuid());
                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                bus.send(rmsg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply re) {
                        if (re.getError() != null) {
                            reply.setError(re.getError());
                        } else {
                            ResizeVolumeOnPrimaryStorageReply r = re.castReply();
                            reply.setSize(r.getVolume().getSize());
                        }
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    private void cloneAndProtectSnapshot(String snapshotPath, String dstPath, ReturnValueCompletion<CloneRsp> completion) {
        ProtectSnapshotCmd cmd = new ProtectSnapshotCmd();
        cmd.snapshotPath = snapshotPath;
        cmd.ignoreError = true;
        httpCall(PROTECT_SNAPSHOT_PATH, cmd, ProtectSnapshotRsp.class, new ReturnValueCompletion<ProtectSnapshotRsp>(completion) {
            @Override
            public void success(ProtectSnapshotRsp returnValue) {
                CloneCmd cmd = new CloneCmd();
                cmd.srcPath = snapshotPath;
                cmd.dstPath = dstPath;
                httpCall(CLONE_PATH, cmd, CloneRsp.class, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void createVolumeFromSnapshot(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        final CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
        String snapShotPath = msg.getSnapshot().getPrimaryStorageInstallPath();
        final String volPath =  makeVolumeInstallPathByTargetPool(msg.getVolumeUuid(), getTargetPoolNameFromAllocatedUrl(snapShotPath));
        VolumeSnapshotInventory sp = msg.getSnapshot();
        CpCmd cmd = new CpCmd();
        cmd.resourceUuid = msg.getSnapshot().getVolumeUuid();
        cmd.srcPath = sp.getPrimaryStorageInstallPath();
        cmd.dstPath = volPath;
        httpCall(CP_PATH, cmd, CpRsp.class, new ReturnValueCompletion<CpRsp>(msg) {
            @Override
            public void success(CpRsp rsp) {
                reply.setInstallPath(volPath);
                reply.setSize(rsp.size);
                // current ceph has no way to get the actual size
                long asize = rsp.actualSize == null ? 1 : rsp.actualSize;
                reply.setActualSize(asize);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    protected void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        final RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("revert-volume-[uuid:%s]-from-snapshot-[uuid:%s]-on-ceph-primary-storage",
                msg.getVolume().getUuid(), msg.getSnapshot().getUuid()));
        chain.enableProgressReport();
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                // get volume path from snapshot path, just split @
                String volumePath = msg.getSnapshot().getPrimaryStorageInstallPath().split("@")[0];

                flow(new NoRollbackFlow() {
                    @Override
                    public String name() {
                        return "undo-snapshot-creation";
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RollbackSnapshotCmd cmd = new RollbackSnapshotCmd();
                        cmd.snapshotPath = msg.getSnapshot().getPrimaryStorageInstallPath();
                        httpCall(ROLLBACK_SNAPSHOT_PATH, cmd, RollbackSnapshotRsp.class, new ReturnValueCompletion<RollbackSnapshotRsp>(msg) {
                            @Override
                            public void success(RollbackSnapshotRsp returnValue) {
                                reply.setSize(returnValue.getSize());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        }, TimeUnit.MILLISECONDS, msg.getTimeout());
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        Long trashId = trash.getTrashId(self.getUuid(), volumePath);
                        if (trashId != null) {
                            trash.removeFromDb(trashId);
                        }

                        reply.setNewVolumeInstallPath(volumePath);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();


    }

    private ImageSpec makeImageSpec(VolumeInventory volume) {
        ImageVO image = dbf.findByUuid(volume.getRootImageUuid(), ImageVO.class);
        if (image == null) {
            throw new OperationFailureException(operr("cannot reinit rootvolume [%s] because image [%s] has been deleted and imagecache cannot be found",
                    volume.getUuid(), volume.getRootImageUuid()));
        }

        ImageSpec imageSpec = new ImageSpec();
        imageSpec.setInventory(ImageInventory.valueOf(image));

        // mismatch fsid means the image is not available for this primary storage.
        // normally this happens due to primary storage migration which do
        // not migrate images stored in ceph backup storage.
        List<String> sameCephBsUuids = Q.New(CephBackupStorageVO.class).select(CephBackupStorageVO_.uuid)
                .eq(CephBackupStorageVO_.fsid, getSelf().getFsid())
                .listValues();
        List<String> supportBsUuids = Q.New(BackupStorageVO.class).select(BackupStorageVO_.uuid)
                .in(BackupStorageVO_.type, backupStorageMediators.keySet())
                .listValues();

        ImageBackupStorageRefInventory ref = imageSpec.getInventory().getBackupStorageRefs().stream()
                .filter(it -> ImageStatus.Ready.toString().equals(it.getStatus()))
                .filter(it -> supportBsUuids.contains(it.getBackupStorageUuid()))
                .filter(it -> !it.getInstallPath().startsWith("ceph://") || sameCephBsUuids.contains(it.getBackupStorageUuid()))
                .min(Comparator.comparing(it -> {
                    if (it.getInstallPath().startsWith("ceph://")) {
                        return -1;
                    } else {
                        return 1;
                    }
                })).orElseThrow(() -> new OperationFailureException(operr("cannot find backupstorage to download image [%s] " +
                        "to primarystorage [%s] due to lack of Ready and accessible image", volume.getRootImageUuid(), getSelf().getUuid())));

        imageSpec.setSelectedBackupStorage(ref);
        return imageSpec;
    }

    protected void handle(final ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("reimage-vm-root-volume-%s", msg.getVolume().getUuid()));
        chain.then(new ShareFlow() {
            final String originalVolumePath = msg.getVolume().getInstallPath();
            final String targetPoolName = getTargetPoolNameFromAllocatedUrl(msg.getAllocatedInstallUrl());
            String volumePath = makeResetImageRootVolumeInstallPath(msg.getVolume().getUuid(), targetPoolName);
            String installUrl;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-image-to-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        installUrl = Q.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, msg.getVolume().getRootImageUuid()).
                                eq(ImageCacheVO_.primaryStorageUuid, msg.getPrimaryStorageUuid()).select(ImageCacheVO_.installUrl).findValue();

                        if (installUrl != null) {
                            trigger.next();
                            return;
                        }

                        DownloadVolumeTemplateToPrimaryStorageMsg dmsg = new DownloadVolumeTemplateToPrimaryStorageMsg();
                        dmsg.setTemplateSpec(makeImageSpec(msg.getVolume()));
                        dmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, dmsg.getPrimaryStorageUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                installUrl = ((DownloadVolumeTemplateToPrimaryStorageReply) reply).getImageCache().getInstallUrl();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "clone-image";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CloneCmd cmd = new CloneCmd();
                        cmd.srcPath = installUrl;
                        cmd.dstPath = volumePath;
                        httpCall(CLONE_PATH, cmd, CloneRsp.class, new ReturnValueCompletion<CloneRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(CloneRsp ret) {
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setNewVolumeInstallPath(volumePath);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(DeleteSnapshotOnPrimaryStorageMsg msg) {
        inQueue().name(String.format("delete-snapshot-on-primarystorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> deleteSnapshotOnPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void deleteSnapshotOnPrimaryStorage(final DeleteSnapshotOnPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        final DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        DeleteSnapshotCmd cmd = new DeleteSnapshotCmd();
        cmd.snapshotPath = msg.getSnapshot().getPrimaryStorageInstallPath();
        httpCall(DELETE_SNAPSHOT_PATH, cmd, DeleteSnapshotRsp.class, new ReturnValueCompletion<DeleteSnapshotRsp>(msg) {
            @Override
            public void success(DeleteSnapshotRsp returnValue) {
                osdHelper.releaseAvailableCapacity(msg.getSnapshot().getPrimaryStorageInstallPath(), msg.getSnapshot().getSize());
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CephDeleteVolumeSnapshotGC snapshotGC = new CephDeleteVolumeSnapshotGC();
                snapshotGC.NAME = String.format("gc-ceph-%s-volumesnapshot-path-%s", self.getUuid(), cmd.snapshotPath);
                snapshotGC.primaryStorageUuid = self.getUuid();
                snapshotGC.volumeSnapshot = msg.getSnapshot();
                snapshotGC.deduplicateSubmit(CephGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        }, TimeUnit.MILLISECONDS, msg.getTimeout());
    }

    private void handle(PurgeSnapshotOnPrimaryStorageMsg msg) {
        inQueue().name(String.format("purge-snapshot-on-primarystorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> purgeSnapshotOnPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    protected void purgeSnapshotOnPrimaryStorage(final PurgeSnapshotOnPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        final PurgeSnapshotOnPrimaryStorageReply reply = new PurgeSnapshotOnPrimaryStorageReply();
        PurgeSnapshotCmd cmd = new PurgeSnapshotCmd();
        cmd.volumePath = msg.getVolumePath();
        httpCall(PURGE_SNAPSHOT_PATH, cmd, PurgeSnapshotRsp.class, new ReturnValueCompletion<PurgeSnapshotRsp>(msg) {
            @Override
            public void success(PurgeSnapshotRsp returnValue) {
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    @Override
    protected void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(FlattenVolumeOnPrimaryStorageMsg msg) {
        FlattenVolumeOnPrimaryStorageReply reply = new FlattenVolumeOnPrimaryStorageReply();
        FlattenCmd cmd = new FlattenCmd();
        cmd.path = msg.getVolume().getInstallPath();

        // TODO unprotect snapshot
        httpCall(FLATTEN_PATH, cmd, FlattenRsp.class, new ReturnValueCompletion<FlattenRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(FlattenRsp rsp) {
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CheckSnapshotMsg msg) {
        CheckSnapshotReply reply = new CheckSnapshotReply();
        bus.reply(msg, reply);
    }

    protected void handle(TakeSnapshotMsg msg) {
        inQueue().name(String.format("take-snapshot-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> takeSnapshot(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void takeSnapshot(final TakeSnapshotMsg msg, final NoErrorCompletion completion) {
        final TakeSnapshotReply reply = new TakeSnapshotReply();

        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.installPath);
        q.add(VolumeVO_.uuid, Op.EQ, sp.getVolumeUuid());
        String volumePath = q.findValue();

        final String spPath = String.format("%s@%s", volumePath, sp.getUuid());
        CreateSnapshotCmd cmd = new CreateSnapshotCmd();
        cmd.volumeUuid = sp.getVolumeUuid();
        cmd.snapshotPath = spPath;
        cmd.name = msg.getName();
        cmd.description = msg.getName();
        httpCall(CREATE_SNAPSHOT_PATH, cmd, CreateSnapshotRsp.class, new ReturnValueCompletion<CreateSnapshotRsp>(msg) {
            @Override
            public void success(CreateSnapshotRsp rsp) {
                // current ceph has no way to get actual size
                long asize = rsp.getActualSize() == null ? 0 : rsp.getActualSize();
                sp.setSize(asize);
                sp.setPrimaryStorageUuid(self.getUuid());
                sp.setPrimaryStorageInstallPath(rsp.getInstallPath());
                sp.setType(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString());
                sp.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                reply.setInventory(sp);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    @Override
    public void attachHook(String clusterUuid, Completion completion) {
        SimpleQuery<ClusterVO> q = dbf.createQuery(ClusterVO.class);
        q.select(ClusterVO_.hypervisorType);
        q.add(ClusterVO_.uuid, Op.EQ, clusterUuid);
        String hvType = q.findValue();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        if (KVMConstant.KVM_HYPERVISOR_TYPE.equals(hvType)) {
            chain.then(attachToKvmCluster(clusterUuid));
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("create-primary-storage-%s-host-in-cluster-%s-refs", self.getUuid(), clusterUuid);

            @Override
            public void run(FlowTrigger trigger, Map data) {
                long total = Q.New(HostVO.class)
                        .eq(HostVO_.clusterUuid, clusterUuid)
                        .eq(HostVO_.status, HostStatus.Connected)
                        .notIn(HostVO_.state, list(HostState.PreMaintenance, HostState.Maintenance))
                        .count();

                if (total == 0) {
                    logger.debug(String.format("no hosts in cluster[uuid: %s] need to create PrimaryStorageHostRef", clusterUuid));
                    trigger.next();
                    return;
                }

                SQL.New("select host.uuid from HostVO host where host.clusterUuid = :clusterUuid and" +
                        " host.status = :status and host.state not in (:hostStates)")
                        .param("clusterUuid", clusterUuid)
                        .param("status", HostStatus.Connected)
                        .param("hostStates", list(HostState.PreMaintenance, HostState.Maintenance))
                        .limit(500)
                        .paginate(total, (List<String> hostUuids, PaginateCompletion paginateCompletion) -> {
                            for (String hostUuid : hostUuids) {
                                updatePrimaryStorageHostStatus(self.getUuid(), hostUuid, PrimaryStorageHostStatus.Connected, null);
                            }

                            paginateCompletion.done();
                        }, new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                logger.debug(String.format("succeed add PrimaryStorageHostRef record to primary storage[uuid: %s]" +
                                        " and hosts in cluster[uuid: %s]", self.getUuid(), clusterUuid));
                                trigger.next();
                            }
                        });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void createSecretOnKvmHosts(List<String> hostUuids, final Completion completion) {
        final CreateKvmSecretCmd cmd = new CreateKvmSecretCmd();
        cmd.setUserKey(getSelf().getUserKey());
        String suuid = CephSystemTags.KVM_SECRET_UUID.getTokenByResourceUuid(self.getUuid(), CephSystemTags.KVM_SECRET_UUID_TOKEN);
        DebugUtils.Assert(suuid != null, String.format("cannot find system tag[%s] for ceph primary storage[uuid:%s]", CephSystemTags.KVM_SECRET_UUID.getTagFormat(), self.getUuid()));
        cmd.setUuid(suuid);

        List<KVMHostAsyncHttpCallMsg> msgs = transform(hostUuids, huuid -> {
            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setCommand(cmd);
            msg.setPath(KVM_CREATE_SECRET_PATH);
            msg.setHostUuid(huuid);
            msg.setNoStatusCheck(true);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid);
            return msg;
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        completion.fail(r.getError());
                        return;
                    }

                    KVMHostAsyncHttpCallReply kr = r.castReply();
                    CreateKvmSecretRsp rsp = kr.toResponse(CreateKvmSecretRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("operation error, because:%s", rsp.getError()));
                        return;
                    }
                }

                completion.success();
            }
        });
    }

    private Flow attachToKvmCluster(String clusterUuid) {
        return new NoRollbackFlow() {
            String __name__ = String.format("create-secret-on-kvm-hosts-in-cluster-%s", clusterUuid);

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
                q.select(HostVO_.uuid);
                q.add(HostVO_.clusterUuid, Op.EQ, clusterUuid);
                q.add(HostVO_.status, Op.EQ, HostStatus.Connected);
                List<String> hostUuids = q.listValue();
                if (hostUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                createSecretOnKvmHosts(hostUuids, new Completion(trigger) {
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
        };
    }

    @Override
    public void deleteHook() {
        List<String> poolNameLists = list(
                getDefaultRootVolumePoolName(),
                getDefaultDataVolumePoolName(),
                getDefaultImageCachePoolName()
        );

        SQL.New(CephPrimaryStoragePoolVO.class)
                .in(CephPrimaryStoragePoolVO_.poolName, poolNameLists).delete();

        if (CephGlobalConfig.PRIMARY_STORAGE_DELETE_POOL.value(Boolean.class)) {


            DeletePoolCmd cmd = new DeletePoolCmd();
            cmd.poolNames = poolNameLists;
            FutureReturnValueCompletion completion = new FutureReturnValueCompletion(null);
            httpCall(DELETE_POOL_PATH, cmd, DeletePoolRsp.class, completion);
            completion.await(TimeUnit.MINUTES.toMillis(30));
            if (!completion.isSuccess()) {
                throw new OperationFailureException(completion.getErrorCode());
            }
        }
        String fsid = getSelf().getFsid();
        new SQLBatch(){

            @Override
            protected void scripts() {
                if(Q.New(CephBackupStorageVO.class).eq(CephBackupStorageVO_.fsid, fsid).find() == null){
                    SQL.New(CephCapacityVO.class).eq(CephCapacityVO_.fsid, fsid).delete();
                }
            }
        }.execute();
        dbf.removeCollection(getSelf().getMons(), CephPrimaryStorageMonVO.class);
    }

    private void handle(CreateEmptyVolumeMsg msg) {
        inQueue().name(String.format("create-empty-volume-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> createEmptyVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void createEmptyVolume(CreateEmptyVolumeMsg msg, final NoErrorCompletion completion) {
        final CreateEmptyVolumeReply reply = new CreateEmptyVolumeReply();
        final CreateEmptyVolumeCmd cmd = new CreateEmptyVolumeCmd();
        cmd.installPath = msg.getInstallPath();
        cmd.size = msg.getSize();
        cmd.shareable = msg.isShareable();
        cmd.skipIfExisting = msg.isSkipIfExisting();

        httpCall(CREATE_VOLUME_PATH, cmd, CreateEmptyVolumeRsp.class, new ReturnValueCompletion<CreateEmptyVolumeRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void success(CreateEmptyVolumeRsp ret) {
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    private void handle(CephToCephMigrateVolumeSegmentMsg msg) {
        inQueue().name(String.format("ceph-to-ceph-migrate-volume-segment-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> cephToCephMigrateVolumeSegment(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    private void cephToCephMigrateVolumeSegment(CephToCephMigrateVolumeSegmentMsg msg, final NoErrorCompletion completion) {
        final CephToCephMigrateVolumeSegmentCmd cmd = new CephToCephMigrateVolumeSegmentCmd();
        cmd.setParentUuid(msg.getParentUuid());
        cmd.setResourceUuid(msg.getResourceUuid());
        cmd.setSrcInstallPath(msg.getSrcInstallPath());
        cmd.setDstInstallPath(msg.getDstInstallPath());

        MonMigrateIpInfo dstMonMigrateIpInfo = new MonMigrateIpInfo(msg.getDstPrimaryStorageUuid());
        CephPrimaryStorageMonVO dstMon = dstMonMigrateIpInfo.monMap.values().iterator().next();
        String dstMonMigrateIp = dstMon.getHostname();
        if (!dstMonMigrateIpInfo.getMonsMigrateIpInCidr().isEmpty()) {
            dstMon = dstMonMigrateIpInfo.getMonsMigrateIpInCidr().get(0);
            dstMonMigrateIp = dstMonMigrateIpInfo.getMigrateIp(dstMon.getUuid());
        }
        cmd.setDstMonHostname(dstMonMigrateIp);
        cmd.setDstMonSshUsername(dstMon.getSshUsername());
        cmd.setDstMonSshPassword(dstMon.getSshPassword());
        cmd.setDstMonSshPort(dstMon.getSshPort());

        final MonMigrateIpInfo srcMonMigrateIpInfo = new MonMigrateIpInfo(msg.getPrimaryStorageUuid());
        List<String> srcMonsWithMigrateIp = new ArrayList<>();
        if (!srcMonMigrateIpInfo.monMigrateIpMap.isEmpty()) {
            srcMonsWithMigrateIp.addAll(srcMonMigrateIpInfo.monMigrateIpMap.keySet());
        }
        Comparator<CephPrimaryStorageMonVO> containFirst = Comparator.comparing(mon -> !srcMonsWithMigrateIp.contains(mon.getUuid()));

        final String apiId = ThreadContext.get(Constants.THREAD_CONTEXT_API);
        final CephToCephMigrateVolumeSegmentReply reply = new CephToCephMigrateVolumeSegmentReply();
        new HttpCaller<>(CEPH_TO_CEPH_MIGRATE_VOLUME_SEGMENT_PATH, cmd, StorageMigrationRsp.class, new ReturnValueCompletion<StorageMigrationRsp>(msg) {
            @Override
            public void success(StorageMigrationRsp returnValue) {
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        }, TimeUnit.MILLISECONDS, msg.getTimeout()).specifyOrder(apiId).specifyOrder(containFirst).call();
    }

    static class MonMigrateIpInfo {
        private final Map<String, CephPrimaryStorageMonVO> monMap;

        private final Map<String, String> monMigrateIpMap = new HashMap<>();

        public MonMigrateIpInfo(String psUuid) {
            CephPrimaryStorageVO psVO = Q.New(CephPrimaryStorageVO.class)
                    .eq(CephPrimaryStorageVO_.uuid, psUuid).find();
            List<CephPrimaryStorageMonVO> monVOS = new ArrayList<>(psVO.getMons());
            Collections.shuffle(monVOS);
            this.monMap = monVOS.stream().collect(
                    Collectors.toMap(CephPrimaryStorageMonVO::getUuid, CephPrimaryStorageMonVO -> CephPrimaryStorageMonVO));

            String migrateCidr = PrimaryStorageSystemTags.PRIMARY_STORAGE_MIGRATE_NETWORK_CIDR
                    .getTokenByResourceUuid(psUuid, PrimaryStorageSystemTags.PRIMARY_STORAGE_MIGRATE_NETWORK_CIDR_TOKEN);
            if (migrateCidr == null) {
                return;
            }

            for (CephPrimaryStorageMonVO mon : monVOS) {
                List<String> ips = new ArrayList<>();
                ips.add(mon.getHostname());
                final String extraIps = CephMonSystemTags.EXTRA_IPS
                        .getTokenByResourceUuid(mon.getUuid(), CephMonSystemTags.EXTRA_IPS_TOKEN);
                Optional.ofNullable(extraIps).ifPresent(it -> ips.addAll(Arrays.asList(it.split(","))));
                List<String> cidrIps = NetworkUtils.filterIpv4sInCidr(ips, migrateCidr);
                if (!cidrIps.isEmpty()) {
                    monMigrateIpMap.put(mon.getUuid(), cidrIps.get(0));
                }
            }
        }

        public List<CephPrimaryStorageMonVO> getMonsMigrateIpInCidr() {
            if (monMigrateIpMap.isEmpty()) {
                return Collections.emptyList();
            }

            return monMigrateIpMap.keySet().stream().map(monMap::get).collect(Collectors.toList());
        }

        public String getMigrateIp(String monUuid) {
            return monMigrateIpMap.get(monUuid);
        }
    }

    private void handle(GetVolumeSnapshotInfoMsg msg) {
        final GetVolumeSnapInfosCmd cmd = new GetVolumeSnapInfosCmd();
        cmd.setVolumePath(msg.getVolumePath());

        final GetVolumeSnapshotInfoReply reply = new GetVolumeSnapshotInfoReply();
        httpCall(GET_VOLUME_SNAPINFOS_PATH, cmd, GetVolumeSnapInfosRsp.class, new ReturnValueCompletion<GetVolumeSnapInfosRsp>(msg) {
            @Override
            public void success(GetVolumeSnapInfosRsp returnValue) {
                reply.setSnapInfos(returnValue.getSnapInfos());
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
    public void handle(AskInstallPathForNewSnapshotMsg msg) {
        AskInstallPathForNewSnapshotReply reply = new AskInstallPathForNewSnapshotReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(GetPrimaryStorageResourceLocationMsg msg) {
        bus.reply(msg, new GetPrimaryStorageResourceLocationReply());
    }

    @Override
    protected void handle(GetVolumeSnapshotSizeOnPrimaryStorageMsg msg) {
        inQueue().name(String.format("get-volume-snapshot-size-on-primarystorage-%s", self.getUuid()))
                .asyncBackup(msg)
                .run(chain -> getVolumeSnapshotSizeOnPrimaryStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                }));
    }

    protected void getVolumeSnapshotSizeOnPrimaryStorage(GetVolumeSnapshotSizeOnPrimaryStorageMsg msg, final NoErrorCompletion completion) {
        GetVolumeSnapshotSizeOnPrimaryStorageReply reply = new GetVolumeSnapshotSizeOnPrimaryStorageReply();
        VolumeSnapshotVO snapshotVO = dbf.findByUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class);

        String installPath = snapshotVO.getPrimaryStorageInstallPath();
        GetVolumeSnapshotSizeCmd cmd = new GetVolumeSnapshotSizeCmd();
        cmd.fsId = getSelf().getFsid();
        cmd.uuid = self.getUuid();
        cmd.volumeSnapshotUuid = snapshotVO.getUuid();
        cmd.installPath = installPath;

        httpCall(GET_VOLUME_SNAPSHOT_SIZE_PATH, cmd, GetVolumeSnapshotSizeRsp.class, new ReturnValueCompletion<GetVolumeSnapshotSizeRsp>(msg) {
            @Override
            public void success(GetVolumeSnapshotSizeRsp rsp) {
                reply.setActualSize(rsp.actualSize);
                reply.setSize(rsp.size);
                bus.reply(msg, reply);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    @Override
    protected void handle(CheckVolumeSnapshotOperationOnPrimaryStorageMsg msg) {
        bus.reply(msg, new CheckVolumeSnapshotOperationOnPrimaryStorageReply());
    }

    protected void handle(GetVolumeWatchersMsg msg) {
        GetVolumeWatchersReply reply = new GetVolumeWatchersReply();

        String installPath = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                .select(VolumeVO_.installPath)
                .findValue();

        // volume might be recovering
        try {
            final String query = new URI(installPath).getQuery();
            if (query != null) {
                installPath = StringUtils.removeEnd(installPath, '?' + query);
            }
        } catch (URISyntaxException ignored) {
        }

        GetVolumeWatchersCmd cmd = new GetVolumeWatchersCmd();
        cmd.volumePath = installPath;

        new HttpCaller<>(GET_IMAGE_WATCHERS_PATH, cmd, GetVolumeWatchersRsp.class, new ReturnValueCompletion<GetVolumeWatchersRsp>(msg) {
            @Override
            public void success(GetVolumeWatchersRsp returnValue) {
                reply.setWatchers(returnValue.watchers);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        }).setAvoidMonUuids(msg.getAvoidCephMonUuids()).tryNext().call();
    }

    protected void handle(GetVolumeBackingChainFromPrimaryStorageMsg msg) {
        GetVolumeBackingChainFromPrimaryStorageReply reply = new GetVolumeBackingChainFromPrimaryStorageReply();
        new While<>(msg.getRootInstallPaths()).each((installPath, compl) -> {
            GetBackingChainCmd cmd = new GetBackingChainCmd();
            cmd.volumePath = installPath;
            new HttpCaller<>(GET_BACKING_CHAIN_PATH, cmd, GetBackingChainRsp.class, new ReturnValueCompletion<GetBackingChainRsp>(msg) {
                @Override
                public void success(GetBackingChainRsp rsp) {
                    if (CollectionUtils.isEmpty(rsp.backingChain)) {
                        reply.putBackingChainInstallPath(installPath, Collections.emptyList());
                        reply.putBackingChainSize(installPath, 0L);
                    } else {
                        reply.putBackingChainInstallPath(installPath, rsp.backingChain.stream()
                                .map(it -> makeCephPath(it)).collect(Collectors.toList()));
                        reply.putBackingChainSize(installPath, rsp.totalSize);
                    }

                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.done();
                }
            }).call();
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    reply.setError(errorCodeList.getCauses().get(0));
                }

                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DeleteVolumeChainOnPrimaryStorageMsg msg) {
        DeleteVolumeChainOnPrimaryStorageReply reply = new DeleteVolumeChainOnPrimaryStorageReply();
        DeleteVolumeChainCmd cmd = new DeleteVolumeChainCmd();
        cmd.installPaths = msg.getInstallPaths();
        new HttpCaller<>(DELETE_VOLUME_CHAIN_PATH, cmd, DeleteVolumeChainRsp.class, new ReturnValueCompletion<DeleteVolumeChainRsp>(msg) {
            private void submitGcForUndeletedInstallPaths(List<String> undeletedInstallPaths) {
                CephDeleteVolumeChainGC volumeChainGC = new CephDeleteVolumeChainGC();
                volumeChainGC.primaryStorageUuid = self.getUuid();
                volumeChainGC.installPaths = undeletedInstallPaths;
                volumeChainGC.NAME = String.format("gc-ceph-%s-volume-chain-%s", self.getUuid(), msg.getChainTop());
                volumeChainGC.chainTop = msg.getChainTop();
                volumeChainGC.deduplicateSubmit(CephGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);
                logger.debug(String.format("unable to delete installPaths %s, now add GC to delete", undeletedInstallPaths));
            }

            @Override
            public void success(DeleteVolumeChainRsp rsp) {
                if (CollectionUtils.isEmpty(rsp.undeletedInstallPaths)) {
                    bus.reply(msg, reply);
                    return;
                }
                submitGcForUndeletedInstallPaths(rsp.undeletedInstallPaths);
                reply.setUndeletedInstallPaths(rsp.undeletedInstallPaths);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                submitGcForUndeletedInstallPaths(cmd.installPaths);
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        }).call();
    }

    private void handle(GetPrimaryStorageUsageReportMsg msg) {
        GetPrimaryStorageUsedPhysicalCapacityForecastReply reply = new GetPrimaryStorageUsedPhysicalCapacityForecastReply();
        List<CephPrimaryStoragePoolVO> poolVOs = Q.New(CephPrimaryStoragePoolVO.class)
                .in(CephPrimaryStoragePoolVO_.uuid, getPoolUuidsFromPoolUris(msg.getUris())).list();

        Map<String, UsageReport> osdUsageReport = poolUsageCollector
                .getUsageReportByResourceUuids(poolVOs.stream().map(vo -> vo.getOsdGroup().getUuid()).collect(Collectors.toList()));

        Map<String, UsageReport> poolUsageReport = new HashMap<>();
        poolVOs.forEach(poolVO -> poolUsageReport.put(poolVO.getUuid(), osdUsageReport.get(poolVO.getOsdGroup().getUuid())));

        reply.setUsageReportMap(poolUsageReport);
        bus.reply(msg, reply);
    }

    protected void handle(CleanUpStorageTrashOnPrimaryStorageMsg msg) {
        CleanUpStorageTrashOnPrimaryStorageReply reply = new CleanUpStorageTrashOnPrimaryStorageReply();
        thdf.singleFlightSubmit(new SingleFlightTask(msg)
            .setSyncSignature("cleanup-ceph-trash-for-ps-" + msg.getPrimaryStorageUuid())
            .run(completion ->
                cleanUpStorageTrash(new ReturnValueCompletion<Map>(completion) {
                    @Override
                    public void success(Map result) {
                        completion.success(result);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                }, msg.isForce())
            ).done(result -> {
                if (result.isSuccess()) {
                    reply.setResult((Map) result.getResult());
                    bus.reply(msg, reply);
                } else {
                    reply.setError(result.getErrorCode());
                    bus.reply(msg, reply);
                }
            }));
    }

    private void cleanUpStorageTrash(ReturnValueCompletion<Map> completion, boolean force) {
        List<String> pools = Q.New(CephPrimaryStoragePoolVO.class)
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, self.getUuid())
                .select(CephPrimaryStoragePoolVO_.poolName)
                .listValues();
        CleanTrashCmd cmd = new CleanTrashCmd();
        cmd.pools = pools;
        cmd.force = force;
        new HttpCaller<>(CLAEN_TRASH_PATH, cmd, CleanTrashRsp.class, new ReturnValueCompletion<CleanTrashRsp>(completion) {
            @Override
            public void success(CleanTrashRsp rsp) {
                completion.success(rsp.pool2TrashResult);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        }).call();
    }

    private List<String> getPoolUuidsFromPoolUris(List<String> poolUris) {
        return poolUris.stream().map(uri -> uri.replace("ceph://", "")).collect(Collectors.toList());
    }

    protected String makeVolumeInstallPathByTargetPool(String volUuid, String targetPoolName) {
        return String.format("ceph://%s/%s", targetPoolName, volUuid);
    }

    private String makeCephPath(String originPath) {
        if (originPath.startsWith("ceph://")) {
            return originPath;
        }

        return String.format("ceph://%s", originPath);
    }

    protected String getTargetPoolNameFromAllocatedUrl(String allocatedUrl) {
        if (allocatedUrl == null) {
            throw new OperationFailureException(operr("allocated url not found"));
        }


        if (!allocatedUrl.startsWith("ceph://")) {
            throw new OperationFailureException(operr("invalid allocated url:%s", allocatedUrl));
        }

        String path = allocatedUrl.replaceFirst("ceph://", "");
        return path.substring(0, path.lastIndexOf("/"));
    }

    private String makeResetImageRootVolumeInstallPath(String volUuid, String targetPoolName) {
        return String.format("ceph://%s/reset-image-%s-%s",
                targetPoolName,
                volUuid,
                System.currentTimeMillis());
    }

    private String getDefaultImageCachePoolName() {
        return CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL.getTokenByResourceUuid(self.getUuid(), CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN);
    }

    private String getDefaultDataVolumePoolName() {
        return CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL.getTokenByResourceUuid(self.getUuid(), CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL_TOKEN);
    }

    private String getDefaultRootVolumePoolName() {
        return CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL.getTokenByResourceUuid(self.getUuid(), CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL_TOKEN);
    }
}
