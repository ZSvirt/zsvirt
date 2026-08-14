package org.zstack.storage.ceph.primary;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class GetVolumeWatchersReply extends MessageReply {
    private List<String> watchers;

    public List<String> getWatchers() {
        return watchers;
    }

    public void setWatchers(List<String> watchers) {
        this.watchers = watchers;
    }
}
