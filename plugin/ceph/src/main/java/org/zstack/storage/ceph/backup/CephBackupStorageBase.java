package org.zstack.storage.ceph.backup;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.agent.AgentConstant;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.SyncThread;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.Constants;
import org.zstack.header.HasThreadContext;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.*;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.CancelTaskResult;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.storage.ceph.*;
import org.zstack.storage.ceph.CephMonBase.PingResult;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.RemotePathValidator;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;
import static org.zstack.header.storage.backup.BackupStorageConstant.IMPORT_IMAGES_FAKE_RESOURCE_UUID;
import static org.zstack.header.storage.backup.BackupStorageConstant.RESTORE_IMAGES_BACKUP_STORAGE_METADATA_TO_DATABASE;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by frank on 7/27/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephBackupStorageBase extends BackupStorageBase {
    private static final CLogger logger = Utils.getLogger(CephBackupStorageBase.class);

    public CephBackupStorageBase() {
    }

    class ReconnectMonLock {
        AtomicBoolean hold = new AtomicBoolean(false);

        boolean lock() {
            return hold.compareAndSet(false, true);
        }

        void unlock() {
            hold.set(false);
        }
    }

    ReconnectMonLock reconnectMonLock = new ReconnectMonLock();

    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected CephBackupStorageMetaDataMaker metaDataMaker;
    @Autowired
    private ApiTimeoutManager timeoutManager;

    public enum PingOperationFailure {
        UnableToCreateFile,
        MonAddrChanged
    }

    public static class AgentCommand {
        String fsid;
        String uuid;

        public String getFsid() {
            return fsid;
        }

        public void setFsid(String fsid) {
            this.fsid = fsid;
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
        CephBackupStorageMonInventory handleMon;

        public AgentResponse() {
            boolean unitTestOn = CoreGlobalProperty.UNIT_TEST_ON;
            if (unitTestOn && type == null) {
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

    public static class Pool {
        String name;
        boolean predefined;
    }

    public static class InitCmd extends AgentCommand {
        List<Pool> pools;
    }

    public static class InitRsp extends AgentResponse {
        String fsid;

        public String getFsid() {
            return fsid;
        }

        public void setFsid(String fsid) {
            this.fsid = fsid;
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

    public static class CheckRsp extends AgentResponse {

    }

    public static class GetDownloadProgressCmd extends AgentCommand {
        private String imageUuid;

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }
    }

    public static class GetDownloadProgressRsp extends AgentResponse {
        private boolean completed;
        private int progress;
        private long size;
        private long actualSize;
        private String installPath;
        private String format;
        private long lastOpTime;
        private long downloadSize;

        public long getDownloadSize() {
            return downloadSize;
        }

        public void setDownloadSize(long downloadSize) {
            this.downloadSize = downloadSize;
        }

        public long getLastOpTime() {
            return lastOpTime;
        }

        public void setLastOpTime(long lastOpTime) {
            this.lastOpTime = lastOpTime;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    public static class DownloadCmd extends AgentCommand implements HasThreadContext, Serializable {
        @NoLogging(type = NoLogging.Type.Uri)
        String url;
        String installPath;
        String imageUuid;
        String sendCommandUrl;

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getSendCommandUrl() {
            return sendCommandUrl;
        }

        public void setSendCommandUrl(String sendCommandUrl) {
            this.sendCommandUrl = sendCommandUrl;
        }
    }

    public static class DownloadRsp extends AgentResponse {
        long size;
        Long actualSize;
        String uploadPath;
        String format;

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

        public String getUploadPath() {
            return uploadPath;
        }

        public void setUploadPath(String uploadPath) {
            this.uploadPath = uploadPath;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    public static class DeleteCmd extends AgentCommand {
        String installPath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class DeleteRsp extends AgentResponse {
    }

    public static class PingCmd extends AgentCommand {
    }

    public static class PingRsp extends AgentResponse {

    }

    public static class GetLocalFileSizeCmd extends AgentCommand {
        public String path ;
    }

    public static class GetLocalFileSizeRsp extends AgentResponse {
        public long size;
    }

    public static class GetImageSizeCmd extends AgentCommand {
        public String imageUuid;
        public String installPath;
    }

    public static class GetImageSizeRsp extends AgentResponse {
        public Long size;
        public Long actualSize;
    }

    public static class AddImageExportTokenCmd extends AgentCommand {
        public String installPath;
        public String token;
        public long expireTime;
    }

    public static class AddImageExportTokenRsp extends AgentResponse {
        public String md5sum;
    }

    public static class RemoveImageExportTokenCmd extends AgentCommand {
        public String installPath;
    }

    public static class RemoveImageExportTokenRsp extends AgentResponse {}

    public static class GetFactsCmd extends AgentCommand {
        public String monUuid;
    }

    public static class GetFactsRsp extends AgentResponse {
        public String fsid;
        public String monAddr;
    }

    public static class GetImagesMetaDataCmd extends AgentCommand {
        private String poolName;

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }
    }

    public static class GetImagesMetaDataRsp extends AgentResponse {
        private String imagesMetadata;

        public String getImagesMetadata() {
            return imagesMetadata;
        }

        public void setImagesMetadata(String imagesMetadata) {
            this.imagesMetadata = imagesMetadata;
        }
    }

    public static class CheckImageMetaDataFileExistCmd extends AgentCommand {
        private String poolName;

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }
    }

    public static class CheckImageMetaDataFileExistRsp extends AgentResponse {
        private String backupStorageMetaFileName;
        private Boolean exist;

        public Boolean getExist() {
            return exist;
        }

        public void setExist(Boolean exist) {
            this.exist = exist;
        }

        public String getBackupStorageMetaFileName() {
            return backupStorageMetaFileName;
        }

        public void setBackupStorageMetaFileName(String backupStorageMetaFileName) {
            this.backupStorageMetaFileName = backupStorageMetaFileName;
        }
    }

    public static class DumpImageInfoToMetaDataFileCmd extends AgentCommand {
        private String poolName;
        private String imageMetaData;
        private boolean dumpAllMetaData;

        public boolean isDumpAllMetaData() {
            return dumpAllMetaData;
        }

        public void setDumpAllMetaData(boolean dumpAllMetaData) {
            this.dumpAllMetaData = dumpAllMetaData;
        }

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }

        public String getImageMetaData() {
            return imageMetaData;
        }

        public void setImageMetaData(String imageMetaData) {
            this.imageMetaData = imageMetaData;
        }
    }

    public static class DumpImageInfoToMetaDataFileRsp extends AgentResponse {
    }

    public static class DeleteImageInfoFromMetaDataFileCmd extends AgentCommand {
        private String imageUuid;
        private String backupStorageUuid;
        private String poolName;

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }

        public String getBackupStorageUuid() {
            return backupStorageUuid;
        }

        public void setBackupStorageUuid(String backupStorageUuid) {
            this.backupStorageUuid = backupStorageUuid;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }
    }

    public static class DeleteImageInfoFromMetaDataFileRsp extends AgentResponse {
        private Integer ret;
        private String out;

        public Integer getRet() {
            return ret;
        }

        public void setRet(Integer ret) {
            this.ret = ret;
        }

        public String getOut() {
            return out;
        }

        public void setOut(String out) {
            this.out = out;
        }
    }

    public static class CephToCephMigrateImageCmd extends AgentCommand implements Serializable {
        String imageUuid;
        long imageSize;
        String srcInstallPath;
        String dstInstallPath;
        String dstMonHostname;
        String dstMonSshUsername;
        @NoLogging
        String dstMonSshPassword;
        int dstMonSshPort;

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public long getImageSize() {
            return imageSize;
        }

        public void setImageSize(long imageSize) {
            this.imageSize = imageSize;
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

    public static class CancelCommand extends AgentCommand implements org.zstack.header.agent.CancelCommand {
        private String cancellationApiId;
        private boolean allowTaskNotFound;

        @Override
        public void setCancellationApiId(String cancellationApiId) {
            this.cancellationApiId = cancellationApiId;
        }

        public boolean isAllowTaskNotFound() {
            return allowTaskNotFound;
        }

        public void setAllowTaskNotFound(boolean allowTaskNotFound) {
            this.allowTaskNotFound = allowTaskNotFound;
        }
    }

    public static class CancelResponse extends AgentResponse {
        private CancelTaskResult cancelResult;

        public CancelTaskResult getCancelResult() {
            return cancelResult == null ? CancelTaskResult.CANCEL_SIGNALLED : cancelResult;
        }

        public void setCancelResult(CancelTaskResult cancelResult) {
            this.cancelResult = cancelResult;
        }
    }

    public static class DownloadFileCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String taskUuid;
        public String installPath;
        @NoLogging(type = NoLogging.Type.Uri)
        public String url;
        @NoLogging(type = NoLogging.Type.Uri)
        public String urlScheme;
        public long timeout;
        @NoLogging(type = NoLogging.Type.Uri)
        public String sendCommandUrl;
    }

    public static class DownloadFileResponse extends AgentResponse {
        public String md5sum;
        public long size;
        public String format;
    }

    public static class DeleteFilesCmd extends AgentCommand implements HasThreadContext, Serializable {
        public List<String> filePaths;
    }

    public static class DeleteFilesResponse extends AgentResponse {
    }

    public static class UploadFileCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String taskUuid;
        public String installPath;
        @NoLogging(type = NoLogging.Type.Uri)
        public String url;
        public long timeout;
    }

    public static class UploadFileResponse extends AgentResponse {
        public String directUploadUrl;
    }

    public static class GetDownloadFileProgressCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String taskUuid;
    }

    public static class GetDownloadFileProgressResponse extends AgentResponse {
        public boolean completed;
        public int progress;
        public long size;
        public long actualSize;
        public String installPath;
        public String format;
        public long lastOpTime;
        public long downloadSize;
        public String md5sum;
        public boolean supportSuspend;
    }

    public static class UnzipFileCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String installPath;
    }

    public static class UnzipFileResponse extends AgentResponse {
        public String unzipInstallPath;
        public Map<String, Long> fileSizes;
    }

    /**
     * Command to deploy a software upgrade package from a Ceph backup storage host
     * to a target host via SCP/SSH.
     *
     * NOTE: targetHostSshPassword is the Base64-encoded password (not plaintext),
     * and is annotated with @NoLogging so it will never appear in logs.
     * The HTTP transport to the Ceph agent is within the trusted management network.
     *
     * TODO(security): The SSH password is transmitted over HTTP (management network) as Base64.
     * While @NoLogging prevents log leakage and the management network is considered trusted,
     * this is still vulnerable to network sniffing. Consider migrating to HTTPS transport
     * or pushing password handling down to the agent side (e.g., vault/key-based auth).
     */
    public static class SoftwareUpgradePackageCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String upgradePackagePath;
        public String upgradePackageTargetPath;
        public String upgradeScriptPath;
        public String softwareType;
        public int targetHostSshPort;
        public String targetHostSshUsername;
        @NoLogging
        public String targetHostSshPassword;
        public String targetHostIp;
    }

    public static class SoftwareUpgradePackageResponse extends AgentResponse {
    }

    // common response of storage migration
    public static class StorageMigrationRsp extends AgentResponse {
    }

    public static final String INIT_PATH = "/ceph/backupstorage/init";
    public static final String DOWNLOAD_IMAGE_PATH = "/ceph/backupstorage/image/download";
    public static final String GET_DOWNLOAD_PROGRESS_PATH = "/ceph/backupstorage/image/progress";
    public static final String DELETE_IMAGE_PATH = "/ceph/backupstorage/image/delete";
    public static final String GET_IMAGE_SIZE_PATH = "/ceph/backupstorage/image/getsize";
    public static final String ADD_EXPORT_TOKEN_PATH = "/ceph/backupstorage/image/export/addtoken";
    public static final String REMOVE_EXPORT_TOKEN_PATH = "/ceph/backupstorage/image/export/removetoken";
    public static final String PING_PATH = "/ceph/backupstorage/ping";
    public static final String GET_FACTS = "/ceph/backupstorage/facts";
    public static final String CHECK_IMAGE_METADATA_FILE_EXIST = "/ceph/backupstorage/checkimagemetadatafileexist";
    public static final String DUMP_IMAGE_METADATA_TO_FILE = "/ceph/backupstorage/dumpimagemetadatatofile";
    public static final String GET_IMAGES_METADATA = "/ceph/backupstorage/getimagesmetadata";
    public static final String DELETE_IMAGES_METADATA = "/ceph/backupstorage/deleteimagesmetadata";
    public static final String CHECK_POOL_PATH = "/ceph/backupstorage/checkpool";
    public static final String GET_LOCAL_FILE_SIZE = "/ceph/backupstorage/getlocalfilesize";
    public static final String CEPH_TO_CEPH_MIGRATE_IMAGE_PATH = "/ceph/backupstorage/image/migrate";

    public static final String FILE_DOWNLOAD_PATH = "/ceph/file/download";
    public static final String FILE_UPLOAD_PATH = "/ceph/file/upload";
    public static final String FILE_DOWNLOAD_PROGRESS_PATH = "/ceph/file/progress";
    public static final String DELETE_FILES_PATH = "/ceph/files/delete";
    public static final String UNZIP_FILE_PATH = "/ceph/file/unzip";
    public static final String SOFTWARE_UPGRADE_PACKAGE_DEPLOY_PATH = "/ceph/upgrade/deploy";

    protected String makeImageInstallPath(String imageUuid) {
        return String.format("ceph://%s/%s", getSelf().getPoolName(), imageUuid);
    }

    private <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback) {
        httpCall(path, cmd, retClass, callback, null, 0);
    }

    private <T extends AgentResponse> void httpCall(final String path, final AgentCommand cmd, final Class<T> retClass, final ReturnValueCompletion<T> callback, TimeUnit unit, long timeout) {
        new HttpCaller<>(path, cmd, retClass, callback, unit, timeout).call();
    }

    protected class HttpCaller<T extends AgentResponse> {
        private Iterator<CephBackupStorageMonBase> it;
        private List<ErrorCode> errorCodes = new ArrayList<ErrorCode>();

        private final String path;
        private final AgentCommand cmd;
        private final Class<T> retClass;
        private final ReturnValueCompletion<T> callback;
        private final TimeUnit unit;
        private final long timeout;

        private String randomFactor = null;
        private boolean tryNext = false;

        HttpCaller(String path, AgentCommand cmd, Class<T> retClass, ReturnValueCompletion<T> callback) {
            this(path, cmd, retClass, callback, null, 0);
        }

        HttpCaller(String path, AgentCommand cmd, Class<T> retClass, ReturnValueCompletion<T> callback, TimeUnit unit, long timeout) {
            this.path = path;
            this.cmd = cmd;
            this.retClass = retClass;
            this.callback = callback;
            this.unit = unit;
            this.timeout = timeout;
        }

        void call() {
            it = prepareMons().iterator();
            prepareCmd();
            doCall();
        }

        HttpCaller<T> specifyOrder(String randomFactor) {
            this.randomFactor = randomFactor;
            return this;
        }

        HttpCaller<T> tryNext() {
            this.tryNext = true;
            return this;
        }

        private void prepareCmd() {
            cmd.uuid = self.getUuid();
            cmd.fsid = getSelf().getFsid();
        }

        private List<CephBackupStorageMonBase> prepareMons() {
            final List<CephBackupStorageMonBase> mons = new ArrayList<CephBackupStorageMonBase>();
            for (CephBackupStorageMonVO monvo : getSelf().getMons()) {
                mons.add(new CephBackupStorageMonBase(monvo));
            }

            if (randomFactor != null) {
                CollectionUtils.shuffleByKeySeed(mons, randomFactor, it -> it.getSelf().getUuid());
            } else {
                Collections.shuffle(mons);
            }

            mons.removeIf(it -> it.getSelf().getStatus() != MonStatus.Connected);
            if (mons.isEmpty()) {
                throw new OperationFailureException(
                        operr("all ceph mons are Disconnected in ceph backup storage[uuid:%s]", self.getUuid())
                );
            }
            return mons;
        }

        private void doCall() {
            if (!it.hasNext()) {
                callback.fail(operr("all monitors cannot execute http call[%s]", path));

                return;
            }

            CephBackupStorageMonBase base = it.next();

            ReturnValueCompletion<T> completion = new ReturnValueCompletion<T>(callback) {
                @Override
                public void success(T ret) {
                    if (!(cmd instanceof InitCmd)) {
                        updateCapacityIfNeeded(ret);
                    }

                    ret.handleMon = CephBackupStorageMonInventory.valueOf(base.getSelf());
                    callback.success(ret);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    if (!errorCode.isError(SysErrors.OPERATION_ERROR)) {
                        String details = String.format("[mon:%s], %s", base.getSelf().getHostname(), errorCode.getDetails());
                        errorCode.setDetails(details);
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
    }

    public CephBackupStorageBase(BackupStorageVO self) {
        super(self);
    }

    protected CephBackupStorageVO getSelf() {
        return (CephBackupStorageVO) self;
    }

    protected CephBackupStorageInventory getInventory() {
        return CephBackupStorageInventory.valueOf(getSelf());
    }

    private void updateCapacityIfNeeded(AgentResponse rsp) {
        if (rsp.getTotalCapacity() != null && rsp.getAvailableCapacity() != null) {
            CephCapacity cephCapacity = new CephCapacity(getSelf().getFsid(), rsp);
            new CephCapacityUpdater().update(cephCapacity);
        }
    }

    @Override
    protected void handle(final GetImageSizeOnBackupStorageMsg msg) {
        CephBackupStorageBase.GetImageSizeCmd cmd = new CephBackupStorageBase.GetImageSizeCmd();
        cmd.imageUuid = msg.getImageUuid();
        cmd.installPath = msg.getImageUrl();

        final GetImageSizeOnBackupStorageReply reply = new GetImageSizeOnBackupStorageReply();
        httpCall(GET_IMAGE_SIZE_PATH, cmd, GetImageSizeRsp.class, new ReturnValueCompletion<GetImageSizeRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(GetImageSizeRsp ret) {
                reply.setSize(ret.size);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(final GetImageDownloadProgressMsg msg) {
        GetDownloadProgressCmd cmd = new GetDownloadProgressCmd();
        cmd.setImageUuid(msg.getImageUuid());
        cmd.setFsid(getSelf().getFsid());
        cmd.setUuid(self.getUuid());

        GetImageDownloadProgressReply r = new GetImageDownloadProgressReply();

        CephBackupStorageMonVO monvo = Q.New(CephBackupStorageMonVO.class)
                .eq(CephBackupStorageMonVO_.backupStorageUuid, msg.getBackupStorageUuid())
                .eq(CephBackupStorageMonVO_.hostname, msg.getHostname())
                .find();
        if (monvo == null) {
            r.setError(operr(
                    "CephMon[hostname:%s] not found on backup storage[uuid:%s]",
                    msg.getHostname(), msg.getBackupStorageUuid()));
            bus.reply(msg, r);
            return;
        }

        new CephBackupStorageMonBase(monvo).httpCall(GET_DOWNLOAD_PROGRESS_PATH, cmd, GetDownloadProgressRsp.class, new ReturnValueCompletion<GetDownloadProgressRsp>(msg) {
            @Override
            public void success(GetDownloadProgressRsp resp) {
                r.setCompleted(resp.isCompleted());
                r.setProgress(resp.getProgress());
                r.setActualSize(resp.getActualSize());
                r.setSize(resp.getSize());
                r.setInstallPath(resp.getInstallPath());
                r.setFormat(resp.getFormat());
                r.setDownloadSize(resp.getDownloadSize());
                r.setLastOpTime(resp.getLastOpTime());
                r.setSupportSuspend(true);
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    protected void handle(final BakeImageMetadataMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("bake-image-metadata-for-bs-%s", msg.getBackupStorageUuid());
            }

            @Override
            public String getSyncSignature() {
                return String.format("bake-image-metadata-for-bs-%s", msg.getBackupStorageUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                bakeImageMetadata(msg, chain);
            }

        });
    }

    private void bakeImageMetadata(BakeImageMetadataMsg msg, final SyncTaskChain chain) {
        if (msg.getOperation().equals(CephConstants.AFTER_ADD_BACKUPSTORAGE)) {
            GetImagesMetaDataCmd cmd = new GetImagesMetaDataCmd();
            cmd.setPoolName(msg.getPoolName());
            final BakeImageMetadataReply reply = new BakeImageMetadataReply();

            httpCall(GET_IMAGES_METADATA, cmd, GetImagesMetaDataRsp.class, new ReturnValueCompletion<GetImagesMetaDataRsp>(msg) {
                @Override
                public void fail(ErrorCode err) {
                    reply.setError(err);
                    logger.error(String.format("get images metadata failed: %s", err));
                    bus.reply(msg, reply);
                    chain.next();
                }

                @Override
                public void success(GetImagesMetaDataRsp ret) {
                    logger.error(String.format("get images metadata: %s successfully", ret.getImagesMetadata()));
                    reply.setImagesMetadata(ret.getImagesMetadata());

                    RestoreImagesBackupStorageMetadataToDatabaseMsg rmsg = new RestoreImagesBackupStorageMetadataToDatabaseMsg();
                    rmsg.setImagesMetadata(ret.getImagesMetadata());
                    rmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                    bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, IMPORT_IMAGES_FAKE_RESOURCE_UUID);
                    bus.send(rmsg);

                    bus.reply(msg, reply);
                    chain.next();
                }
            });
        } else if (msg.getOperation().equals(CephConstants.AFTER_ADD_IMAGE)) {
            final BakeImageMetadataReply reply = new BakeImageMetadataReply();
            CheckImageMetaDataFileExistCmd cmd = new CheckImageMetaDataFileExistCmd();
            cmd.setPoolName(msg.getPoolName());
            httpCall(CHECK_IMAGE_METADATA_FILE_EXIST, cmd, CheckImageMetaDataFileExistRsp.class, new ReturnValueCompletion<CheckImageMetaDataFileExistRsp>(msg) {
                @Override
                public void fail(ErrorCode err) {
                    logger.error(String.format("check images metadata file: %s failed", reply.getBackupStorageMetaFileName()));
                    dumpImagesBackupStorageInfoToMetaDataFile(msg, reply, true, chain);
                }

                @Override
                public void success(CheckImageMetaDataFileExistRsp ret) {
                    dumpImagesBackupStorageInfoToMetaDataFile(msg, reply, false, chain);
                }
            });
        } else if (msg.getOperation().equals(CephConstants.AFTER_EXPUNGE_IMAGE)) {
            final BakeImageMetadataReply reply = new BakeImageMetadataReply();
            CephBackupStorageBase.DeleteImageInfoFromMetaDataFileCmd deleteCmd = new CephBackupStorageBase.DeleteImageInfoFromMetaDataFileCmd();
            deleteCmd.setImageUuid(msg.getImg().getUuid());
            deleteCmd.setBackupStorageUuid(msg.getBackupStorageUuid());
            deleteCmd.setPoolName(msg.getPoolName());
            httpCall(DELETE_IMAGES_METADATA, deleteCmd, DeleteImageInfoFromMetaDataFileRsp.class,
                    new ReturnValueCompletion<DeleteImageInfoFromMetaDataFileRsp>(msg, chain) {
                @Override
                public void success(DeleteImageInfoFromMetaDataFileRsp returnValue) {
                    bus.reply(msg, reply);
                    chain.next();
                }
                @Override
                public void fail(ErrorCode err) {
                    reply.setError(err);
                    logger.error(String.format("delete ceph images metadata failed"));
                    bus.reply(msg, reply);
                    chain.next();
                }
            });
        }
    }

    private void dumpImagesBackupStorageInfoToMetaDataFile(BakeImageMetadataMsg msg, BakeImageMetadataReply reply, boolean allImagesInfo, SyncTaskChain chain ) {
        ImageInventory img = msg.getImg();
        logger.debug("dump ceph images info to meta data file");
        DumpImageInfoToMetaDataFileCmd dumpCmd = new DumpImageInfoToMetaDataFileCmd();
        String metaData;
        if (allImagesInfo) {
            metaData = metaDataMaker.getAllImageInventories(img, null);
        } else {
            metaData = JSONObjectUtil.toJsonString(img);
        }
        dumpCmd.setImageMetaData(metaData);
        dumpCmd.setDumpAllMetaData(allImagesInfo);
        dumpCmd.setPoolName(msg.getPoolName());

        httpCall(DUMP_IMAGE_METADATA_TO_FILE, dumpCmd, DumpImageInfoToMetaDataFileRsp.class, new ReturnValueCompletion<DumpImageInfoToMetaDataFileRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                logger.error("dump ceph images metadata failed");
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public void success(DumpImageInfoToMetaDataFileRsp ret) {
                logger.debug("dump ceph images metadata successfully");
                bus.reply(msg, reply);
                chain.next();
            }
        });
    }

    private void handle(CephToCephMigrateImageMsg msg) {
        final CephToCephMigrateImageCmd cmd = new CephToCephMigrateImageCmd();
        cmd.setImageUuid(msg.getImageUuid());
        cmd.setImageSize(msg.getImageSize());
        cmd.setSrcInstallPath(msg.getSrcInstallPath());
        cmd.setDstInstallPath(msg.getDstInstallPath());
        cmd.setDstMonHostname(msg.getDstMonHostname());
        cmd.setDstMonSshUsername(msg.getDstMonSshUsername());
        cmd.setDstMonSshPassword(msg.getDstMonSshPassword());
        cmd.setDstMonSshPort(msg.getDstMonSshPort());

        final CephToCephMigrateImageReply reply = new CephToCephMigrateImageReply();
        httpCall(CEPH_TO_CEPH_MIGRATE_IMAGE_PATH, cmd, StorageMigrationRsp.class, new ReturnValueCompletion<StorageMigrationRsp>(msg) {
            @Override
            public void success(StorageMigrationRsp returnValue) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        }, TimeUnit.MILLISECONDS, msg.getTimeout());
    }

    private void handle(ExportImageFromBackupStorageMsg msg) {
        exportImage(msg);
    }

    @Override
    protected void handle(final DownloadImageMsg msg) {
        final DownloadCmd cmd = new DownloadCmd();
        cmd.url = msg.getImageInventory().getUrl();
        cmd.installPath = makeImageInstallPath(msg.getImageInventory().getUuid());
        cmd.imageUuid = msg.getImageInventory().getUuid();
        cmd.sendCommandUrl = restf.getSendCommandUrl();

        SQL.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageInventory().getUuid())
                .set(ImageBackupStorageRefVO_.installPath, cmd.installPath)
                .update();

        final DownloadImageReply reply = new DownloadImageReply();
        HttpCaller<DownloadRsp> caller = new HttpCaller<>(DOWNLOAD_IMAGE_PATH, cmd, DownloadRsp.class, new ReturnValueCompletion<DownloadRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DownloadRsp ret) {
                if (cmd.getUrl().startsWith("upload://")) {
                    reply.setInstallPath(ret.getUploadPath());
                } else {
                    reply.setInstallPath(cmd.installPath);
                }
                reply.setSize(ret.size);

                // current ceph has no way to get the actual size
                // if we cannot get the actual size from HTTP, use the virtual size
                long asize = ret.actualSize == null ? ret.size : ret.actualSize;
                reply.setActualSize(asize);
                reply.setMd5sum("not calculated");
                reply.setFormat(ret.format);
                bus.reply(msg, reply);
            }
        });

        if (cmd.url.startsWith("file:///") || cmd.url.startsWith("/")) {
            caller.tryNext();
        }

        String apiId = ThreadContext.get(Constants.THREAD_CONTEXT_API);
        caller.specifyOrder(apiId).call();
    }

    @Override
    protected void handle(final CancelDownloadImageMsg msg) {
        CancelDownloadImageReply reply = new CancelDownloadImageReply();

        CancelCommand cmd = new CancelCommand();
        cmd.setCancellationApiId(msg.getCancellationApiId());
        cmd.setAllowTaskNotFound(msg.isAllowTaskNotFound());

        new HttpCaller<>(AgentConstant.CANCEL_JOB, cmd, CancelResponse.class, new ReturnValueCompletion<CancelResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CancelResponse rsp) {
                reply.setCancelResult(rsp.getCancelResult());
                bus.reply(msg, reply);
            }
        }).specifyOrder(msg.getCancellationApiId()).tryNext().call();
    }

    @Override
    protected void handle(final DownloadVolumeMsg msg) {
        final DownloadCmd cmd = new DownloadCmd();
        cmd.url = msg.getUrl();
        cmd.installPath = makeImageInstallPath(msg.getVolume().getUuid());

        final DownloadVolumeReply reply = new DownloadVolumeReply();
        httpCall(DOWNLOAD_IMAGE_PATH, cmd, DownloadRsp.class, new ReturnValueCompletion<DownloadRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DownloadRsp ret) {
                reply.setInstallPath(cmd.installPath);
                reply.setSize(ret.size);
                reply.setMd5sum("not calculated");
                bus.reply(msg, reply);
            }
        });
    }

    @Transactional(readOnly = true)
    private boolean canDelete(String installPath) {
        String sql = "select count(c) from ImageBackupStorageRefVO img, ImageCacheVO c where img.imageUuid = c.imageUuid and img.backupStorageUuid = :bsUuid and img.installPath = :installPath";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("bsUuid", self.getUuid());
        q.setParameter("installPath", installPath);
        return q.getSingleResult() == 0;
    }

    @Override
    protected void handle(final DeleteBitsOnBackupStorageMsg msg) {
        final DeleteBitsOnBackupStorageReply reply = new DeleteBitsOnBackupStorageReply();
        if (!canDelete(msg.getInstallPath())) {
            //TODO: GC, the image is still referred, need to cleanup
            bus.reply(msg, reply);
            return;
        }

        DeleteCmd cmd = new DeleteCmd();
        cmd.installPath = msg.getInstallPath();

        httpCall(DELETE_IMAGE_PATH, cmd, DeleteRsp.class, new ReturnValueCompletion<DeleteRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                //TODO GC, do not reply error
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteRsp ret) {
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(BackupStorageAskInstallPathMsg msg) {
        BackupStorageAskInstallPathReply reply = new BackupStorageAskInstallPathReply();
        reply.setInstallPath(makeImageInstallPath(msg.getImageUuid()));
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final SyncImageSizeOnBackupStorageMsg msg) {
        GetImageSizeCmd cmd = new GetImageSizeCmd();
        cmd.imageUuid = msg.getImage().getUuid();

        ImageBackupStorageRefInventory ref = findOneOrNull(msg.getImage().getBackupStorageRefs(), arg -> self.getUuid().equals(arg.getBackupStorageUuid()));

        if (ref == null) {
            throw new CloudRuntimeException(String.format("cannot find ImageBackupStorageRefInventory of image[uuid:%s] for" +
                    " the backup storage[uuid:%s]", msg.getImage().getUuid(), self.getUuid()));
        }

        final SyncImageSizeOnBackupStorageReply reply = new SyncImageSizeOnBackupStorageReply();
        cmd.installPath = ref.getInstallPath();
        httpCall(GET_IMAGE_SIZE_PATH, cmd, GetImageSizeRsp.class, new ReturnValueCompletion<GetImageSizeRsp>(msg) {
            @Override
            public void success(GetImageSizeRsp rsp) {
                reply.setSize(rsp.size);

                // current ceph cannot get actual size
                long asize = rsp.actualSize == null ? msg.getImage().getActualSize() : rsp.actualSize;
                reply.setActualSize(asize);
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
    protected void handle(GetLocalFileSizeOnBackupStorageMsg msg) {
        GetLocalFileSizeOnBackupStorageReply reply = new GetLocalFileSizeOnBackupStorageReply();
        GetLocalFileSizeCmd cmd = new GetLocalFileSizeCmd();
        cmd.path = msg.getUrl();
        String apiId = ThreadContext.get(Constants.THREAD_CONTEXT_API);
        new HttpCaller<>(GET_LOCAL_FILE_SIZE, cmd, GetLocalFileSizeRsp.class, new ReturnValueCompletion<GetLocalFileSizeRsp>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(GetLocalFileSizeRsp ret) {
                reply.setSize(ret.size);
                bus.reply(msg, reply);
            }
        }).tryNext().specifyOrder(apiId).call();
    }

    @Override
    protected void handle(GetImageEncryptedOnBackupStorageMsg msg) {
        GetImageEncryptedOnBackupStorageReply reply = new GetImageEncryptedOnBackupStorageReply();

        String  installPath = Q.New(ImageBackupStorageRefVO.class)
                .select(ImageBackupStorageRefVO_.installPath)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, self.getUuid())
                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                .findValue();

        reply.setEncrypted(installPath);
        bus.reply(msg, reply);
    }

    @Override
    protected void connectHook(final boolean newAdded, final Completion completion) {
        final List<CephBackupStorageMonBase> mons = transform(getSelf().getMons(), CephBackupStorageMonBase::new);

        class Connector {
            private ErrorCodeList errorCodes = new ErrorCodeList();
            Iterator<CephBackupStorageMonBase> it = mons.iterator();

            void connect(final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    if (errorCodes.size() == mons.size()) {
                        trigger.fail(multiErr(errorCodes, "unable to connect to the ceph backup storage[uuid:%s], failed to connect all ceph monitors.",
                                        self.getUuid()));
                    } else {
                        // reload because mon status changed
                        self = dbf.reload(self);
                        trigger.next();
                    }
                    return;
                }

                final CephBackupStorageMonBase base = it.next();
                base.connect(new Completion(completion) {
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
        chain.setName(String.format("connect-ceph-backup-storage-%s", self.getUuid()));
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

                        final List<CephBackupStorageMonBase> mons = transformAndRemoveNull(getSelf().getMons(),
                                arg -> arg.getStatus() == MonStatus.Connected ? new CephBackupStorageMonBase(arg) : null);

                        DebugUtils.Assert(!mons.isEmpty(), "how can be no connected MON!!! ???");

                        List<ErrorCode> errs = Collections.synchronizedList(new ArrayList<>());
                        new While<>(mons).all((mon, coml) ->{
                            GetFactsCmd cmd = new GetFactsCmd();
                            cmd.uuid = self.getUuid();
                            cmd.monUuid = mon.getSelf().getUuid();
                            mon.httpCall(GET_FACTS, cmd, GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(coml) {
                                @Override
                                public void success(GetFactsRsp rsp) {
                                    CephBackupStorageMonVO monVO = mon.getSelf();
                                    fsids.put(monVO.getUuid(), rsp.fsid);
                                    monVO.setMonAddr(rsp.monAddr == null ? monVO.getHostname() : rsp.monAddr);
                                    dbf.update(monVO);
                                    coml.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errs.add(errorCode);
                                    coml.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errs.isEmpty()){
                                    trigger.fail(errs.get(0));
                                    return;
                                }

                                Set<String> set = new HashSet<String>();
                                set.addAll(fsids.values());

                                if (set.size() != 1) {
                                    StringBuilder sb = new StringBuilder("the fsid returned by mons are mismatching, it seems the mons belong to different ceph clusters:\n");
                                    for (CephBackupStorageMonBase mon : mons) {
                                        String fsid = fsids.get(mon.getSelf().getUuid());
                                        sb.append(String.format("%s (mon ip) --> %s (fsid)\n", mon.getSelf().getHostname(), fsid));
                                    }

                                    throw new OperationFailureException(operr(sb.toString()));
                                }

                                // check if there is another ceph setup having the same fsid
                                String fsId = set.iterator().next();

                                CephBackupStorageVO otherCeph = Q.New(CephBackupStorageVO.class)
                                        .eq(CephBackupStorageVO_.fsid, fsId)
                                        .notEq(CephBackupStorageVO_.uuid, self.getUuid())
                                        .find();
                                if (otherCeph != null) {
                                    throw new OperationFailureException(
                                            operr("there is another CEPH backup storage[name:%s, uuid:%s] with the same" +
                                                            " FSID[%s], you cannot add the same CEPH setup as two different backup storage",
                                                    otherCeph.getName(), otherCeph.getUuid(), fsId)
                                    );
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String _name_ = "check-pool";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {

                        Pool p = new Pool();
                        p.name = getSelf().getPoolName();
                        p.predefined = CephSystemTags.PREDEFINED_BACKUP_STORAGE_POOL.hasTag(self.getUuid());

                        if(!newAdded){
                            CheckCmd check = new CheckCmd();
                            check.setPools(list(p));
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
                        } else {
                            trigger.next();
                        }

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "init";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {

                        Pool p = new Pool();
                        p.name = getSelf().getPoolName();
                        p.predefined = CephSystemTags.PREDEFINED_BACKUP_STORAGE_POOL.hasTag(self.getUuid());


                        InitCmd cmd = new InitCmd();
                        cmd.pools = list(p);

                        httpCall(INIT_PATH, cmd, InitRsp.class, new ReturnValueCompletion<InitRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(InitRsp ret) {
                                if (getSelf().getFsid() == null) {
                                    getSelf().setFsid(ret.fsid);
                                    self = dbf.updateAndRefresh(self);
                                }

                                CephCapacityUpdater updater = new CephCapacityUpdater();
                                CephCapacity cephCapacity = new CephCapacity(ret.fsid, ret);
                                updater.update(cephCapacity, true);
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "generate-ceph-images-metadata-file";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (!newAdded) {
                            String backupStorageHostName = metaDataMaker.getHostnameFromBackupStorage(CephBackupStorageInventory.valueOf(getSelf()));
                            String backupStorageUuid = getSelf().getUuid();
                            metaDataMaker.dumpImagesBackupStorageInfoToMetaDataFile(null,true, backupStorageHostName, backupStorageUuid);
                        }
                        trigger.next();
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
                            self = dbf.reload(self);
                            if (!getSelf().getMons().isEmpty()) {
                                dbf.removeCollection(getSelf().getMons(), CephBackupStorageMonVO.class);
                            }
                        }

                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void pingHook(final Completion completion) {
        final List<CephBackupStorageMonBase> mons = transform(getSelf().getMons(), CephBackupStorageMonBase::new);

        final List<ErrorCode> errors = new ArrayList<>();

        class Ping {
            private AtomicBoolean replied = new AtomicBoolean(false);

            @AsyncThread
            private void reconnectMon(final CephBackupStorageMonBase mon, boolean delay) {
                if (!CephGlobalConfig.BACKUP_STORAGE_MON_AUTO_RECONNECT.value(Boolean.class)) {
                    logger.debug(String.format("do not reconnect the ceph backup storage mon[uuid:%s] as the global config[%s] is set to false",
                            self.getUuid(), CephGlobalConfig.BACKUP_STORAGE_MON_AUTO_RECONNECT.getCanonicalName()));
                    return;
                }

                // there has been a reconnect in process
                if (!reconnectMonLock.lock()) {
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
                            TimeUnit.SECONDS.sleep(CephGlobalConfig.BACKUP_STORAGE_MON_RECONNECT_DELAY.value(Long.class));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                        }
                    }

                    mon.connect(new Completion(releaseLock) {
                        @Override
                        public void success() {
                            logger.debug(String.format("successfully reconnected the mon[uuid:%s] of the ceph backup" +
                                    " storage[uuid:%s, name:%s]", mon.getSelf().getUuid(), self.getUuid(), self.getName()));
                            releaseLock.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            //TODO
                            logger.warn(String.format("failed to reconnect the mon[uuid:%s] of the ceph backup" +
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

            void ping() {
                // this is only called when all mons are disconnected
                final AsyncLatch latch = new AsyncLatch(mons.size(), new NoErrorCompletion() {
                    @Override
                    public void done() {
                        if (!replied.compareAndSet(false, true)) {
                            return;
                        }

                        ErrorCode err = errf.stringToOperationError(String.format("failed to ping the ceph backup storage[uuid:%s, name:%s]",
                                self.getUuid(), self.getName()), errors);
                        completion.fail(err);
                    }
                });

                for (final CephBackupStorageMonBase mon : mons) {
                    mon.ping(new ReturnValueCompletion<PingResult>(latch) {
                        private void thisMonIsDown(ErrorCode err) {
                            //TODO
                            logger.warn(String.format("cannot ping mon[uuid:%s] of the ceph backup storage[uuid:%s, name:%s], %s",
                                    mon.getSelf().getUuid(), self.getUuid(), self.getName(), err));
                            errors.add(err);
                            mon.changeStatus(MonStatus.Disconnected);
                            reconnectMon(mon, true);
                            latch.ack();
                        }

                        @Override
                        public void success(PingResult res) {
                            if (res.success) {
                                // as long as there is one mon working, the backup storage works
                                pingSuccess();

                                if (mon.getSelf().getStatus() == MonStatus.Disconnected) {
                                    reconnectMon(mon, false);
                                }

                            } else if (PingOperationFailure.UnableToCreateFile.toString().equals(res.failure)) {
                                // as long as there is one mon saying the ceph not working, the backup storage goes down
                                logger.warn(String.format("the ceph backup storage[uuid:%s, name:%s] is down, as one mon[uuid:%s] reports" +
                                        " an operation failure[%s]", self.getUuid(), self.getName(), mon.getSelf().getUuid(), res.error));
                                backupStorageDown();
                            } else if (!res.success || PingOperationFailure.MonAddrChanged.toString().equals(res.failure)) {
                                // this mon is down(success == false), but the backup storage may still work as other mons may work
                                ErrorCode errorCode = operr("operation error, because:%s", res.error);
                                thisMonIsDown(errorCode);
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
            private void backupStorageDown() {
                if (!replied.compareAndSet(false, true)) {
                    return;
                }

                // set all mons to be disconnected
                for (CephBackupStorageMonBase base : mons) {
                    base.changeStatus(MonStatus.Disconnected);
                }

                ErrorCode err = errf.stringToOperationError(String.format("failed to ping the backup primary storage[uuid:%s, name:%s]",
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
    public List<ImageInventory> scanImages() {
        return null;
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddMonToCephBackupStorageMsg) {
            handle((APIAddMonToCephBackupStorageMsg) msg);
        } else if (msg instanceof APIUpdateCephBackupStorageMonMsg) {
            handle((APIUpdateCephBackupStorageMonMsg) msg);
        } else if (msg instanceof APIRemoveMonFromCephBackupStorageMsg) {
            handle((APIRemoveMonFromCephBackupStorageMsg) msg);
        } else if (msg instanceof APIExportImageFromBackupStorageMsg) {
            handle((APIExportImageFromBackupStorageMsg) msg);
        } else if (msg instanceof APIDeleteExportedImageFromBackupStorageMsg) {
            handle((APIDeleteExportedImageFromBackupStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    @Override
    protected void handleLocalMessage(Message msg) throws URISyntaxException {
        if (msg instanceof BakeImageMetadataMsg) {
            handle((BakeImageMetadataMsg) msg);
        } else if (msg instanceof GetImageDownloadProgressMsg) {
            handle((GetImageDownloadProgressMsg) msg);
        } else if (msg instanceof CephToCephMigrateImageMsg) {
            handle((CephToCephMigrateImageMsg) msg);
        } else if (msg instanceof ExportImageFromBackupStorageMsg) {
            handle((ExportImageFromBackupStorageMsg) msg);
        }
        else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(APIDeleteExportedImageFromBackupStorageMsg msg) {
        APIDeleteExportedImageFromBackupStorageEvent evt = new APIDeleteExportedImageFromBackupStorageEvent(msg.getId());
        Tuple t = Q.New(ImageBackupStorageRefVO.class).select(ImageBackupStorageRefVO_.installPath, ImageBackupStorageRefVO_.exportUrl)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                .findTuple();
        if (t == null || t.get(1) == null) {
            bus.publish(evt);
            return;
        }

        RemoveImageExportTokenCmd cmd = new RemoveImageExportTokenCmd();
        cmd.installPath = t.get(0, String.class);
        httpCall(REMOVE_EXPORT_TOKEN_PATH, cmd, RemoveImageExportTokenRsp.class, new ReturnValueCompletion<RemoveImageExportTokenRsp>(evt) {
            @Override
            public void success(RemoveImageExportTokenRsp rsp) {
                SQL.New(ImageBackupStorageRefVO.class).set(ImageBackupStorageRefVO_.exportUrl, null)
                        .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                        .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                        .update();

                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIExportImageFromBackupStorageMsg msg) {
        APIExportImageFromBackupStorageEvent evt = new APIExportImageFromBackupStorageEvent(msg.getId());
        ExportImageFromBackupStorageMsg emsg = new ExportImageFromBackupStorageMsg();
        bus.makeLocalServiceId(emsg, BackupStorageConstant.SERVICE_ID);
        emsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        emsg.setImageUuid(msg.getImageUuid());
        emsg.setExportFormat(msg.getExportFormat());
        emsg.setImageName(Q.New(ImageVO.class).select(ImageVO_.name).eq(ImageVO_.uuid, msg.getImageUuid()).findValue());

        bus.send(emsg, new CloudBusCallBack(msg) {
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

    protected void exportImage(ExportImageFromBackupStorageMsg msg) {
        ExportImageFromBackupStorageReply reply = new ExportImageFromBackupStorageReply();
        Tuple t = Q.New(ImageBackupStorageRefVO.class).select(ImageBackupStorageRefVO_.installPath, ImageBackupStorageRefVO_.exportUrl)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                .findTuple();
        if (t == null) {
            reply.setError(operr("image[uuid: %s] is not on backup storage[uuid:%s, name:%s]",
                    msg.getImageUuid(), self.getUuid(), self.getName()));
            bus.reply(msg, reply);
            return;
        }

        if (t.get(1) != null) {
            reply.setImageUrl(t.get(1, String.class));
            bus.reply(msg, reply);
            return;
        }

        String imageInstallUrl = t.get(0, String.class);
        AddImageExportTokenCmd cmd = new AddImageExportTokenCmd();
        cmd.installPath = imageInstallUrl;
        cmd.token = Platform.getUuidFromBytes(msg.getImageUuid().getBytes());
        httpCall(ADD_EXPORT_TOKEN_PATH, cmd, AddImageExportTokenRsp.class, new ReturnValueCompletion<AddImageExportTokenRsp>(msg) {
            @Override
            public void success(AddImageExportTokenRsp rsp) {
                String url = buildUrl(rsp.handleMon.getHostname(), cmd.token);
                String imageName = Q.New(ImageVO.class).select(ImageVO_.name).eq(ImageVO_.uuid, msg.getImageUuid()).findValue();
                String exportUrl = CephBackStorageHelper.CephBackStorageExportUrl.addNameToExportUrl(url, imageName);
                SQL.New(ImageBackupStorageRefVO.class).eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                        .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                        .set(ImageBackupStorageRefVO_.exportUrl, exportUrl)
                        .set(ImageBackupStorageRefVO_.exportMd5Sum, rsp.md5sum)
                        .update();

                reply.setImageUrl(exportUrl);
                reply.setMd5sum(rsp.md5sum);
                bus.reply(msg, reply);
            }

            private String buildUrl(String hostname, String token) {
                String[] splits = imageInstallUrl.split("/");
                String poolName = splits[splits.length - 2];
                String imageName = splits[splits.length - 1];
                return CephAgentUrl.backupStorageUrl(hostname, CephBackupStorageMonBase.EXPORT) +
                        String.format("/%s/%s?token=%s", poolName, imageName, token);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIRemoveMonFromCephBackupStorageMsg msg) {
        List<CephBackupStorageMonVO> vos = Q.New(CephBackupStorageMonVO.class)
                .in(CephBackupStorageMonVO_.hostname, msg.getMonHostnames())
                .eq(CephBackupStorageMonVO_.backupStorageUuid, self.getUuid())
                .list();

        if (!vos.isEmpty()) {
            dbf.removeCollection(vos, CephBackupStorageMonVO.class);
        }

        String dstHostName = Q.New(CephBackupStorageMonVO.class).select(CephBackupStorageMonVO_.hostname)
                .eq(CephBackupStorageMonVO_.backupStorageUuid, self.getUuid())
                .orderByAsc(CephBackupStorageMonVO_.status)
                .limit(1).findValue();

        if (dstHostName != null) {
            vos.forEach(vo -> replaceImageExportUrl(vo.getHostname(), dstHostName));
        }

        APIRemoveMonFromCephBackupStorageEvent evt = new APIRemoveMonFromCephBackupStorageEvent(msg.getId());
        evt.setInventory(CephBackupStorageInventory.valueOf(dbf.reload(getSelf())));
        bus.publish(evt);
    }

    private void replaceImageExportUrl(String srcHostName, String dstHostName) {
        List<ImageBackupStorageRefVO> refs = Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, self.getUuid())
                .like(ImageBackupStorageRefVO_.exportUrl, "http://" + srcHostName + "%")
                .list();

        refs.forEach(it -> it.setExportUrl(it.getExportUrl().replace(srcHostName, dstHostName)));
        dbf.updateCollection(refs);
    }

    private void handle(final APIAddMonToCephBackupStorageMsg msg) {
        final APIAddMonToCephBackupStorageEvent evt = new APIAddMonToCephBackupStorageEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-mon-ceph-backup-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            List<CephBackupStorageMonVO> monVOs = new ArrayList<CephBackupStorageMonVO>();

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-mon-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (String url : msg.getMonUrls()) {
                            CephBackupStorageMonVO monvo = new CephBackupStorageMonVO();
                            MonUri uri = new MonUri(url);
                            monvo.setUuid(Platform.getUuid());
                            monvo.setStatus(MonStatus.Connecting);
                            monvo.setHostname(uri.getHostname());
                            monvo.setMonAddr(monvo.getHostname());
                            monvo.setMonPort(uri.getMonPort());
                            monvo.setSshPort(uri.getSshPort());
                            monvo.setSshUsername(uri.getSshUsername());
                            monvo.setSshPassword(uri.getSshPassword());
                            monvo.setBackupStorageUuid(self.getUuid());
                            monVOs.add(monvo);
                        }

                        dbf.persistCollection(monVOs);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        dbf.removeCollection(monVOs, CephBackupStorageMonVO.class);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "connect-mons";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<CephBackupStorageMonBase> bases = transform(monVOs, CephBackupStorageMonBase::new);

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

                        for (CephBackupStorageMonBase base : bases) {
                            base.connect(new Completion(trigger) {
                                @Override
                                public void success() {
                                    latch.ack();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
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
                        List<CephBackupStorageMonBase> bases = transform(monVOs, CephBackupStorageMonBase::new);

                        final List<ErrorCode> errors = new ArrayList<>();

                        final AsyncLatch latch = new AsyncLatch(bases.size(), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                // one fail, all fail
                                if (!errors.isEmpty()) {
                                    trigger.fail(multiErr(errors, "unable to add mon to ceph backup storage"));
                                } else {
                                    trigger.next();
                                }
                            }
                        });

                        for (final CephBackupStorageMonBase base : bases) {
                            GetFactsCmd cmd = new GetFactsCmd();
                            cmd.uuid = self.getUuid();
                            cmd.monUuid = base.getSelf().getUuid();
                            base.httpCall(GET_FACTS, cmd, GetFactsRsp.class, new ReturnValueCompletion<GetFactsRsp>(latch) {
                                @Override
                                public void success(GetFactsRsp rsp) {
                                    String fsid = rsp.fsid;
                                    if (!getSelf().getFsid().equals(fsid)) {
                                        errors.add(operr("the mon[ip:%s] returns a fsid[%s] different from the current fsid[%s] of the cep cluster," +
                                                        "are you adding a mon not belonging to current cluster mistakenly?", base.getSelf().getHostname(), fsid, getSelf().getFsid()));
                                    }

                                    CephBackupStorageMonVO monVO = base.getSelf();
                                    SQL.New(CephBackupStorageMonVO.class)
                                            .eq(CephBackupStorageMonVO_.uuid, monVO.getUuid())
                                            .set(CephBackupStorageMonVO_.monAddr, rsp.monAddr == null ? monVO.getHostname() : rsp.monAddr)
                                            .update();

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
                        evt.setInventory(CephBackupStorageInventory.valueOf(dbf.reload(getSelf())));
                        bus.publish(evt);
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

    private void handle(final APIUpdateCephBackupStorageMonMsg msg) {
        final APIUpdateCephBackupStorageMonEvent evt = new APIUpdateCephBackupStorageMonEvent(msg.getId());
        CephBackupStorageMonVO monvo = dbf.findByUuid(msg.getMonUuid(), CephBackupStorageMonVO.class);
        if (msg.getHostname() != null) {
            monvo.setHostname(msg.getHostname());
        }
        if (msg.getMonPort() != null && msg.getMonPort() > 0 && msg.getMonPort() <= 65535) {
            monvo.setMonPort(msg.getMonPort());
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
        evt.setInventory(CephBackupStorageInventory.valueOf(dbf.reload(getSelf())));
        bus.publish(evt);
    }

    @Override
    public void deleteHook() {
        String fsid = getSelf().getFsid();
        new SQLBatch() {
            @Override
            protected void scripts() {
                if(Q.New(CephPrimaryStorageVO.class).eq(CephPrimaryStorageVO_.fsid, fsid).find() == null){
                    SQL.New(CephCapacityVO.class).eq(CephCapacityVO_.fsid, fsid).delete();
                }

            }
        }.execute();
        dbf.removeCollection(getSelf().getMons(), CephBackupStorageMonVO.class);

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
        reply.setError(operr("ceph backup storage do not support calculate image hash"));
        bus.reply(msg, reply);
    }

    @SyncThread(signature = RESTORE_IMAGES_BACKUP_STORAGE_METADATA_TO_DATABASE)
    private void doRestoreImagesBackupStorageMetadataToDatabase(RestoreImagesBackupStorageMetadataToDatabaseMsg msg) {
        metaDataMaker.restoreImagesBackupStorageMetadataToDatabase(msg.getImagesMetadata(), msg.getBackupStorageUuid());
    }

    /**
     * Find a specific Ceph mon by its UUID.
     * @throws OperationFailureException if the mon cannot be found
     */
    private CephBackupStorageMonVO findMonByUuid(String backupStorageHostUuid) {
        if (backupStorageHostUuid == null) {
            throw new OperationFailureException(operr("backup storage host uuid is null"));
        }

        Set<CephBackupStorageMonVO> mons = getSelf().getMons();
        if (mons == null || mons.isEmpty()) {
            throw new OperationFailureException(operr("backup storage [%s] has no mon", getSelf().getName()));
        }

        return mons.stream()
                .filter(m -> m.getUuid().equals(backupStorageHostUuid)).findAny()
                .orElseThrow(() -> new OperationFailureException(
                        operr("failed to find mon with uuid [%s]", backupStorageHostUuid)));
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
            httpCall(FILE_UPLOAD_PATH, cmd, UploadFileResponse.class, new ReturnValueCompletion<UploadFileResponse>(msg) {
                @Override
                public void fail(ErrorCode err) {
                    reply.setError(err);
                    bus.reply(msg, reply);
                }

                @Override
                public void success(UploadFileResponse rsp) {
                    reply.setDirectUploadUrl(rsp.directUploadUrl);
                    reply.setBackupStorageHostUuid(rsp.handleMon.getMonUuid());
                    bus.reply(msg, reply);
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

        httpCall(FILE_DOWNLOAD_PATH, cmd, DownloadFileResponse.class, new ReturnValueCompletion<DownloadFileResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DownloadFileResponse rsp) {
                reply.setMd5sum(rsp.md5sum);
                reply.setSize(rsp.size);
                reply.setFormat(rsp.format);
                reply.setBackupStorageHostUuid(rsp.handleMon.getMonUuid());
                bus.reply(msg, reply);
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

        CephBackupStorageMonVO mon;
        try {
            mon = findMonByUuid(msg.getBackupStorageHostUuid());
        } catch (OperationFailureException e) {
            reply.setError(e.getErrorCode());
            bus.reply(msg, reply);
            return;
        }

        UnzipFileCmd cmd = new UnzipFileCmd();
        cmd.uuid = self.getUuid();
        cmd.fsid = getSelf().getFsid();
        cmd.installPath = msg.getInstallPath();

        CephBackupStorageMonBase monBase = new CephBackupStorageMonBase(mon);
        monBase.httpCall(UNZIP_FILE_PATH, cmd, UnzipFileResponse.class, new ReturnValueCompletion<UnzipFileResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(UnzipFileResponse rsp) {
                reply.setUnzipInstallPath(rsp.unzipInstallPath);
                reply.setFileSizes(rsp.fileSizes);
                bus.reply(msg, reply);
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

        CephBackupStorageMonVO mon;
        try {
            mon = findMonByUuid(msg.getBackupStorageHostUuid());
        } catch (OperationFailureException e) {
            reply.setError(e.getErrorCode());
            bus.reply(msg, reply);
            return;
        }

        DeleteFilesCmd cmd = new DeleteFilesCmd();
        cmd.uuid = self.getUuid();
        cmd.fsid = getSelf().getFsid();
        cmd.filePaths = msg.getFilePaths();

        CephBackupStorageMonBase monBase = new CephBackupStorageMonBase(mon);
        monBase.httpCall(DELETE_FILES_PATH, cmd, DeleteFilesResponse.class, new ReturnValueCompletion<DeleteFilesResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteFilesResponse rsp) {
                bus.reply(msg, reply);
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

        CephBackupStorageMonVO mon;
        try {
            mon = findMonByUuid(msg.getBackupStorageHostUuid());
        } catch (OperationFailureException e) {
            reply.setError(e.getErrorCode());
            bus.reply(msg, reply);
            return;
        }

        GetDownloadFileProgressCmd cmd = new GetDownloadFileProgressCmd();
        cmd.uuid = self.getUuid();
        cmd.fsid = getSelf().getFsid();
        cmd.taskUuid = msg.getTaskUuid();

        CephBackupStorageMonBase monBase = new CephBackupStorageMonBase(mon);
        monBase.httpCall(FILE_DOWNLOAD_PROGRESS_PATH, cmd, GetDownloadFileProgressResponse.class, new ReturnValueCompletion<GetDownloadFileProgressResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(GetDownloadFileProgressResponse rsp) {
                reply.setCompleted(rsp.completed);
                reply.setProgress(rsp.progress);
                reply.setActualSize(rsp.actualSize);
                reply.setSize(rsp.size);
                reply.setInstallPath(rsp.installPath);
                reply.setDownloadSize(rsp.downloadSize);
                reply.setLastOpTime(rsp.lastOpTime);
                reply.setMd5sum(rsp.md5sum);
                reply.setSupportSuspend(rsp.supportSuspend);
                reply.setFormat(rsp.format);
                bus.reply(msg, reply);
            }
        });
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
        String pathErr = RemotePathValidator.validateRemotePath(msg.getUpgradePackagePath(), "upgradePackagePath");
        if (pathErr != null) {
            reply.setError(operr(pathErr));
            bus.reply(msg, reply);
            return;
        }
        if (msg.getUpgradePackageTargetPath() != null) {
            pathErr = RemotePathValidator.validateRemotePath(msg.getUpgradePackageTargetPath(), "upgradePackageTargetPath");
            if (pathErr != null) {
                reply.setError(operr(pathErr));
                bus.reply(msg, reply);
                return;
            }
        }
        if (msg.getUpgradeScriptPath() != null) {
            pathErr = RemotePathValidator.validateRemotePath(msg.getUpgradeScriptPath(), "upgradeScriptPath");
            if (pathErr != null) {
                reply.setError(operr(pathErr));
                bus.reply(msg, reply);
                return;
            }
        }

        CephBackupStorageMonVO mon;
        try {
            mon = findMonByUuid(msg.getBackupStorageHostUuid());
        } catch (OperationFailureException e) {
            reply.setError(e.getErrorCode());
            bus.reply(msg, reply);
            return;
        }

        SoftwareUpgradePackageCmd cmd = new SoftwareUpgradePackageCmd();
        cmd.upgradePackagePath = msg.getUpgradePackagePath();
        cmd.upgradePackageTargetPath = msg.getUpgradePackageTargetPath();
        cmd.softwareType = msg.getSoftwareType();
        cmd.upgradeScriptPath = msg.getUpgradeScriptPath();
        cmd.targetHostSshPort = msg.getTargetHostSshPort();
        cmd.targetHostSshUsername = msg.getTargetHostSshUsername();
        cmd.targetHostSshPassword = msg.getTargetHostSshPassword();
        cmd.targetHostIp = msg.getTargetHostIp();

        CephBackupStorageMonBase monBase = new CephBackupStorageMonBase(mon);
        monBase.httpCall(SOFTWARE_UPGRADE_PACKAGE_DEPLOY_PATH, cmd, SoftwareUpgradePackageResponse.class, new ReturnValueCompletion<SoftwareUpgradePackageResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(SoftwareUpgradePackageResponse rsp) {
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(CancelDownloadFileOnBackupStorageHostMsg msg) {
        CancelDownloadFileOnBackupStorageHostReply reply = new CancelDownloadFileOnBackupStorageHostReply();

        if (StringUtils.isEmpty(msg.getCancellationApiId())) {
            reply.setError(operr("cancellationApiId is required to cancel a download task"));
            bus.reply(msg, reply);
            return;
        }

        CephBackupStorageMonVO mon;
        try {
            mon = findMonByUuid(msg.getBackupStorageHostUuid());
        } catch (OperationFailureException e) {
            reply.setError(e.getErrorCode());
            bus.reply(msg, reply);
            return;
        }

        CancelCommand cmd = new CancelCommand();
        cmd.setCancellationApiId(msg.getCancellationApiId());
        cmd.setAllowTaskNotFound(msg.isAllowTaskNotFound());

        CephBackupStorageMonBase monBase = new CephBackupStorageMonBase(mon);
        monBase.httpCall(AgentConstant.CANCEL_JOB, cmd, CancelResponse.class, new ReturnValueCompletion<CancelResponse>(msg) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CancelResponse rsp) {
                reply.setCancelResult(rsp.getCancelResult());
                bus.reply(msg, reply);
            }
        });
    }
}
