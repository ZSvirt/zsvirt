package org.zstack.test.integration.zql

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

class ZqlTest extends Test {
    static SpringSpec springSpec = makeSpring {
        sftpBackupStorage()
        localStorage()
        nfsPrimaryStorage()
        smp()
        virtualRouter()
        flatNetwork()
        securityGroup()
        kvm()
        ceph()
        vyos()
        flatNetwork()
        eip()
        lb()
        portForwarding()
        include("LongJobManager.xml")
        include("HostAllocateExtension.xml")
    }

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
}
