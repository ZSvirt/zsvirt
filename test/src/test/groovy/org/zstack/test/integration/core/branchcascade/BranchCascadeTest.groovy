package org.zstack.test.integration.core.branchcascade

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/22.
 */
class BranchCascadeTest extends Test {
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
        include("KvmTest.xml")
        eip()
        lb()
        portForwarding()
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
