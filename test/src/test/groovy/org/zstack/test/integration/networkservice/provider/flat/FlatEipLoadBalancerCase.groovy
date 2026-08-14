package org.zstack.test.integration.networkservice.provider.flat

import org.zstack.core.db.Q
import org.zstack.header.network.l3.L3NetworkVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.vip.VipPeerL3NetworkRefVO
import org.zstack.network.service.vip.VipPeerL3NetworkRefVO_
import org.zstack.network.service.virtualrouter.eip.VirtualRouterEipRefVO
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.VirtualRouterOfferingInventory
import org.zstack.sdk.VirtualRouterVmInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * @author: zhanyong.miao
 * @date: 2020-04-16
 * */
class FlatEipLoadBalancerCase extends SubCase {
    EnvSpec env

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

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
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
                    attachL2Network("l2-1")
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

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }


                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL2"

                        ip {
                            startIp = "11.168.200.10"
                            endIp = "11.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.200.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                lb {
                    name = "lb"
                    useVip("pubL3")

                    listener {
                        name = "lb-listener"
                        protocol = "tcp"
                        loadBalancerPort = 22
                        instancePort = 22
                    }

                }

                eip {
                    name = "eip"
                    useVip("pubL2")
                    useVmNic("vm", "l3")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

        }
    }

    @Override
    void test() {
        env.create {
            /* case
    *  lb pub-flat: detach nics
    *  eip public-flat attach nics
    *  lb delete vrouter, detach nic and attach nic
    *  check eip ref & vip table
    * */
            testCreateFlatEipLbCase()
        }
    }



    private String attachOffering2L3(String offerUuid, String L3Uuid) {
        SystemTagInventory tag = createSystemTag {
            tag = "virtualRouterOffering::" + offerUuid
            resourceType = L3NetworkVO.simpleName
            resourceUuid = L3Uuid
        }
        return tag.uuid
    }

    private void detachOfferingFromL3(String resourceUuid) {
        deleteTag {
            uuid = resourceUuid
        }
    }

    private void testCreateFlatEipLbCase() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def pubOffer = env.inventoryByName("vro") as VirtualRouterOfferingInventory
        def listener = env.inventoryByName("lb-listener") as LoadBalancerListenerInventory

        String resource = attachOffering2L3(pubOffer.uuid, l3.uuid)

        def result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        } as List<VmNicInventory>
        assert result.size() == 1
        assert result.get(0).l3NetworkUuid == l3.uuid

        String nicUuid = result.get(0).uuid
        addVmNicToLoadBalancer {
            vmNicUuids = [nicUuid]
            listenerUuid = listener.uuid
        }
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.l3NetworkUuid, l3.uuid).count() == 2
        assert Q.New(VirtualRouterEipRefVO.class).count() == 0

        VirtualRouterVmInventory vr = queryVirtualRouterVm {}[0]
        destroyVmInstance {
            uuid = vr.uuid
        }

        removeVmNicFromLoadBalancer {
            vmNicUuids = [nicUuid]
            listenerUuid = listener.uuid
        }

        addVmNicToLoadBalancer {
            vmNicUuids = [nicUuid]
            listenerUuid = listener.uuid
        }
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.l3NetworkUuid, l3.uuid).count() == 2
        assert Q.New(VirtualRouterEipRefVO.class).count() == 0

        detachOfferingFromL3(resource)

    }


    @Override
    void clean() {
        env.delete()
    }
}
