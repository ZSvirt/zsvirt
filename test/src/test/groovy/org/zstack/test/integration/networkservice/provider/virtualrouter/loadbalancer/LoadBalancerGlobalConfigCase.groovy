package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerSystemTags
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.LoadBalancerInventory
import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.network.service.lb.LoadBalancerGlobalConfig
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.utils.TagUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static java.util.Arrays.asList


class LoadBalancerGlobalConfigCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
        useSpring(NetworkServiceProviderTest.springSpec)
        spring {
            lb()
            portForwarding()
        }
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
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
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
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
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
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.SNAT.toString(),
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
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testHttpMode()
            testDefaultHttpMode()
        }
    }


    void testHttpMode(){
        expect(AssertionError.class) {
            updateGlobalConfig {
                category = LoadBalancerGlobalConfig.CATEGORY
                name = LoadBalancerGlobalConfig.HTTP_MODE.name
                value = "http-server"
            }
        }

        updateGlobalConfig {
            category = LoadBalancerGlobalConfig.CATEGORY
            name = LoadBalancerGlobalConfig.HTTP_MODE.name
            value = "http-server-close"
        }
    }

    void testDefaultHttpMode() {
        def pub = env.inventoryByName("pubL3") as L3NetworkInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory

        VipInventory vip = createVip {
            name = "vip-1"
            l3NetworkUuid = pub.uuid
        }

        LoadBalancerInventory lb = createLoadBalancer {
            name = "test-lb-1"
            vipUuid = vip.getUuid()
        }

        LoadBalancerListenerInventory lbl = createLoadBalancerListener {
            protocol = "tcp"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 44
            instancePort = 22
            name = "test-listener"
        }

        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        addVmNicToLoadBalancer {
            listenerUuid = lbl.uuid
            vmNicUuids = asList(vm.getVmNics().get(0).getUuid())
        }

        VirtualRouterLoadBalancerBackend.LbTO to = null
        def http_mode = null
        retryInSecs {
            to = cmd.lbs.get(0)
            for (String tag : to.getParameters()) {
                if (LoadBalancerSystemTags.HTTP_MODE.isMatch(tag)) {
                    Map<String, String> token = TagUtils.parse(LoadBalancerSystemTags.HTTP_MODE.getTagFormat(), tag)
                    http_mode = token.get(LoadBalancerSystemTags.HTTP_MODE_TOKEN)
                }
            }
            assert http_mode == null
        }

        cmd = null
        LoadBalancerListenerInventory http_lbl = createLoadBalancerListener {
            protocol = "http"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 33
            instancePort = 33
            name = "test-listener"
        }
        addVmNicToLoadBalancer {
            listenerUuid = http_lbl.uuid
            vmNicUuids = asList(vm.getVmNics().get(0).getUuid())
        }
        retryInSecs {
            cmd.lbs.each { VirtualRouterLoadBalancerBackend.LbTO lbTO ->
                if (lbTO.getListenerUuid().equals(http_lbl.uuid)) {
                    to = lbTO
                }
            }
            for (String tag : to.getParameters()) {
                if (LoadBalancerSystemTags.HTTP_MODE.isMatch(tag)) {
                    Map<String, String> token = TagUtils.parse(LoadBalancerSystemTags.HTTP_MODE.getTagFormat(), tag)
                    http_mode = token.get(LoadBalancerSystemTags.HTTP_MODE_TOKEN)
                }
            }
            assert http_mode == "http-server-close"
        }

        cmd = null
        LoadBalancerListenerInventory https_lbl = createLoadBalancerListener {
            protocol = "https"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 46
            instancePort = 46
            name = "test-listener"
        }
        addVmNicToLoadBalancer {
            listenerUuid = https_lbl.uuid
            vmNicUuids = asList(vm.getVmNics().get(0).getUuid())
        }
        retryInSecs {
            cmd.lbs.each { VirtualRouterLoadBalancerBackend.LbTO lbTO ->
                if (lbTO.getListenerUuid().equals(https_lbl.uuid)) {
                    to = lbTO
                }
            }
            for (String tag : to.getParameters()) {
                if (LoadBalancerSystemTags.HTTP_MODE.isMatch(tag)) {
                    Map<String, String> token = TagUtils.parse(LoadBalancerSystemTags.HTTP_MODE.getTagFormat(), tag)
                    http_mode = token.get(LoadBalancerSystemTags.HTTP_MODE_TOKEN)
                }
            }
            assert http_mode == "http-server-close"
        }
    }
}
