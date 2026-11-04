package org.zstack.ha.fencers;

public interface KvmFencer {
    String getName();
    String vmFencedReason(String fencerName);
}
