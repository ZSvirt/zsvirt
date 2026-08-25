package org.zstack.allocator.domain.message;

import org.zstack.allocator.domain.api.AllocatorDomainMessage;
import org.zstack.header.message.DeletionMessage;

public class AllocatorDomainDeletionMsg extends DeletionMessage implements AllocatorDomainMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAllocatorDomainUuid() {
        return uuid;
    }
}
