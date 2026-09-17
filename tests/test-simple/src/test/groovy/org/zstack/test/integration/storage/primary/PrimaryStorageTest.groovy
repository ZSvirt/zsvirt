package org.zstack.test.integration.storage.primary

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by shixin on 2018/03/14.
 */
class PrimaryStorageTest extends Test {
    static SpringSpec springSpec = makeSpring {
        localStorage()
        nfsPrimaryStorage()
        sftpBackupStorage()
        smp()
        ceph()
        externalPrimaryStorage()
        zbs()
        virtualRouter()
        vyos()
        kvm()
        flatNetwork()
        securityGroup()
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
