package org.zstack.test.integration.kvm.hostallocator

import org.zstack.core.db.Q
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.GetVmStartingCandidateClustersHostsResult
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.*


/**
 * Created by david on 3/6/17.
 */
class LeastVmPreferredAllocatorCase extends SubCase {
    EnvSpec env
    String thisImageUuid
    String instOffering
    String l3uuid
    HostInventory host1, host2, host3

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.fourVmThreeHostEnv()
    }

    @Override
    void test() {
        env.create {
            thisImageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
            instOffering = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            l3uuid = (env.specByName("pubL3") as L3NetworkSpec).inventory.uuid
            host1 = env.inventoryByName("kvm1") as HostInventory
            host2 = env.inventoryByName("kvm2") as HostInventory
            host3 = env.inventoryByName("kvm3") as HostInventory
            testCreateMulitpleVMs(8)
            testLeastVmHostAllocateDryRunSorted()
        }
    }

    void testCreateMulitpleVMs(Long num) {
        KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.updateValue("2G")

        // We have three hosts, each has 8 CPU and 10G memory.
        // The reserved memory for each host is 1GB.
        //   host1 has 4 VMs  (instance offering: 2 CPU, 2GB)
        //   host2 has no VM
        //   host3 has no VM
        // We can still create 8 VMs in all, on host2 and host3, with the instance offering.

        def threads = []
        1.upto(num, {
            def vmName = "VM-${it}".toString()
            def thread = Thread.start {
                createVmInstance {
                    name = vmName
                    instanceOfferingUuid = instOffering
                    imageUuid = thisImageUuid
                    l3NetworkUuids = [l3uuid]
                }
            }

            threads.add(thread)
        })

        threads.each{it.join()}

        // We shall not be able to create another VM instance now

        def a = new CreateVmInstanceAction()
        a.name = "VMM"
        a.instanceOfferingUuid = instOffering
        a.imageUuid = thisImageUuid
        a.l3NetworkUuids = [l3uuid]
        a.sessionId = currentEnvSpec.session.uuid

        assert a.call().error != null
    }

    void testLeastVmHostAllocateDryRunSorted(){
        List<String> toStopVmUuids = Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid).eq(VmInstanceVO_.hostUuid, host3.uuid).listValues()
        toStopVmUuids.addAll(Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid).eq(VmInstanceVO_.hostUuid, host2.uuid).limit(2).listValues())
        toStopVmUuids.addAll(Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid).eq(VmInstanceVO_.hostUuid, host1.uuid).limit(1).listValues())
        assert toStopVmUuids.size() == 7

        for (String vmUuid : toStopVmUuids){
            stopVmInstance {
                uuid = vmUuid
            }
        }

        def r = getVmStartingCandidateClustersHosts {
            uuid = toStopVmUuids.get(0)
        } as GetVmStartingCandidateClustersHostsResult

        assert r.hosts.get(0).uuid == host3.uuid // host3 has no VM running
        assert r.hosts.get(1).uuid == host2.uuid // host2 has 2 VMs running
        assert r.hosts.get(2).uuid == host1.uuid // host1 has 3 VMs running
    }
}
