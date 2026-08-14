package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

public class GetFileDownloadProgressReply extends MessageReply {
    private boolean completed;
    private int progress;

    private long size;
    private long actualSize;
    private long downloadSize;
    private String installPath;
    private long lastOpTime;
    private boolean supportSuspend;
    private String md5sum;

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

    public long getLastOpTime() {
        return lastOpTime;
    }

    public void setLastOpTime(long lastOpTime) {
        this.lastOpTime = lastOpTime;
    }

    public long getDownloadSize() {
        return downloadSize;
    }

    public void setDownloadSize(long downloadSize) {
        this.downloadSize = downloadSize;
    }

    public boolean isSupportSuspend() {
        return supportSuspend;
    }

    public void setSupportSuspend(boolean supportSuspend) {
        this.supportSuspend = supportSuspend;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }
}
