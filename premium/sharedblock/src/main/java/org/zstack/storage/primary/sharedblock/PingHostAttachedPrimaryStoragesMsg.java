package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.Message;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

import java.util.ArrayList;
import java.util.List;

public class PingHostAttachedPrimaryStoragesMsg extends NeedReplyMessage implements PrimaryStorageMessage, SharedBlockGroupPrimaryStorageHypervisorSpecificMessage {
    private List<String> primaryStorageUuids;
    private String hypervisorType;
    private String hostUuid;
    private List<String> searchedMnIds = new ArrayList<>();

    public List<String> getSearchedMnIds() {
        return searchedMnIds;
    }

    public void setSearchedMnIds(List<String> searchedMnIds) {
        this.searchedMnIds = searchedMnIds;
    }

    public void addSearchedMnId(String mnId) {
        this.searchedMnIds.add(mnId);
    }

    public List<String> getPrimaryStorageUuids() {
        return primaryStorageUuids;
    }

    public void setPrimaryStorageUuids(List<String> primaryStorageUuids) {
        this.primaryStorageUuids = primaryStorageUuids;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuids.get(0);
    }
}
