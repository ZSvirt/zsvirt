package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.agent.AgentResponse
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
class CreateLoadBalancerHttpsFailCase extends SubCase{
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
            TestRefreshLbFail()
            TestCeateCertificateFail()
        }
    }

    void TestRefreshLbFail(){
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")

        def cerStr = "Test certificate" as String
        CertificateInventory cerInv = createCertificate {
            name = "cer-1"
            certificate = cerStr
        }

        VipInventory vip = createVip {
            name = "test-vip"
            l3NetworkUuid = pubL3.uuid
        }
        LoadBalancerInventory lb = createLoadBalancer {
            name = "test-lb"
            vipUuid = vip.uuid
        }

        LoadBalancerListenerInventory listener = createLoadBalancerListener {
            protocol = "https"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 44
            instancePort = 22
            name = "test-listener"
            certificateUuid = cerInv.uuid
        }

        env.simulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) {
            VirtualRouterLoadBalancerBackend.RefreshLbRsp rsp = new VirtualRouterLoadBalancerBackend.RefreshLbRsp()
            rsp.setError("No such file")
            rsp.setSuccess(false)
            return rsp
        }

        VirtualRouterLoadBalancerBackend.CertificateCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.DELETE_CERTIFICATE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.CertificateCmd)
            return rsp
        }

        expect (AssertionError.class) {
            addVmNicToLoadBalancer {
                listenerUuid = listener.uuid
                vmNicUuids = [vm.getVmNics().get(0).getUuid()]
            }
        }
        assert cmd.uuid == cerInv.uuid

        env.simulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) {
            VirtualRouterLoadBalancerBackend.RefreshLbRsp rsp = new VirtualRouterLoadBalancerBackend.RefreshLbRsp()
            return rsp
        }

        addVmNicToLoadBalancer {
            listenerUuid = listener.uuid
            vmNicUuids = [vm.getVmNics().get(0).getUuid()]
        }

        deleteLoadBalancer {
            uuid = lb.uuid
        }

        deleteVip {
            uuid = vip.uuid
        }

        deleteCertificate {
            uuid = cerInv.uuid
        }
    }

    void TestCeateCertificateFail(){
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")

        def cerStr = "Test certificate" as String
        CertificateInventory cerInv = createCertificate {
            name = "cer-1"
            certificate = cerStr
        }

        VipInventory vip = createVip {
            name = "test-vip"
            l3NetworkUuid = pubL3.uuid
        }
        LoadBalancerInventory lb = createLoadBalancer {
            name = "test-lb"
            vipUuid = vip.uuid
        }

        LoadBalancerListenerInventory listener = createLoadBalancerListener {
            protocol = "https"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 44
            instancePort = 22
            name = "test-listener"
            certificateUuid = cerInv.uuid
        }

        env.simulator(VirtualRouterLoadBalancerBackend.CREATE_CERTIFICATE_PATH) {
            VirtualRouterLoadBalancerBackend.CertificateRsp rsp = new VirtualRouterLoadBalancerBackend.CertificateRsp()
            rsp.setError("No such file")
            rsp.setSuccess(false)
            return rsp
        }

        VirtualRouterLoadBalancerBackend.CertificateCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.DELETE_CERTIFICATE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.CertificateCmd)
            return rsp
        }

        expect (AssertionError.class) {
            addVmNicToLoadBalancer {
                listenerUuid = listener.uuid
                vmNicUuids = [vm.getVmNics().get(0).getUuid()]
            }
        }
        assert cmd.uuid == cerInv.uuid

        deleteLoadBalancer {
            uuid = lb.uuid
        }

        deleteVip {
            uuid = vip.uuid
        }

        deleteCertificate {
            uuid = cerInv.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }

}
