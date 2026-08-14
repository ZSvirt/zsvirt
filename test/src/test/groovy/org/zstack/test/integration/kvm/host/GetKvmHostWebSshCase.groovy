package org.zstack.test.integration.kvm.host

import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.KVMHost
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetHostWebSshUrlResult
import org.zstack.sdk.KVMHostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * @Author : jingwang
 * @create 2023/5/12 2:20 PM
 */
class GetKvmHostWebSshCase extends SubCase {
    EnvSpec env
    ClusterInventory cluster1
    KVMHostInventory host

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.oneHostEnv()
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            cluster1 = env.inventoryByName("cluster") as ClusterInventory
            host = env.inventoryByName("kvm") as KVMHostInventory
            testGetKvmHostWebSsh()
            testGetKvmHostWebSshOnHttps()
        }
    }

    void testGetKvmHostWebSsh() {
        def ret = getHostWebSshUrl {
            uuid = host.uuid
            userName = "root"
            password = "password"
        } as GetHostWebSshUrlResult

        String port = KVMGlobalConfig.HOST_WEBSSH_PORT.value();
        assert String.format("ws://{{ip}}:%s/ws?id=%s", port, "mockId") == ret.url
    }

    void testGetKvmHostWebSshOnHttps() {
        def ret = getHostWebSshUrl {
            uuid = host.uuid
            https = true
            userName = "root"
            password = "password"
        } as GetHostWebSshUrlResult

        String port = KVMGlobalConfig.HOST_WEBSSH_HTTPS_PORT.value();
        assert String.format("wss://{{ip}}:%s/ws?id=%s", port, "mockId") == ret.url
    }
}
