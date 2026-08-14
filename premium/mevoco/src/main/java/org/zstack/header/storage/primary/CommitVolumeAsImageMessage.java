package org.zstack.header.storage.primary;

import org.zstack.header.image.AddImageMessage;

/**
 * Created by david on 8/3/16.
 */
public interface CommitVolumeAsImageMessage extends AddImageMessage {
    String getVolumeUuid();
}
