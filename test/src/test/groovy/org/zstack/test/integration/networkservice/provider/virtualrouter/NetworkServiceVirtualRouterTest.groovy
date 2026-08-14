package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by shixin on 2018/04/10.
 */
class NetworkServiceVirtualRouterTest extends Test {
    static SpringSpec springSpec = makeSpring {
        localStorage()
        sftpBackupStorage()
        portForwarding()
        virtualRouter()
        flatNetwork()
        securityGroup()
        eip()
        lb()
        vyos()
        nfsPrimaryStorage()
        kvm()
        include("mediateApiValidator.xml")
    }

    @Override
    void setup() {
        useSpring(springSpec)
        spring {
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
