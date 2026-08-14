package org.zstack.test.integration.storage.volume

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.volume.VolumeType
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import java.util.stream.Collectors

/**
 * Created by ads6 on 2018/1/12.
 */

class AttachDataVolumeMaxDataVolumeNumCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    private static final int MAX_DATA_VOLUME_NUMBER = 24
    private static final int EXCEED_DATA_VOLUME_NUMBER = 2

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
            vm = env.inventoryByName("vm") as VmInstanceInventory
            attachMultiDataVolumestoVm()
            testDataVolumeOrder()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void attachMultiDataVolumestoVm() {
        def diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory

        KVMAgentCommands.AttachDataVolumeCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME){rsp,HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body,KVMAgentCommands.AttachDataVolumeCmd.class)
            return rsp
        }

        int successAttachedVolumeNum = 0
        def threadList = []
        for (int i = 0; i < MAX_DATA_VOLUME_NUMBER + EXCEED_DATA_VOLUME_NUMBER; i++){
            VolumeInventory dataVolume = createDataVolume {
                name = 'test-vol'
                diskOfferingUuid = diskOffering.uuid
                systemTags = i % 2 == 0 ? ["capability::virtio-scsi"] : []
            } as VolumeInventory

            threadList.add(new Thread(new Runnable() {
                @Override
                void run() {
                    attachDataVolumeToVm {
                        vmInstanceUuid = vm.uuid
                        volumeUuid = dataVolume.uuid
                    }
                    successAttachedVolumeNum++
                }
            }))
        }

        threadList.each { it.start() }
        threadList.each { it.join() }

        // expect success-attached-volume-number will not exceed the max-data-volume-number
        assert successAttachedVolumeNum == MAX_DATA_VOLUME_NUMBER
        assert cmd.addons["attachedDataVolumes"].size() == MAX_DATA_VOLUME_NUMBER - 1
        assert Q.New(VolumeVO.class)
                .eq(VolumeVO_.type, VolumeType.Data)
                .eq(VolumeVO_.vmInstanceUuid, vm.uuid)
                .count() == MAX_DATA_VOLUME_NUMBER
    }

    void testDataVolumeOrder(){
        env.simulator(KVMConstant.KVM_START_VM_PATH){ HttpEntity<String> e ->
            KVMAgentCommands.StartVmCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            List<Integer> deviceIds = cmd.dataVolumes.stream().map({it -> it.getDeviceId()}).collect(Collectors.toList())
            int lastId = 0
            deviceIds.each {
                assert it > lastId
                lastId = it
            }
            return new KVMAgentCommands.StartVmResponse()
        }

        for (int i = 0; i < 3 ; i++) {
            rebootVmInstance {
                uuid = vm.uuid
            }
        }
    }
}
