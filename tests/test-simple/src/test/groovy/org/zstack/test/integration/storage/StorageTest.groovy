package org.zstack.test.integration.storage

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/27.
 */
class StorageTest extends Test {
    static SpringSpec springSpec = makeSpring {
        localStorage()
        nfsPrimaryStorage()
        sftpBackupStorage()
        externalPrimaryStorage()
        smp()
        ceph()
        virtualRouter()
        vyos()
        kvm()
        flatNetwork()
        securityGroup()
        eip()
        lb()
        portForwarding()
        expon()
        zbs()
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
