package org.zstack.test.integration.computes.pci

import org.zstack.pciDevice.KvmPciDeviceBackend.PciDeviceKvmBackend
import org.zstack.pciDevice.PciDeviceTOExampleMaker
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test
import org.zstack.testlib.premium.PremiumEnv

class PciTest extends Test {
    static SpringSpec springSpec = PremiumEnv.makeSpring()

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }

    static PciDeviceKvmBackend.GetPciDevicesRsp m60Response() {
        def rsp = new PciDeviceKvmBackend.GetPciDevicesRsp()
        rsp.pciDevicesInfo.add(PciDeviceTOExampleMaker.M60_VIRTUALIZABLE())
        rsp.hostIommuStatus = true
        rsp.success = true
        return rsp
    }
}
