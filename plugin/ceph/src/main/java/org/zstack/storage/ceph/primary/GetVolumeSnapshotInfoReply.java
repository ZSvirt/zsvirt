package org.zstack.storage.ceph.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase.SnapInfo;

import java.util.List;

/**
 * Created by GuoYi on 10/19/17.
 */
public class GetVolumeSnapshotInfoReply extends MessageReply {
    private List<SnapInfo> snapInfos;

    public List<SnapInfo> getSnapInfos() {
        return snapInfos;
    }

    public void setSnapInfos(List<SnapInfo> snapInfos) {
        this.snapInfos = snapInfos;
    }
}
