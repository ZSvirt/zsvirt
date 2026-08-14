package org.zstack.test.integration.kvm

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/22.
 */
class KvmTest extends Test {
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
