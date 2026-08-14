package org.zstack.header.vm;

import static org.zstack.header.vm.VmInstanceConstant.*;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/3/4 13:14
 */
public interface KvmReportVmShutdownEventExtensionPoint {
    void kvmReportVmShutdownEvent(ShutdownDetail vmUuid);

    public class ShutdownDetail {
        public String vmUuid;
        public String detail;
        public String opaque;

        public boolean triggerByHost;
        public boolean triggerByGuest;

        public static ShutdownDetail of(KvmReportVmShutdownEventMsg message) {
            ShutdownDetail struct = new ShutdownDetail();
            struct.vmUuid = message.getVmInstanceUuid();
            struct.detail = message.getDetail();
            struct.opaque = message.getOpaque();

            struct.triggerByHost = SHUTDOWN_DETAIL_BY_HOST.equals(struct.detail);
            struct.triggerByGuest = SHUTDOWN_DETAIL_BY_GUEST.equals(struct.detail);

            return struct;
        }
    }
}
