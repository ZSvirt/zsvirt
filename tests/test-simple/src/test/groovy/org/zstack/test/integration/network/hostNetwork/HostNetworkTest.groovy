package org.zstack.test.integration.network.hostNetwork

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test


class HostNetworkTest extends Test {
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
        include("HostNetworkManager.xml")
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

