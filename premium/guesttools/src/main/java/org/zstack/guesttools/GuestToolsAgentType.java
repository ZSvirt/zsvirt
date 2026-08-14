package org.zstack.guesttools;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImagePlatform;
import org.zstack.kvm.KVMConstant;

public enum GuestToolsAgentType {
    WindowsOnKvm,
    LinuxOnKvm,
    BaremetalForLinux;

    public static GuestToolsAgentType fromImagePlatformAndHypervisorType(ImagePlatform platform, String hypervisorType) {
        if (KVMConstant.KVM_HYPERVISOR_TYPE.equals(hypervisorType)) {
            return fromImagePlatform(platform);
        }
        return null;
    }

    public static GuestToolsAgentType fromImagePlatform(ImagePlatform platform) {
        if (platform == ImagePlatform.Windows || platform == ImagePlatform.WindowsVirtio) {
            return WindowsOnKvm;
        } else {
            return LinuxOnKvm;
        }
    }
    
    public ImagePlatform toImagePlatform() {
        switch (this) {
        case WindowsOnKvm:
            return ImagePlatform.Windows;
        case LinuxOnKvm: case BaremetalForLinux:
            return ImagePlatform.Linux;
        }
        throw new CloudRuntimeException(String.format("fail to convert GuestToolsAgentType[%s] to ImagePlatform", this));
    }
}
