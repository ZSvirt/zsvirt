package org.zstack.testlib.premium.network

import org.zstack.sdk.HostNetworkBondingInventory
import org.zstack.testlib.EnvSpec

class NetworkController {
    NetworkController(EnvSpec env) {
        this.env = env
    }

    final EnvSpec env

    HostNetworkBondingInventory getHostNetworkBonding(String hostUuid, String bondingName, boolean assertExist = true) {
        def bondings = env.queryHostNetworkBonding {
            conditions = ["hostUuid=${hostUuid}".toString(),
                          "bondingName=${bondingName}".toString()]
        } as List<HostNetworkBondingInventory>

        if (assertExist) {
            assert bondings.size() == 1
            return bondings.get(0)
        }

        return null
    }
}
