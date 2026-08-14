package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

public class EstimateVolumeTemplateSizeMsg extends NeedReplyMessage implements VolumeMessage {
    private String volumeUuid;

    private boolean ignoreError = true;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public boolean isIgnoreError() {
        return ignoreError;
    }

    public void setIgnoreError(boolean ignoreError) {
        this.ignoreError = ignoreError;
    }
}
