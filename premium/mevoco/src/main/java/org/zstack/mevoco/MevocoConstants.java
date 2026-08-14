package org.zstack.mevoco;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.storage.ceph.CephConstants;

import java.util.Arrays;
import java.util.List;

/**
 * Created by frank on 9/14/2015.
 */
public class MevocoConstants {
    public static final String CONVERGED_OFFERING_TYPE = "Converged";

    @PythonClass
    public static final String MEVOCO_VM_TYPE = "MevocoVm";

    public static final String KVM_NIC_QOS = "NicQos";
    public static final String KVM_VOLUME_QOS = "VolumeQos";

    public static final String KVM_USER_DEFINED_XML = "userDefinedXml";
    public static final String KVM_USER_DEFINED_XML_HOOK_SCRIPT = "userDefinedXmlHookScript";

    public static final String MEVOCO_HOST_ALLOCATOR_STRATEGY = "Mevoco";

    public static final String SERVICE_ID = "mevoco";
    public static final String ACTION_CATEGORY = "mevoco";

    public static final String VM_CONSOLE_SPICE = "spice";
    public static final String VM_CONSOLE_VNC = "vnc";
    public static final String VM_CONSOLE_VNCANDSPICE = "vncAndSpice";

    public static final String NATIVE_AIO = "NativeAio";

    public static final Long MAX_NIC_BANDWIDTH = 32*1024*1024*1024L; /*32G*/

    public static final String QCOW2_OPTIONS = "qcow2Options";
    public static final String PRIMARYSTORAGE_UUID = "primaryStorageUuid";

    public static final String VOLUMES_SNAPSHOT_RESULTS = "volumesSnapshotResults";

    public static final List<VmInstanceState> supportedVmStatesForLiveSnapshotOnVolumes = Arrays.asList(VmInstanceState.Running);

}
