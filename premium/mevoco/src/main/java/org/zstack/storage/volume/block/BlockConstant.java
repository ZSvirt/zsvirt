package org.zstack.storage.volume.block;

import org.zstack.header.volume.VolumeProtocol;

public interface BlockConstant {
    String BLOCK_PROTOCOL = VolumeProtocol.iSCSI.toString();

    String ISCSI_PATH_PREFIX = "iscsi://3260/";

    static String getInstanceIscsiIqn(String vmUuid) {
        return String.format("iqn.2015-01.io.zstack:initiator.instance.%s", vmUuid);
    }
}
