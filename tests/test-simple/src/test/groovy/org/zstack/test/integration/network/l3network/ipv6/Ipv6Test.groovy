package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by shixin on 2018/09/25.
 */
class Ipv6Test extends Test {
    static SpringSpec springSpec = makeSpring {
        virtualRouter()
        vyos()
        kvm()
        localStorage()
        sftpBackupStorage()
        flatNetwork()
        securityGroup()
        nfsPrimaryStorage()
        include("vip.xml")
        include("vxlan.xml")
        eip()
        lb()
        portForwarding()
    }

    @Override
    void setup() {
        useSpring(springSpec)
        spring {
            include("eip.xml")
            securityGroup()
        }
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        runSubCases()
    }
}
