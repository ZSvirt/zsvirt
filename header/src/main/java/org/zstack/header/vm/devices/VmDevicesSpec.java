package org.zstack.header.vm.devices;

import org.zstack.header.tpm.entity.TpmSpec;

public class VmDevicesSpec {
    /**
     * Default value of tpm must be null, because tpm.enable is true by default.
     */
    private TpmSpec tpm;
    private NvRamSpec nvRam;

    public TpmSpec getTpm() {
        return tpm;
    }

    public void setTpm(TpmSpec tpm) {
        this.tpm = tpm;
    }

    public NvRamSpec getNvRam() {
        return nvRam;
    }

    public void setNvRam(NvRamSpec nvRam) {
        this.nvRam = nvRam;
    }

    public static VmDevicesSpec __example__() {
        VmDevicesSpec spec = new VmDevicesSpec();
        spec.setTpm(TpmSpec.__example__());
        return spec;
    }
}
