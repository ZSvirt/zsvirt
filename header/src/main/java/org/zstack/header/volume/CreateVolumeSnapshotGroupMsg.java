package org.zstack.header.volume;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.ConsistentType;
import org.zstack.header.storage.snapshot.SnapshotBackendOperation;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by MaJin on 2021/6/23.
 */
public class CreateVolumeSnapshotGroupMsg extends NeedReplyMessage implements CreateVolumeSnapshotGroupMessage, VolumeMessage {
    private String resourceUuid;
    private String rootVolumeUuid;
    private String name;
    private String description;
    private ConsistentType consistentType = ConsistentType.None;
    private SessionInventory session;
    private VmInstanceInventory vmInstance;

    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
    }


    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    @Override
    public String getResourceUuid() {
        return resourceUuid;
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

    public SnapshotBackendOperation getBackendOperation() {
        return SnapshotBackendOperation.FILE_CREATION;
    }

    @Override
    public void setVmInstance(VmInstanceInventory vmInstance) {
        this.vmInstance = vmInstance;
    }

    @Override
    public VmInstanceInventory getVmInstance() {
        return vmInstance;
    }

    @Override
    public String getVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setConsistentType(ConsistentType consistentType) {
        this.consistentType = consistentType;
    }

    public ConsistentType getConsistentType() {
        return consistentType;
    }

    @Override
    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

}
