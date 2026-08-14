package org.zstack.zwatch.namespace;

import java.util.ArrayList;
import java.util.List;

import org.zstack.header.host.APIGetHostNUMATopologyMsg;
import org.zstack.header.storage.snapshot.APIGetVolumeSnapshotSizeMsg;
import org.zstack.header.vm.APIGetVmsCapabilitiesMsg;
import org.zstack.header.vm.APITakeVmConsoleScreenshotMsg;

/**
 * Created by ZStack on 2021/3/25.
 */
public class AuditConstants {
    public final static List<String> API_AUDIT_BLOCK_LIST = new ArrayList<>();

    static {
        API_AUDIT_BLOCK_LIST.add(APIGetVmsCapabilitiesMsg.class.getName());
        API_AUDIT_BLOCK_LIST.add(APIGetHostNUMATopologyMsg.class.getName());
        API_AUDIT_BLOCK_LIST.add("org.zstack.license.APIGetLicenseUKeyStatusMsg");
        API_AUDIT_BLOCK_LIST.add(APIGetVolumeSnapshotSizeMsg.class.getName());
        API_AUDIT_BLOCK_LIST.add(APITakeVmConsoleScreenshotMsg.class.getName());
    }
}
