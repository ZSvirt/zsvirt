package org.zstack.storage.volume.block;

public interface BlockVolumeMessage {
    String getBlockVolumeUuid();
    
    String getName();
    
    String getDescription();
}
