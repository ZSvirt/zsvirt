package org.zstack.storage.backup.imagestore;

import org.zstack.header.HasThreadContext;
import org.zstack.header.agent.AgentResponse;
import org.zstack.header.agent.CancelCommand;
import org.zstack.header.log.NoLogging;
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands;
import org.zstack.utils.DebugUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ImageStoreBackupStorageCommands {
    public static class AgentCommand {
    }

    public static class ImageStoreResponse extends AgentResponse {
    }

    public static class ImageCommand extends AgentCommand {
        public String name;
        public String id;
    }

    public static class ConnectCmd extends AgentCommand {
        private String uuid;
        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class StorageInfo {
        private String type;
        private String url;
        private String options;

        public void setType(String type) {
            this.type = type;
        }

        public void setOptions(String options) {
            this.options = options;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getType() {
            return type;
        }

        public String getOptions() {
            return options;
        }

        public String getUrl() {
            return url;
        }

        public static StorageInfo valueOf(String tag) {
            Map<String, String> tokens = ImageStoreSystemTags.STORAGE_INFO.getTokensByTag(tag);
            StorageInfo result = new StorageInfo();
            result.type = tokens.get(ImageStoreSystemTags.FS_TYPE_TOKEN);
            result.url = tokens.get(ImageStoreSystemTags.URL_TOKEN);
            result.options = tokens.get(ImageStoreSystemTags.OPTION_TOKEN);
            return result;
        }

        public static StorageInfo valueOfUrl(String url) {
            StorageInfo result = new StorageInfo();
            result.type = url.split("://")[0];
            result.url = url;
            return result;
        }
    }

    public static class ConnectResponse extends AgentResponse {
        private long totalSize;
        private long freeSize;
        private List<String> ipAddresses;

        private String iscsiInitiatorName;
        private StorageInfo storageInfo;

        private int ioRate;
        private boolean supportFuse;

        public long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }

        public long getFreeSize() {
            return freeSize;
        }

        public void setFreeSize(long freeSize) {
            this.freeSize = freeSize;
        }

        public List<String> getIpAddresses() {
            return ipAddresses;
        }

        public void setIpAddresses(List<String> ipAddresses) {
            this.ipAddresses = ipAddresses;
        }

        public StorageInfo getStorageInfo() {
            return storageInfo;
        }

        public void setStorageInfo(StorageInfo storageInfo) {
            this.storageInfo = storageInfo;
        }

        public int getIoRate() {
            return ioRate;
        }

        public void setIoRate(int ioRate) {
            this.ioRate = ioRate;
        }

        public boolean isSupportFuse() {
            return supportFuse;
        }

        public void setSupportFuse(boolean supportFuse) {
            this.supportFuse = supportFuse;
        }

        public String getIscsiInitiatorName() {
            return iscsiInitiatorName;
        }

        public void setIscsiInitiatorName(String iscsiInitiatorName) {
            this.iscsiInitiatorName = iscsiInitiatorName;
        }
    }

    // PingCmd requires nothing in the body.
    public static class PingCmd extends AgentCommand {
        public String uuid;
    }

    public static class PingResponse extends ImageStoreResponse {
        private String uuid;
        public Long totalSize;
        public Long freeSize;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class DeleteImageCmd extends ImageCommand {
    }

    public static class DeleteImageResponse extends ImageStoreResponse {
    }

    public static class GetImageInfoCmd extends ImageCommand {
        public String imageUuid;
        public String installPath;
    }

    public static class GetImageChainInfoCmd extends ImageCommand {
        public String installPath;
    }

    public static class GetLocalCacheOnPrimaryCmd extends ImageCommand {
        public String imageUuid;
        public String url;
    }

    public static class GetLocalCacheOnPrimaryResponse extends AgentResponse {
        public String path;
    }

    public static class ImageInfoResponse extends ImageStoreResponse {
        public String name;
        public String id;
        public long size; // disk size
        public long virtualsize;
        public String blobsum;
    }

    public static class GetImageChainInfoResponse extends ImageStoreResponse {
        public List<ImageStoreImageResponse> chain;
    }

    public static class DeleteImagePackageCmd extends AgentCommand {
        private String exportUrl;

        public String getExportUrl() {
            return exportUrl;
        }

        public void setExportUrl(String exportUrl) {
            this.exportUrl = exportUrl;
        }
    }

    public static class DeleteImagePackageResponse extends AgentResponse {
    }

    public static class PackExportedImagesCmd extends AgentCommand {
        private List<String> installPathList;

        private String imageExportFormat;
        private String configFileContent;
        private String configFileFormat;
        private String packageName;
        private String packageFormat;

        public List<String> getInstallPathList() {
            return installPathList;
        }

        public void setInstallPathList(List<String> installPathList) {
            this.installPathList = installPathList;
        }

        public String getImageExportFormat() {
            return imageExportFormat;
        }

        public void setImageExportFormat(String imageExportFormat) {
            this.imageExportFormat = imageExportFormat;
        }

        public String getConfigFileContent() {
            return configFileContent;
        }

        public void setConfigFileContent(String configFileContent) {
            this.configFileContent = configFileContent;
        }

        public String getConfigFileFormat() {
            return configFileFormat;
        }

        public void setConfigFileFormat(String configFileFormat) {
            this.configFileFormat = configFileFormat;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getPackageFormat() {
            return packageFormat;
        }

        public void setPackageFormat(String packageFormat) {
            this.packageFormat = packageFormat;
        }
    }

    public static class PackExportedImagesResponse extends ImageStoreResponse {
        private long size;
        private String md5Sum;
        private String packageUrl;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getMd5Sum() {
            return md5Sum;
        }

        public void setMd5Sum(String md5Sum) {
            this.md5Sum = md5Sum;
        }

        public String getPackageUrl() {
            return packageUrl;
        }

        public void setPackageUrl(String packageUrl) {
            this.packageUrl = packageUrl;
        }
    }

    public static class ExportImageCmd extends AgentCommand implements HasThreadContext {
        private String extFmt = "";
        private String installPath;

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getExtFmt() {
            return extFmt;
        }

        public void setExtFmt(String extFmt) {
            this.extFmt = extFmt;
        }
    }

    public static class ExportImageAsRemoteTargetCmd extends AgentCommand implements HasThreadContext {
        private String installPath;
        private String remoteTargetType;
        private String hostname;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getRemoteTargetType() {
            return remoteTargetType;
        }

        public void setRemoteTargetType(String remoteTargetType) {
            this.remoteTargetType = remoteTargetType;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class ExportImageAsRemoteTargetResponse extends ImageStoreResponse {
        private String targetUri;

        public String getTargetUri() {
            return targetUri;
        }

        public void setTargetUri(String targetUri) {
            this.targetUri = targetUri;
        }
    }

    public static class DelExpImageCmd extends AgentCommand {
        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getExtFmt() {
            return extFmt;
        }

        public void setExtFmt(String extFmt) {
            this.extFmt = extFmt;
        }

        private String installPath;
        private String extFmt;
    }

    public static class PullImageCmd extends AgentCommand {
        private String token;
        private String addr;
        private String ca;
        private String name;
        private String id;

        public void setRefer(String installPath) {
            DebugUtils.Assert(installPath.length() > "zstore://".length(), String.format("ill format prefix of installPath: %s", installPath));

            String[] path = installPath.substring("zstore://".length()).split("/");

            DebugUtils.Assert(path.length == 2, String.format("ill format installPath: %s", installPath));
            name = path[0];
            id = path[1];
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getAddr() {
            return addr;
        }

        public void setAddr(String addr) {
            this.addr = addr;
        }

        public String getCa() {
            return ca;
        }

        public void setCa(String ca) {
            this.ca = ca;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class AllocateUploadSpaceCmd extends AgentCommand {
        private long expireHour = 72;

        public long getExpireHour() {
            return expireHour;
        }

        public void setExpireHour(long expireHour) {
            this.expireHour = expireHour;
        }
    }

    public static class AllocateUploadSpaceResponse extends AgentResponse {
        private String uploadDir;

        public String getUploadDir() {
            return uploadDir;
        }

        public void setUploadDir(String uploadDir) {
            this.uploadDir = uploadDir;
        }
    }

    public static class ExportNbdImagesCmd extends AgentCommand implements HasThreadContext {
        private String workspace;
        private List<Long> sizes;

        public String getWorkspace() {
            return workspace;
        }

        public void setWorkspace(String workspace) {
            this.workspace = workspace;
        }

        public List<Long> getSizes() {
            return sizes;
        }

        public void setSizes(List<Long> sizes) {
            this.sizes = sizes;
        }
    }

    public static class ExportNbdImagesRsp extends AgentResponse {
        private List<String> imagePaths;
        private List<Integer> ports;
        private String nbdDescription;

        public List<String> getImagePaths() {
            return imagePaths;
        }

        public void setImagePaths(List<String> imagePaths) {
            this.imagePaths = imagePaths;
        }

        public List<Integer> getPorts() {
            return ports;
        }

        public void setPorts(List<Integer> ports) {
            this.ports = ports;
        }

        public void setNbdDescription(String nbdDescription) {
            this.nbdDescription = nbdDescription;
        }

        public String getNbdDescription() {
            return nbdDescription;
        }
    }

    public static class CancelExportNbdImagesCmd extends AgentCommand {
        private List<String> imagePaths;
        private List<Integer> ports;
        private String nbdDescription;

        public List<String> getImagePaths() {
            return imagePaths;
        }

        public void setImagePaths(List<String> imagePaths) {
            this.imagePaths = imagePaths;
        }

        public List<Integer> getPorts() {
            return ports;
        }

        public void setPorts(List<Integer> ports) {
            this.ports = ports;
        }

        public void setNbdDescription(String nbdDescription) {
            this.nbdDescription = nbdDescription;
        }

        public String getNbdDescription() {
            return nbdDescription;
        }
    }

    public static class CancelExportNbdImagesRsp extends AgentResponse {
    }

    public static class GetImageHashCmd extends AgentCommand {
        private String alogrithm;
        private String installPath;

        public String getAlogrithm() {
            return alogrithm;
        }

        public void setAlogrithm(String alogrithm) {
            this.alogrithm = alogrithm;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }
    }

    public static class GetImageHashRsp extends AgentResponse {
        private String hash;

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }
    }

    public static class DeleteRemoteTargetCmd extends AgentCommand implements HasThreadContext, Serializable {
        @NoLogging
        private String targetUri;

        public String getTargetUri() {
            return targetUri;
        }

        public void setTargetUri(String targetUri) {
            this.targetUri = targetUri;
        }
    }

    public static class DeleteRemoteTargetRsp extends AgentResponse {
    }

    public static class PushImageCmd extends AgentCommand implements HasThreadContext {
        private String token;
        private String addr;
        private String ca;
        private String name;
        private String id;

        void setRefer(String installPath) {
            DebugUtils.Assert(installPath.trim().length() > "zstore://".length(), String.format("ill format prefix installPath: %s", installPath));

            String[] path = installPath.trim().substring("zstore://".length()).split("/");

            DebugUtils.Assert(path.length == 2, String.format("ill format installPath: %s", installPath));

            name = path[0];
            id = path[1];
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getAddr() {
            return addr;
        }

        public void setAddr(String addr) {
            this.addr = addr;
        }

        public String getCa() {
            return ca;
        }

        public void setCa(String ca) {
            this.ca = ca;
        }
    }

    public static class TaskProgressResponse extends ImageStoreResponse {
        private long lastOpTime;
        private String status;

        public long getLastOpTime() {
            return lastOpTime;
        }

        public void setLastOpTime(long lastOpTime) {
            this.lastOpTime = lastOpTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class DelExpImageResponse extends ImageStoreResponse {
    }

    public static class ExportImageResponse extends ImageStoreResponse {
        public String getImgUrl() {
            return imgUrl;
        }

        public void setImgUrl(String imgUrl) {
            this.imgUrl = imgUrl;
        }

        public String getMd5Sum() {
            return md5Sum;
        }

        public void setMd5Sum(String md5Sum) {
            this.md5Sum = md5Sum;
        }

        private String imgUrl;
        private String md5Sum;
    }

    public static class RunGarbageCollectorCmd extends AgentCommand {
        private List<String> imagesToKeep;

        public List<String> getImagesToKeep() {
            return imagesToKeep;
        }

        public void setImagesToKeep(List<String> imagesToKeep) {
            this.imagesToKeep = imagesToKeep;
        }
    }

    public static class RunGarbageCollectorResponse extends ImageStoreResponse {
        private long freed;

        public long getFreed() {
            return freed;
        }

        public void setFreed(long freed) {
            this.freed = freed;
        }
    }

    public static class SetStorageQuotaCmd extends AgentCommand {
        private long maxCapacity;

        public long getMaxCapacity() {
            return maxCapacity;
        }

        public void setMaxCapacity(long maxCapacity) {
            this.maxCapacity = maxCapacity;
        }
    }

    public static class SetStorageQuotaResponse extends ImageStoreResponse {
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

    public static class GetDownloadProgressResponse extends ImageStoreResponse {
        private boolean completed;
        private int progress;
        private long size;
        private long actualSize;
        private long downloadSize;
        private String installPath;
        private String format;
        private long lastOpTime;

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

        public long getDownloadSize() {
            return downloadSize;
        }

        public void setDownloadSize(long downloadSize) {
            this.downloadSize = downloadSize;
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

        public long getLastOpTime() {
            return lastOpTime;
        }

        public void setLastOpTime(long lastOpTime) {
            this.lastOpTime = lastOpTime;
        }
    }

    public static class DownloadImgCmd extends AgentCommand implements HasThreadContext, Serializable {
        @NoLogging(type = NoLogging.Type.Uri)
        public String imgurl;
        public String name;
        public String arch;
        public String desc;
        public String tag;
        public String user;
        public String uuid;
        public String imageuuid;
        public String cacert;
        public int concurrency = ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY.value(Integer.class) < 4 ?
                4 : ImageStoreGlobalConfig.BLOB_UPLOAD_CONCURRENCY.value(Integer.class);
    }

    public static class DownloadImgResponse extends ImageStoreResponse {
        public String name;
        public String id;
        public String blobsum;
        public long size;         // "qemu-img info" disk size
        public long virtualsize;  // "qemu-img info" virtual size

        // image store capacity
        public Long totalSize;
        public Long freeSize;
        public String format;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        // disk size
        public long getDiskSize() {
            return size;
        }

        public void setDiskSize(long actualSize) {
            this.size = size;
        }

        public String getBlobsum() {
            return blobsum;
        }

        public void setBlobsum(String blobsum) {
            this.blobsum = blobsum;
        }

        // virtual size
        public long getVirtualsize() {
            return virtualsize;
        }

        public void setVirtualsize(long size) {
            this.virtualsize = size;
        }

        public Long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(Long size) {
            this.totalSize = size;
        }

        public Long getFreeSize() {
            return freeSize;
        }

        public void setFreeSize(long freeSize) {
            this.freeSize = freeSize;
        }
    }

    public static class CancelDownloadImgCmd extends AgentCommand implements CancelCommand, Serializable {
        @NoLogging(type = NoLogging.Type.Uri)
        public String imgurl;
        public String name;
        public String imageuuid;
        public String cancellationApiId;

        @Override
        public void setCancellationApiId(String cancellationApiId) {
            this.cancellationApiId = cancellationApiId;
        }
    }

    public static class CancelDownloadImgRsp extends ImageStoreResponse {

    }

    public static class UploadImageToRemoteTargetCmd extends AgentCommand {
        private String remoteTargetUrl;
        private String installPath;
        private String format;
        private String imageFormat;
        private int concurrency;

        public void setRemoteTargetUrl(String remoteTargetUrl) {
            this.remoteTargetUrl = remoteTargetUrl;
        }

        public String getRemoteTargetUrl() {
            return remoteTargetUrl;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getFormat() {
            return format;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public int getConcurrency() {
            return concurrency;
        }

        public void setImageFormat(String imageFormat) {
            this.imageFormat = imageFormat;
        }

        public String getImageFormat() {
            return imageFormat;
        }
    }

    public static class UploadImageToRemoteTargetRsp extends AgentResponse {

    }

    public static class ImportImageCmd extends DownloadImgCmd {
        private String parent;
        public String processToRelease;

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }
    }

    public static class ImportImageResponse extends DownloadImgResponse {
    }

    public static class AllocateImageStoreInstallPathCmd extends AgentCommand {
        public String parent;
    }

    public static class AllocateImageStoreInstallPathResponse extends ImageStoreResponse {
        public String installPath;
    }

    public static class DeleteCmd extends AgentCommand {
        // the primary install path of the image, in the schema of:
        //   zstore://image-name/image-id
        private String installPath;
        private String uuid;
        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getUuid() {
            return uuid;
        }
    }

    public static class DeleteResponse extends ImageStoreResponse {
        // image store capacity
        public long totalSize;
        public long freeSize;

        public long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(long size) {
            this.totalSize = size;
        }

        public long getFreeSize() {
            return freeSize;
        }

        public void setFreeSize(long freeSize) {
            this.freeSize = freeSize;
        }
    }

    public static class GetImagesMetaDataCmd extends AgentCommand {
        private String backupStoragePath;

        public String getBackupStoragePath() {
            return backupStoragePath;
        }

        public void setBackupStoragePath(String backupStoragePath) {
            this.backupStoragePath = backupStoragePath;
        }
    }

    public static class GetImagesMetaDataRsp extends ImageStoreResponse{
        private String imagesMetaData;

        public String getImagesMetaData() {
            return imagesMetaData;
        }

        public void setImagesMetaData(String imagesMetaData) {
            this.imagesMetaData = imagesMetaData;
        }
    }

    public static class CheckImageMetaDataFileExistCmd extends AgentCommand {
        private String backupStoragePath;

        public String getBackupStoragePath() {
            return backupStoragePath;
        }

        public void setBackupStoragePath(String backupStoragePath) {
            this.backupStoragePath = backupStoragePath;
        }
    }

    public static class CheckImageMetaDataFileExistRsp extends ImageStoreResponse{
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

    public static class GenerateImageMetaDataFileCmd extends AgentCommand {
        private String backupStoragePath;

        public String getBackupStoragePath() {
            return backupStoragePath;
        }

        public void setBackupStoragePath(String backupStoragePath) {
            this.backupStoragePath = backupStoragePath;
        }
    }

    public static class GenerateImageMetaDataFileRsp extends ImageStoreResponse{
        private String backupStorageMetaFileName;

        public String getBackupStorageMetaFileName() {
            return backupStorageMetaFileName;
        }

        public void setBackupStorageMetaFileName(String backupStorageMetaFileName) {
            this.backupStorageMetaFileName = backupStorageMetaFileName;
        }
    }

    public static class DumpImageInfoToMetaDataFileCmd extends AgentCommand {
        private String backupStoragePath;
        private String imageMetaData;
        private boolean dumpAllMetaData;

        public boolean isDumpAllMetaData() {
            return dumpAllMetaData;
        }

        public void setDumpAllMetaData(boolean dumpAllMetaData) {
            this.dumpAllMetaData = dumpAllMetaData;
        }

        public String getBackupStoragePath() {
            return backupStoragePath;
        }

        public void setBackupStoragePath(String backupStoragePath) {
            this.backupStoragePath = backupStoragePath;
        }

        public String getImageMetaData() {
            return imageMetaData;
        }

        public void setImageMetaData(String imageMetaData) {
            this.imageMetaData = imageMetaData;
        }
    }

    public static class DumpImageInfoToMetaDataFileRsp extends ImageStoreResponse{
    }

    public static class DeleteImageInfoFromMetaDataFileCmd extends AgentCommand {
        private String imageUuid;
        private String imageBackupStorageUuid;
        private String backupStoragePath;

        public String getBackupStoragePath() {
            return backupStoragePath;
        }

        public void setBackupStoragePath(String backupStoragePath) {
            this.backupStoragePath = backupStoragePath;
        }

        public String getImageBackupStorageUuid() {
            return imageBackupStorageUuid;
        }

        public void setImageBackupStorageUuid(String imageBackupStorageUuid) {
            this.imageBackupStorageUuid = imageBackupStorageUuid;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }
    }

    public static class DeleteImageInfoFromMetaDataFileRsp extends ImageStoreResponse{
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

    public static class GetLocalFileSizeCmd extends SftpBackupStorageCommands.AgentCommand {
        public String path ;
    }

    public static class GetLocalFileSizeRsp extends SftpBackupStorageCommands.AgentResponse {
        public long size;
    }

    public static class CleanLocalImageStoreCacheCmd extends AgentCommand {
        public String mountPath;
    }

    public static class CleanLocalImageStoreCacheRsp extends AgentResponse {
    }

    public static class ArchiveStorageDataCmd extends AgentCommand {
        public String dstInstallPath;
        public boolean dryRun;
    }

    public static class ArchiveStorageDataRsp extends ImageStoreResponse {
        public long size;
    }

    public static class UnpackStorageDataCmd extends AgentCommand {
        public String srcInstallPath;
    }

    public static class UnpackStorageDataRsp extends ImageStoreResponse {
    }

    public static class SyncStorageDataCmd extends AgentCommand implements Serializable {
        public String installPath;
        public String hostname;
        public String username;
        @NoLogging
        public String password;
        public int sshPort;
    }

    public static class SyncStorageDataRsp extends ImageStoreResponse {
    }

    public static class CancelJobCmd extends AgentCommand implements CancelCommand {
        public String cancellationApiId;

        @Override
        public void setCancellationApiId(String cancellationApiId) {
            this.cancellationApiId = cancellationApiId;
        }
    }

    public static class CancelJobRsp extends ImageStoreResponse {

    }

    // ---- ZMigrate file operation commands ----

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

    public static class DownloadFileResponse extends ImageStoreResponse {
        public String md5sum;
        public long size;
        public String format;
    }

    public static class DeleteFilesCmd extends AgentCommand implements HasThreadContext, Serializable {
        public List<String> filePaths;
    }

    public static class DeleteFilesResponse extends ImageStoreResponse {
    }

    public static class UploadFileCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String taskUuid;
        public String installPath;
        @NoLogging(type = NoLogging.Type.Uri)
        public String url;
        public long timeout;
    }

    public static class UploadFileResponse extends ImageStoreResponse {
        public String directUploadUrl;
    }

    public static class GetDownloadFileProgressCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String taskUuid;
    }

    public static class GetDownloadFileProgressResponse extends ImageStoreResponse {
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

    public static class CancelDownloadFileCmd extends AgentCommand implements CancelCommand, Serializable {
        public String cancellationApiId;

        @Override
        public void setCancellationApiId(String cancellationApiId) {
            this.cancellationApiId = cancellationApiId;
        }
    }

    public static class CancelDownloadFileRsp extends ImageStoreResponse {
    }

    public static class UnzipFileCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String installPath;
    }

    public static class UnzipFileResponse extends ImageStoreResponse {
        public String unzipInstallPath;
        public Map<String, Long> fileSizes;
    }

    /**
     * Command to deploy a software upgrade package from an ImageStore backup storage host
     * to a target host via SCP/SSH.
     *
     * NOTE: targetHostSshPassword is the Base64-encoded password (not plaintext),
     * and is annotated with @NoLogging so it will never appear in logs.
     * The HTTP transport to the ImageStore agent is within the trusted management network.
     */
    public static class SoftwareUpgradePackageCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String upgradePackagePath;
        public String upgradePackageTargetPath;
        public String upgradeScriptPath;
        public int targetHostSshPort;
        public String targetHostSshUsername;
        @NoLogging
        public String targetHostSshPassword;
        public String targetHostIp;
        public String softwareType;
    }

    public static class SoftwareUpgradePackageResponse extends ImageStoreResponse {
    }
}
