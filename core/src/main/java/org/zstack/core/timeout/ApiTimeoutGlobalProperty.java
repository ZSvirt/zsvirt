package org.zstack.core.timeout;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/3/26.
 */
@GlobalPropertyDefinition
public class ApiTimeoutGlobalProperty {
    @GlobalProperty(name="ApiTimeout.org.zstack.header.image.APIAddImageMsg", defaultValue = "timeout::72h")
    @Deprecated
    public static String APIAddImageMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg", defaultValue = "timeout::72h")
    @Deprecated
    public static String APICreateRootVolumeTemplateFromRootVolumeMsg;

    @GlobalProperty(name="ApiTimeout.org.zstack.header.volume.APICreateDataVolumeFromVolumeTemplateMsg", defaultValue = "timeout::72h")
    @Deprecated
    public static String APICreateDataVolumeFromVolumeTemplateMsg;

    @GlobalProperty(name="ApiTimeout.org.zstack.header.vm.APIMigrateVmMsg", defaultValue = "timeout::1h")
    @Deprecated
    public static String APIMigrateVmMsg;

    @GlobalProperty(name="ApiTimeout.org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg", defaultValue = "timeout::72h")
    @Deprecated
    public static String APICreateDataVolumeTemplateFromVolumeMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotMsg", defaultValue = "timeout::3h")
    @Deprecated
    public static String APICreateDataVolumeFromVolumeSnapshotMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg", defaultValue = "timeout::24h")
    @Deprecated
    public static String APILocalStorageMigrateVolumeMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotMsg", defaultValue = "timeout::72h")
    @Deprecated
    public static String APICreateRootVolumeTemplateFromVolumeSnapshotMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.volume.APICreateVolumeSnapshotMsg", defaultValue = "timeout::3h")
    @Deprecated
    public static String APICreateVolumeSnapshotMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.vm.APICreateVmInstanceMsg", defaultValue = "timeout::12h")
    @Deprecated
    public static String APICreateVmInstanceMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg", defaultValue = "timeout::6h")
    @Deprecated
    public static String APIDeleteVolumeSnapshotMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.vm.APIExpungeVmInstanceMsg", defaultValue = "timeout::3h")
    @Deprecated
    public static String APIExpungeVmInstanceMsg;
    @GlobalProperty(name="ApiTimeout.org.zstack.header.storage.snapshot.APIRevertVolumeFromSnapshotMsg", defaultValue = "timeout::24h")
    @Deprecated
    public static String APIRevertVolumeFromSnapshotMsg;

    @GlobalProperty(name="ApiTimeout.org.zstack.header.cluster.APIUpdateClusterOSMsg", defaultValue = "timeout::24h")
    @Deprecated
    public static String APIUpdateClusterOSMsg;

    @GlobalProperty(name="api.timeout.syncCallAPI", defaultValue = "5m")
    public static String SYNCCALL_API_TIMEOUT;
    @GlobalProperty(name="api.timeout.internalMessage", defaultValue = "30m")
    public static String INTERNAL_MESSAGE_TIMEOUT;
    @GlobalProperty(name="api.timeout.minimal", defaultValue = "5m")
    public static String MINIMAL_TIMEOUT;
}
