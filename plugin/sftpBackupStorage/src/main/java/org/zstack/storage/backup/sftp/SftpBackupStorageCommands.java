package org.zstack.storage.backup.sftp;

import org.zstack.header.HasThreadContext;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.CancelTaskResult;

import java.io.Serializable;

public class SftpBackupStorageCommands {
    public static class AgentCommand {
        public String uuid;
    }

    public static class AgentResponse {
        private boolean success = true;
        private String error;
        private Long totalCapacity;
        private Long availableCapacity;

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

        public boolean isSuccess() {
            return success;
        }
        public void setSuccess(boolean success) {
            this.success = success;
        }
        public String getError() {
            return error;
        }
        public void setError(String error) {
            this.error = error;
        }
    }
    
    public static class PingCmd extends AgentCommand {
    }
    public static class PingResponse extends AgentResponse {
        private String uuid;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }
    
    public static class ConnectCmd extends AgentCommand {
        private String storagePath;
        private String sendCommandUrl;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getStoragePath() {
            return storagePath;
        }
        public void setStoragePath(String storagePath) {
            this.storagePath = storagePath;
        }

        public void setSendCommandUrl(String sendCommandUrl) {
            this.sendCommandUrl = sendCommandUrl;
        }

        public String getSendCommandUrl() {
            return sendCommandUrl;
        }
    }
    public static class ConnectResponse extends AgentResponse {
    }

    public static class DownloadCmd extends AgentCommand implements Serializable, HasThreadContext {
        private String imageUuid;
        private String installPath;
        @NoLogging(type = NoLogging.Type.Uri)
        private String url;
        private long timeout;
        private String urlScheme;

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
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

        public long getTimeout() {
            return timeout;
        }
        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
        public String getUrlScheme() {
            return urlScheme;
        }
        public void setUrlScheme(String urlScheme) {
            this.urlScheme = urlScheme;
        }

    }
    public static class DownloadResponse extends AgentResponse {
        public String md5Sum;
        public long size;
        public long actualSize;
        public String format;

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public String getMd5Sum() {
            return md5Sum;
        }
        public void setMd5Sum(String md5Sum) {
            this.md5Sum = md5Sum;
        }
        public long getSize() {
            return size;
        }
        public void setSize(long size) {
            this.size = size;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    public static class DeleteCmd extends AgentCommand {
        private String installUrl;
        public String getInstallUrl() {
            return installUrl;
        }

        public void setInstallUrl(String installUrl) {
            this.installUrl = installUrl;
        }
    }

    public static class DeleteResponse extends AgentResponse {
    }
    
    public static class GetSshKeyCommand extends AgentCommand {
    }
    public static class GetSshKeyResponse extends AgentResponse {
        private String sshKey;

        public String getSshKey() {
            return sshKey;
        }

        public void setSshKey(String sshKey) {
            this.sshKey = sshKey;
        }
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
        public long size;
        public long actualSize;
    }

    public static class GetImageHashCmd extends AgentCommand {
        public String path ;
    }

    public static class GetImageHashRsp extends AgentResponse {
        public String hash;
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

    public static class GetImagesMetaDataRsp extends AgentResponse {
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

    public static class CheckImageMetaDataFileExistRsp extends AgentResponse{
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

    public static class GenerateImageMetaDataFileRsp extends AgentResponse {
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

    public static class DumpImageInfoToMetaDataFileRsp extends AgentResponse {
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
}
