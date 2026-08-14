package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

public class GetImageDownloadProgressReply extends MessageReply {
    private boolean completed;
    private int progress;

    private long size;
    private long actualSize;
    private long downloadSize;
    private String installPath;
    private String format;
    private long lastOpTime;
    private boolean supportSuspend;

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

    public boolean isDownloadComplete() {
        return actualSize > 0 && actualSize == downloadSize;
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
}
