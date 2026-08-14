package org.zstack.header.storage.snapshot.group;

import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/7/10.
 */
public class RevertVmFromSnapshotGroupInnerReply extends MessageReply {
    private List<RevertSnapshotGroupResult> results = Collections.synchronizedList(new ArrayList<>());

    public List<RevertSnapshotGroupResult> getResults() {
        return results;
    }

    public void setResults(List<RevertSnapshotGroupResult> results) {
        this.results = results;
    }

    public void addResult(RevertSnapshotGroupResult result) {
        results.add(result);
    }
}
