package org.zstack.header.host;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by yaoning.li on 2022-10-29.
 */
public interface PingHostFailedExtensionPoint {
    void afterPingHostFailed(String hostUuid, ErrorCode reason);
}
