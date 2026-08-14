package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by shixin on 18-03-26.
 */
class DeleteLoadBalancerWithHttpsCase extends SubCase{
    DatabaseFacade dbf
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
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
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
        dbf = bean(DatabaseFacade.class)
        env.create {
            TestDeleteLoadBalancer()
        }
    }

    void TestDeleteLoadBalancer() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")

        CertificateInventory cerInv1 = createCertificate {
            name = "cer-1"
            certificate = "Test certificate 1"
        }

        CertificateInventory cerInv2 = createCertificate {
            name = "cer-2"
            certificate = "Test certificate 2"
        }

        CertificateInventory cerInv3 = createCertificate {
            name = "cer-3"
            certificate = "Test certificate 3"
        }

        VipInventory vip = createVip {
            name = "test-vip"
            l3NetworkUuid = pubL3.uuid
        }
        LoadBalancerInventory lb1 = createLoadBalancer {
            name = "test-lb-1"
            vipUuid = vip.uuid
        }

        LoadBalancerListenerInventory listener11 = createLoadBalancerListener {
            protocol = "https"
            loadBalancerUuid = lb1.uuid
            loadBalancerPort = 101
            instancePort = 101
            name = "test-listener-1-1"
            certificateUuid = cerInv1.uuid
        }

        LoadBalancerListenerInventory listener12 = createLoadBalancerListener {
            protocol = "https"
            loadBalancerUuid = lb1.uuid
            loadBalancerPort = 102
            instancePort = 102
            name = "test-listener-1-2"
            certificateUuid = cerInv2.uuid
        }

        LoadBalancerInventory lb2 = createLoadBalancer {
            name = "test-lb-2"
            vipUuid = vip.uuid
        }

        LoadBalancerListenerInventory listener21 = createLoadBalancerListener {
            protocol = "https"
            loadBalancerUuid = lb2.uuid
            loadBalancerPort = 201
            instancePort = 201
            name = "test-listener-2-1"
            certificateUuid = cerInv1.uuid
        }

        LoadBalancerListenerInventory listener22 = createLoadBalancerListener {
            protocol = "https"
            loadBalancerUuid = lb2.uuid
            loadBalancerPort = 202
            instancePort = 202
            name = "test-listener-2-2"
            certificateUuid = cerInv2.uuid
        }

        LoadBalancerListenerInventory listener23 = createLoadBalancerListener {
            protocol = "https"
            loadBalancerUuid = lb2.uuid
            loadBalancerPort = 203
            instancePort = 203
            name = "test-listener-2-3"
            certificateUuid = cerInv3.uuid
        }

        addVmNicToLoadBalancer {
            listenerUuid = listener11.uuid
            vmNicUuids = [vm.getVmNics().get(0).getUuid()]
        }
        addVmNicToLoadBalancer {
            listenerUuid = listener12.uuid
            vmNicUuids = [vm.getVmNics().get(0).getUuid()]
        }
        addVmNicToLoadBalancer {
            listenerUuid = listener21.uuid
            vmNicUuids = [vm.getVmNics().get(0).getUuid()]
        }
        addVmNicToLoadBalancer {
            listenerUuid = listener22.uuid
            vmNicUuids = [vm.getVmNics().get(0).getUuid()]
        }
        addVmNicToLoadBalancer {
            listenerUuid = listener23.uuid
            vmNicUuids = [vm.getVmNics().get(0).getUuid()]
        }

        VirtualRouterLoadBalancerBackend.DeleteLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.DELETE_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.DeleteLbCmd.class)
            return rsp
        }

        deleteLoadBalancer {
            uuid = lb2.uuid
        }
        assert cmd.lbs.size() == 3
        for (VirtualRouterLoadBalancerBackend.LbTO to: cmd.lbs) {
            assert to.mode == "https"
            assert to.certificateUuid == cerInv1.uuid || to.certificateUuid == cerInv2.uuid || to.certificateUuid == cerInv3.uuid
        }

        deleteCertificate {
            uuid = cerInv1.uuid
        }

        deleteCertificate {
            uuid = cerInv2.uuid
        }

        deleteCertificate {
            uuid = cerInv3.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }

}
