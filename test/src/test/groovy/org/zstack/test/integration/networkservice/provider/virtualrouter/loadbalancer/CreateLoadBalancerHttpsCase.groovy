package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.network.service.virtualrouter.vyos.VyosGlobalConfig
import org.zstack.sdk.CertificateInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.LoadBalancerInventory
import org.zstack.sdk.LoadBalancerListenerCertificateRefInventory
import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by shixin on 18-03-26.
 */
class CreateLoadBalancerHttpsCase extends SubCase{
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
            TestCreateLoadBalancerHttps()
            TestChangeLoadBalancerCertificate()
        }
    }

    void TestCreateLoadBalancerHttps(){
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

        VyosGlobalConfig.ENABLE_HAPROXY_LOG.updateValue(false)

        LoadBalancerListenerInventory listener = createLoadBalancerListener {
            protocol = "https"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 44
            instancePort = 22
            name = "test-listener"
            certificateUuid = cerInv.uuid
        }

        CertificateInventory cer1 = queryCertificate {
            delegate.conditions = ["name=cer-1"]
        }[0]
        assert cer1.getListeners().size() == 1
        def ref = cer1.listeners.get(0) as LoadBalancerListenerCertificateRefInventory
        assert ref.listenerUuid == listener.uuid


        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        addVmNicToLoadBalancer {
            listenerUuid = listener.uuid
            vmNicUuids = [vm.getVmNics().get(0).getUuid()]
        }
        assert cmd.lbs.get(0).certificateUuid == cerInv.uuid
        assert cmd.enableHaproxyLog == false

        cerInv = updateCertificate {
            uuid = cerInv.uuid
            name = "cer-2"
            description = "test-cer"
        }
        assert cerInv.description == "test-cer"
        assert cerInv.name == "cer-2"

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

    void TestChangeLoadBalancerCertificate(){
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

        updateLoadBalancerListener {
            uuid = listener.uuid
            name = "test-1"
        }

        addVmNicToLoadBalancer {
            listenerUuid = listener.uuid
            vmNicUuids = [vm.getVmNics().get(0).getUuid()]
        }

        def cerStr2 = "Test certificate-2" as String
        CertificateInventory cerInv2 = createCertificate {
            name = "cer-1"
            certificate = cerStr2
        }

        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        expect(AssertionError.class) {
            removeCertificateFromLoadBalancerListener {
                listenerUuid = listener.uuid
                certificateUuid = cerInv2.uuid
            }
        }

        removeCertificateFromLoadBalancerListener {
            listenerUuid = listener.uuid
            certificateUuid = cerInv.uuid
        }

        expect(AssertionError.class) {
            removeCertificateFromLoadBalancerListener {
                listenerUuid = listener.uuid
                certificateUuid = cerInv.uuid
            }
        }

        addCertificateToLoadBalancerListener {
            listenerUuid = listener.uuid
            certificateUuid = cerInv2.uuid
        }

        assert cmd.lbs.get(0).certificateUuid == cerInv2.uuid

        deleteCertificate {
            uuid = cerInv.uuid
        }

        deleteCertificate {
            uuid = cerInv2.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }

}
