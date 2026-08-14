package org.zstack.test.integration.networkservice.provider

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/27.
 */
class NetworkServiceProviderTest extends Test {
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
