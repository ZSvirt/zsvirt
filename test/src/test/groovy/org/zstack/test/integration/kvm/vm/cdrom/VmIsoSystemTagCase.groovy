package org.zstack.test.integration.kvm.vm.cdrom

import org.zstack.compute.vm.VmCdRomGlobalProperty
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.db.Q
import org.zstack.header.configuration.InstanceOfferingVO
import org.zstack.header.configuration.InstanceOfferingVO_
import org.zstack.header.image.ImageConstant
import org.zstack.header.message.MessageReply
import org.zstack.header.network.l3.L3NetworkVO
import org.zstack.header.network.l3.L3NetworkVO_
import org.zstack.header.vm.CreateVmInstanceMsg
import org.zstack.header.vm.DiskAO
import org.zstack.header.vm.VmCreationStrategy
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vm.VmNicSpec
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmCdRomInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2019/01/13.
 */
class VmIsoSystemTagCase extends SubCase {
    EnvSpec env
    DiskOfferingInventory diskOffering
    ImageInventory iso
    ImageInventory iso1
    ImageInventory iso2
    L3NetworkInventory l3
    InstanceOfferingInventory instanceOfferingInventory

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
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "iso_0"
                    url = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
                }

                image {
                    name = "iso_1"
                    url = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
                }

                image {
                    name = "iso_2"
                    url = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
                }

                image {
                    name = "image"
                    url = "http://zstack.org/download/image.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
            }
        }

    }

    @Override
    void test() {
        env.create {
            diskOffering = env.inventoryByName("diskOffering")
            iso = env.inventoryByName("iso_0")
            iso1 = env.inventoryByName("iso_1")
            iso2 = env.inventoryByName("iso_2")
            l3 = env.inventoryByName("l3")
            instanceOfferingInventory = env.inventoryByName("instanceOffering")

            testVmIsoSystemTag()
            /* hostname 不能相同的检擦去掉了
            testCreateTagError() */
        }
    }

    void testCreateTagError() {
        String hostName = "testHostName"
        CloudBus bus = bean(CloudBus.class)
        VmInstanceInventory hostNameVm = createVmInstance {
            name = "hostNameVm"
            instanceOfferingUuid = instanceOfferingInventory.uuid
            rootDiskOfferingUuid = diskOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.getUuid()]
            sessionId = currentEnvSpec.session.uuid
            systemTags = ["${VmSystemTags.HOSTNAME_TOKEN}::${hostName}".toString()]
        }
        MessageReply error = null
        CreateVmInstanceMsg cmsg = new CreateVmInstanceMsg()
        cmsg.setSystemTags(["${VmSystemTags.HOSTNAME_TOKEN}::${hostName}".toString()])
        cmsg.setZoneUuid(hostNameVm.getZoneUuid())
        cmsg.setInstanceOfferingUuid(hostNameVm.getInstanceOfferingUuid())
        cmsg.setClusterUuid(hostNameVm.getClusterUuid())
        cmsg.setName("testError")
        cmsg.setImageUuid(hostNameVm.getImageUuid())
        cmsg.setType(hostNameVm.getType())
        cmsg.setHostUuid(hostNameVm.getHostUuid())
        InstanceOfferingVO iovo = Q.New(InstanceOfferingVO.class).eq(InstanceOfferingVO_.uuid, hostNameVm.getInstanceOfferingUuid()).find()
        cmsg.setInstanceOfferingUuid(iovo.getUuid())
        cmsg.setCpuNum(iovo.getCpuNum())
        cmsg.setCpuSpeed(iovo.getCpuSpeed())
        cmsg.setMemorySize(iovo.getMemorySize())
        cmsg.setAllocatorStrategy(iovo.getAllocatorStrategy())
        L3NetworkVO l3VO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, l3.uuid).find()
        VmNicSpec nicSpec = new VmNicSpec(org.zstack.header.network.l3.L3NetworkInventory.valueOf(Arrays.asList(l3VO)))
        cmsg.setL3NetworkSpecs(Arrays.asList(nicSpec))
        cmsg.setDefaultL3NetworkUuid(hostNameVm.getDefaultL3NetworkUuid())
        cmsg.setStrategy(VmCreationStrategy.InstantStart.toString())
        cmsg.setAccountUuid(currentEnvSpec.session.getAccountUuid())

        def bootDisk = DiskAO.rootDisk().withImage(hostNameVm.getImageUuid())
        bootDisk.diskOfferingUuid = diskOffering.uuid
        cmsg.diskAOs = [bootDisk]

        bus.makeLocalServiceId(cmsg, VmInstanceConstant.SERVICE_ID)
        bus.send(cmsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                error = reply
            }
        })

        retryInSecs {
            assert error != null
            assert error.getError().getDetails().contains("${VmSystemTags.HOSTNAME_TOKEN}::${hostName}".toString())
            assert !Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Created).exists
        }
    }

    void testVmIsoSystemTag() {
        VmCdRomGlobalProperty.syncVmIsoSystemTag = true
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOfferingInventory.uuid
            rootDiskOfferingUuid = diskOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            systemTags = [
                    "${VmSystemTags.CD_ROM_LIST_TOKEN}::${iso.uuid}::${iso1.uuid}::${VmInstanceConstant.EMPTY_CDROM}".toString()
            ]
        }

        retryInSecs {
            List<String> isoUuids = getIsoUuidByVmUuid(vm.uuid)
            assert 2 == isoUuids.size()
            assert isoUuids.containsAll([
                    iso.uuid,
                    iso1.uuid
            ])
        }

        attachIsoToVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso2.uuid
        }
        retryInSecs {
            List<String> isoUuids = getIsoUuidByVmUuid(vm.uuid)
            assert 3 == isoUuids.size()
            assert isoUuids.containsAll([
                    iso.uuid,
                    iso1.uuid,
                    iso2.uuid
            ])
        }

        detachIsoFromVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso2.uuid
        }
        retryInSecs {
            List<String> isoUuids = getIsoUuidByVmUuid(vm.uuid)
            assert 2 == isoUuids.size()
            assert isoUuids.containsAll([
                    iso.uuid,
                    iso1.uuid
            ])
        }

        detachIsoFromVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso1.uuid
        }
        retryInSecs {
            List<String> isoUuids = getIsoUuidByVmUuid(vm.uuid)
            assert 1 == isoUuids.size()
            assert isoUuids.containsAll([
                    iso.uuid
            ])
        }
    }

    List<String> getIsoUuidByVmUuid(String vmUuid) {
        List<String> result = []

        List<Map<String, String>> tokenList = VmSystemTags.ISO.getTokensOfTagsByResourceUuid(vmUuid)
        for (Map<String, String> tokens : tokenList) {
            result.add(tokens.get(VmSystemTags.ISO_TOKEN))
        }

        return result
    }

}