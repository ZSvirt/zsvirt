package org.zstack.test.integration.kvm.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.allocator.HostCapacityVO
import org.zstack.header.allocator.HostCapacityVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.GetCpuMemoryCapacityResult
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.kvm.host.HostEnv
import org.zstack.sdk.ClusterInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by zouye on 2017/3/1.
 */
class CheckHostCapacityWhenAddHostCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testCheckCapacityWhenAddHost()
            testCreateVmWithReserveCapacity()
        }
    }

    void testCreateVmWithReserveCapacity() {
        def image = env.inventoryByName("image1")
        def l3 = env.inventoryByName("l3")
        def instanceOffering = env.inventoryByName("instanceOffering")
        def cluster = env.inventoryByName("cluster")

        expect(AssertionError.class) {
            createVmInstance {
                name = "vm"
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = instanceOffering.uuid
                clusterUuid = cluster.uuid
            }
        }

        updateResourceConfig {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.name
            value = "1G"
            resourceUuid = cluster.uuid
        }

        GetCpuMemoryCapacityResult result = getCpuMemoryCapacity {
            clusterUuids = [cluster.uuid]
        }

        KVMGlobalConfig.RESERVED_CPU_CAPACITY.updateValue(result.availableCpu)

        createVmInstance {
            name = "vm"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instanceOffering.uuid
            clusterUuid = cluster.uuid
        }
    }

    void testCheckCapacityWhenAddHost() {
        ClusterInventory cluster = env.inventoryByName("cluster")
        KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.updateValue("1024G")

        env.afterSimulator(KVMConstant.KVM_HOST_CAPACITY_PATH) { rsp, HttpEntity<String> e ->
            rsp as KVMAgentCommands.HostCapacityResponse
            rsp.setCpuCoreNum(20)
            rsp.setTotalMemory(SizeUnit.GIGABYTE.toByte(10))
            return rsp
        }

        def action = new AddKVMHostAction()
        action.username = "root"
        action.password = "password"
        action.name = "addHost"
        action.managementIp = "localhost"
        action.clusterUuid = cluster.uuid
        action.sessionId = adminSession()

        AddKVMHostAction.Result res = action.call()
        assert res.error != null

        KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.updateValue("9G")

        res = action.call()
        assert res.error == null

        retryInSecs {
            Integer cpuCoreNum = Q.New(HostCapacityVO.class)
                    .eq(HostCapacityVO_.uuid, res.value.getInventory().uuid)
                    .select(HostCapacityVO_.cpuCoreNum)
                    .findValue()
            assert cpuCoreNum == 20
        }

        deleteHost {
            uuid = res.value.inventory.uuid
        }

        updateResourceConfig {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.name
            value = "11G"
            resourceUuid = cluster.uuid
        }

        res = action.call()
        assert res.error != null

        KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.updateValue("1024G")
        updateResourceConfig {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.name
            value = "9G"
            resourceUuid = cluster.uuid
        }
        res = action.call()
        assert res.error == null
    }

    @Override
    void clean() {
        env.delete()
    }
}
