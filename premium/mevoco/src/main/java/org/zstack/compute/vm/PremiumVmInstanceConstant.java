package org.zstack.compute.vm;

import java.nio.file.Paths;

/**
 * Created by MaJin on 2021/3/15.
 */
public interface PremiumVmInstanceConstant {
    String VM_INSTANCE_FROM_VOLUME_SPEC = "VmInstanceFromVolumeSpec";
    String DATA_VOLUME_SNAPSHOT = "DataVolumeSnapshot";
    
    // The key of StartVmCmd.addOns
    String SYSTEM_VIRTIO_DRIVER_DEVICE_TYPE = "systemVirtioDriverDeviceType";

    String VIRTIO_DRIVER_VFD_AMD64_FILENAME = "virtio-win_amd64.vfd";
    String VIRTIO_DRIVER_VFD_X86_FILENAME = "virtio-win_x86.vfd";
    String VIRTIO_DRIVER_VFD_AMD64_PATH_ON_HOST = Paths.get("/var", "lib", "zstack", "virtio-drivers", VIRTIO_DRIVER_VFD_AMD64_FILENAME).toString();
    String VIRTIO_DRIVER_VFD_X86_PATH_ON_HOST = Paths.get("/var", "lib", "zstack", "virtio-drivers", VIRTIO_DRIVER_VFD_X86_FILENAME).toString();
    String MD5_VIRTIO_DRIVER_VFD_AMD64 = "700ce86ce5cacbbe816d9971dfaee3f7";
    String MD5_VIRTIO_DRIVER_VFD_X86 = "21af0366dceb6d7c8e5abdc7bbe128a1";
    String VIRTIO_DRIVER_DIRECTORY_ON_MN_CLASS_PATH = Paths.get("tools", "virtio-drivers").toString();

    String ANSIBLE_PLAYBOOK_NAME = "transmission.py";
    String ANSIBLE_MODULE_PATH = "ansible/transmission";
}
