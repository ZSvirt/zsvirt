package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * 请求目标主存储对指定文件做 backing file 前缀替换（prefix rebase）。
 * <p>
 * 各存储插件（LocalStorage / SharedBlock / NFS）自行选择 host、构造 agent command 并发送。
 */
public class RebaseVolumeBackingFileOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;

    /**
     * volume + snapshot 的 installPath 列表（已做路径替换的逻辑路径）。
     * LocalStorage / NFS 下即绝对路径；SharedBlock 下为 sharedblock:// scheme 路径，由插件内部转绝对路径。
     */
    private List<String> installPaths;

    /** 旧路径前缀 */
    private String oldPrefix;

    /** 新路径前缀 */
    private String newPrefix;

    /** 注册请求指定的 hostUuid（LocalStorage 需要，其他存储可忽略） */
    private String hostUuid;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public List<String> getInstallPaths() {
        return installPaths;
    }

    public void setInstallPaths(List<String> installPaths) {
        this.installPaths = installPaths;
    }

    public String getOldPrefix() {
        return oldPrefix;
    }

    public void setOldPrefix(String oldPrefix) {
        this.oldPrefix = oldPrefix;
    }

    public String getNewPrefix() {
        return newPrefix;
    }

    public void setNewPrefix(String newPrefix) {
        this.newPrefix = newPrefix;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
