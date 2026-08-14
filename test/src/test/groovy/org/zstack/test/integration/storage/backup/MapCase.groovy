package org.zstack.test.integration.storage.backup

import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by xing5 on 2017/3/31.
 */
class MapCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            L3NetworkInventory l3 = env.inventoryByName("l3")
            detachNetworkServiceFromL3Network {
                l3NetworkUuid = l3.uuid
                networkServices =  l3.networkServices.inject([:]) { map, col ->
                    map << [(col.networkServiceProviderUuid) : col.networkServiceType]
                }
            }
        }
    }
}
