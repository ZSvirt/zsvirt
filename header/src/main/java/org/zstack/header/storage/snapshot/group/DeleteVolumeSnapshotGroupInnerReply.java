package org.zstack.header.storage.snapshot.group;

import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/7/10.
 */
public class DeleteVolumeSnapshotGroupInnerReply extends MessageReply {
    private List<DeleteSnapshotGroupResult> results = Collections.synchronizedList(new ArrayList<>());

    public List<DeleteSnapshotGroupResult> getResults() {
        return results;
    }

    public void setResults(List<DeleteSnapshotGroupResult> results) {
        this.results = results;
    }

    public void addResult(DeleteSnapshotGroupResult result) {
        results.add(result);
    }
}
