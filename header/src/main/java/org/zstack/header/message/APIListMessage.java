package org.zstack.header.message;

import java.util.List;

@Deprecated
public abstract class APIListMessage extends APISyncCallMessage {
    private int length = Integer.MAX_VALUE;
    private int offset = 0;
    @NoJsonSchema
    private List<String> uuids;

    public APIListMessage() {
        uuids = null;
    }

    public APIListMessage(List<String> uuids) {
        super();
        this.uuids = uuids;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return length == -1 ? Integer.MAX_VALUE : length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

}
