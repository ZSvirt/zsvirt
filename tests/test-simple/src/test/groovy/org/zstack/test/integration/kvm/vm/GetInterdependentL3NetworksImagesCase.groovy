package org.zstack.test.integration.kvm.vm

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.ConcurrentHashMap

/**
 * Created by zhanyong.miao on 2019/01/05.
 */
class GetInterdependentL3NetworksImagesCase extends SubCase {
    EnvSpec env

    def DOC = """
test the API ApiGetInterdependentL3NetworksImagesMsg
"""

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = prepareEnvironment()
    }

    @Override
    void test() {
        env.create {
            testGetL3NetworkByImage()
            testGetImageByL3Network()
            testGetL3NetworkByBS()
            testGetBSsByL3Networks()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    EnvSpec prepareEnvironment() {
        return Test.makeEnv {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "image2"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            cephBackupStorage {
                name="bs"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }
                image {
                    name = "test-image2"
                    url  = "http://zstack.org/download/vr.qcow2"
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
                    attachBackupStorage("sftp")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster-ceph"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(128)
                        totalCpu = 64
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(128)
                        totalCpu = 64
                    }

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(128)
                        totalCpu = 64
                    }

                    attachPrimaryStorage("ps")
                    attachBackupStorage("bs")
                    attachL2Network("l22")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                cephPrimaryStorage {
                    name="ps"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]

                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-1"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }

                    l3Network {
                        name = "l3-3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l22"
                    physicalInterface = "eth0"
                    vlan = 222

                    l3Network {
                        name = "l3-4"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.Centralized_DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     NetworkServiceType.DHCP.toString(),
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.101.10"
                            endIp = "192.168.101.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.101.1"
                        }
                    }
                }

            }
        }
    }

    void testGetL3NetworkByImage() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def sftpImage1 = env.inventoryByName("image1") as ImageInventory
        //def sftpImage2 = env.inventoryByName("image2") as ImageInventory
        def cephImage1 = env.inventoryByName("test-image1") as ImageInventory
        def cephImage2 = env.inventoryByName("test-image2") as ImageInventory
        def l31 = env.inventoryByName("l3-1") as L3NetworkInventory
        def l32 = env.inventoryByName("l3-2") as L3NetworkInventory
        def l33 = env.inventoryByName("l3-3") as L3NetworkInventory
        def l34 = env.inventoryByName("l3-4") as L3NetworkInventory

        List<L3NetworkInventory> l3s = getInterdependentL3NetworksImages {
            zoneUuid = zone.uuid
            imageUuid = sftpImage1.uuid
        }

        assert (3 == l3s.size())

        // batch api queries
        Map<Integer, List<L3NetworkInventory>> results = new ConcurrentHashMap<>()
        def threads = []
        for (int i = 0; i < 5; i++) {
            def key = i
            def thread = Thread.start {
                List<L3NetworkInventory> result = getInterdependentL3NetworksImages {
                    zoneUuid = zone.uuid
                    imageUuid = sftpImage1.uuid
                }
                results.put(key, result)
            }

            threads.add(thread)
        }

        threads.each { it.join() }
        assert (results.size() == 5)
        // confirm all list contains the same elements
        List<L3NetworkInventory> first = results.get(0)
        results.each { key, value ->
            assert (first.size() == value.size())
            first.each { l3 ->
                assert value.uuid.contains(l3.uuid)
            }
        }

        List<String> uuids = [l31.uuid, l32.uuid,l33.uuid]
        l3s.each { l3 ->
            assert uuids.contains(l3.uuid)
        }

        List< L3NetworkInventory> cephl3s = getInterdependentL3NetworksImages {
            zoneUuid = zone.uuid
            imageUuid = cephImage1.uuid
        }

        assert (1 == cephl3s.size())
        assert (cephl3s[0].uuid == l34.uuid )

        cephl3s = getInterdependentL3NetworksImages {
            zoneUuid = zone.uuid
            imageUuid = cephImage2.uuid
        }

        assert (1 == cephl3s.size())
        assert (cephl3s[0].uuid == l34.uuid )

    }

    void testGetImageByL3Network() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def sftpImage1 = env.inventoryByName("image1") as ImageInventory
        def sftpImage2 = env.inventoryByName("image2") as ImageInventory
        def cephImage1 = env.inventoryByName("test-image1") as ImageInventory
        def cephImage2 = env.inventoryByName("test-image2") as ImageInventory
        def l31 = env.inventoryByName("l3-1") as L3NetworkInventory
        def l32 = env.inventoryByName("l3-2") as L3NetworkInventory
        def l33 = env.inventoryByName("l3-3") as L3NetworkInventory
        def l34 = env.inventoryByName("l3-4") as L3NetworkInventory

        List< ImageInventory> images = getInterdependentL3NetworksImages {
            zoneUuid = zone.uuid
            l3NetworkUuids = [l31.uuid]
        }

        assert (2 == images.size())
        List<String> uuids = [sftpImage1.uuid, sftpImage2.uuid]
        images.each { image ->
            assert uuids.contains(image.uuid)
        }

        images = getInterdependentL3NetworksImages {
            zoneUuid = zone.uuid
            l3NetworkUuids = [l31.uuid, l32.uuid, l33.uuid]
        }

        assert (2 == images.size())
        images = getInterdependentL3NetworksImages {
            zoneUuid = zone.uuid
            l3NetworkUuids = [l34.uuid]
        }

        assert (2 == images.size())
        uuids = [cephImage1.uuid, cephImage2.uuid]
        images.each { image ->
            assert uuids.contains(image.uuid)
        }

        images = getInterdependentL3NetworksImages {
            zoneUuid = zone.uuid
            l3NetworkUuids = [l31.uuid, l32.uuid, l33.uuid, l34.uuid]
        }
        assert (0 == images.size())
    }

    void testGetL3NetworkByBS() {
        BackupStorageInventory ceph = env.inventoryByName("bs") as BackupStorageInventory
        BackupStorageInventory sftp = env.inventoryByName("sftp") as BackupStorageInventory
        ZoneInventory zone = env.inventoryByName("zone") as ZoneInventory
        L3NetworkInventory l31 = env.inventoryByName("l3-1") as L3NetworkInventory
        L3NetworkInventory l32 = env.inventoryByName("l3-2") as L3NetworkInventory
        L3NetworkInventory l33 = env.inventoryByName("l3-3") as L3NetworkInventory
        L3NetworkInventory l34 = env.inventoryByName("l3-4") as L3NetworkInventory

        def cephL3s = getInterdependentL3NetworksBackupStorages {
            zoneUuid = zone.uuid
            backupStorageUuid = ceph.uuid
        } as List

        assert cephL3s.size() == 1
        assert cephL3s.stream().allMatch({ l3 -> l3.uuid == l34.uuid })

        def sftpL3s = getInterdependentL3NetworksBackupStorages {
            zoneUuid = zone.uuid
            backupStorageUuid = sftp.uuid
        } as List

        assert sftpL3s.size() == 3
        assert sftpL3s.stream().allMatch({l3 -> l3.uuid in [l31.uuid, l32.uuid, l33.uuid]})

        def L3s = getInterdependentL3NetworksImages {
            zoneUuid = zone.uuid
        } as List

        assert L3s.size() == 4
        assert L3s.stream().allMatch({l3 -> l3.uuid in [l31.uuid, l32.uuid, l33.uuid, l34.uuid]})
    }

    void testGetBSsByL3Networks() {
        BackupStorageInventory ceph = env.inventoryByName("bs") as BackupStorageInventory
        BackupStorageInventory sftp = env.inventoryByName("sftp") as BackupStorageInventory
        ZoneInventory zone = env.inventoryByName("zone") as ZoneInventory
        L3NetworkInventory l31 = env.inventoryByName("l3-1") as L3NetworkInventory
        L3NetworkInventory l34 = env.inventoryByName("l3-4") as L3NetworkInventory

        def l31Bs = getInterdependentL3NetworksBackupStorages {
            zoneUuid = zone.uuid
            l3NetworkUuids = [l31.uuid]
        } as List

        assert l31Bs.size() == 1
        assert l31Bs.first().uuid == sftp.uuid

        def l34Bs = getInterdependentL3NetworksBackupStorages {
            zoneUuid = zone.uuid
            l3NetworkUuids = [l34.uuid]
        } as List

        assert l34Bs.size() == 1
        assert l34Bs.first().uuid == ceph.uuid
    }
}