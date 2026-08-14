package org.zstack.test.integration.networkservice.provider.flat.userdata

import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceVO
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

/**
 * Created by miao on 17-7-12.
 */
class LimitUserdataTagAmountCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory l3
    ImageInventory image
    InstanceOfferingInventory offering
    String userdata = "this test user data"
    String userdata2 = "this test user data2"

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
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

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "local"
                    url = "127.0.0.1:/nfs"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
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
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
            image = (env.specByName("image") as ImageSpec).inventory
            offering = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory

            testSetMoreThanOneUserdataWhenCreateVm()
            testAddMoreUserdataTag()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testSetMoreThanOneUserdataWhenCreateVm() {
        // test set more than one userdata tag when create vm
        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "vm"
        action.imageUuid = image.uuid
        action.l3NetworkUuids = [l3.uuid]
        action.instanceOfferingUuid = offering.uuid
        action.systemTags = [
                VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata.getBytes()))]),
                VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata2.getBytes()))])
        ]
        action.sessionId = env.session.uuid

        CreateVmInstanceAction.Result res = action.call()
        assert res.error != null
    }

    void testAddMoreUserdataTag() {
        // test add more userdata tag when one already exists
        vm = createVmInstance {
            name = "vm"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = offering.uuid
            systemTags = [VmSystemTags.USERDATA.instantiateTag([
                    (VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata.getBytes()))
            ])]
        } as VmInstanceInventory

        CreateSystemTagAction action2 = new CreateSystemTagAction(
                resourceType: VmInstanceVO.getSimpleName(),
                resourceUuid: vm.uuid,
                tag: VmSystemTags.USERDATA.instantiateTag([
                        (VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata2.getBytes()))
                ]),
                sessionId: currentEnvSpec.session.uuid
        )

        CreateSystemTagAction.Result res2 = action2.call()

        assert res2.error != null
    }

}


