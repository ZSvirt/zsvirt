package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalanceQuotaConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by shixin on 18-05-03.
 */
class LoadBalancerQuotaCase extends SubCase{
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
            }
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            TestLoadBalancerQuota()
            TestLoadBalancerListenerQuota()
        }
    }

    void TestLoadBalancerQuota(){
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        shareResource {
            resourceUuids = [pubL3.uuid]
            toPublic = true
        }

        AccountInventory account = createAccount {
            name = "test"
            password = "password"
            sessionId = adminSession()
        }
        attachPredefineRoles(account.uuid, "networks", "vip", "load-balancer")

        updateQuota {
            identityUuid = account.uuid
            name = LoadBalanceQuotaConstant.LOAD_BALANCER_NUM
            value = 5
        }

        SessionInventory sessionInventory1 = logInByAccount {
            accountName = "test"
            password = "password"
        }

        VipInventory vip = createVip {
            name = "test-vip"
            l3NetworkUuid = pubL3.uuid
            sessionId = sessionInventory1.uuid
        }

        for (int i = 0; i < 5; i++) {
            createLoadBalancer {
                name = "test-lb-" + i
                vipUuid = vip.uuid
                sessionId = sessionInventory1.uuid
            }
        }

        expect (AssertionError.class){
            createLoadBalancer {
                name = "test-lb-6"
                vipUuid = vip.uuid
                sessionId = sessionInventory1.uuid
            }
        }
    }

    void TestLoadBalancerListenerQuota(){
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        AccountInventory account = createAccount {
            name = "test-1"
            password = "password"
            sessionId = adminSession()
        }
        attachPredefineRoles(account.uuid, "networks", "vip", "load-balancer")

        updateQuota {
            identityUuid = account.uuid
            name = LoadBalanceQuotaConstant.LOAD_BALANCER_LISTENER_NUM
            value = 5
            sessionId = adminSession()
        }

        SessionInventory sessionInventory1 = logInByAccount {
            accountName = "test-1"
            password = "password"
        }

        VipInventory vip = createVip {
            name = "test-vip"
            l3NetworkUuid = pubL3.uuid
            sessionId = sessionInventory1.uuid
        }

        LoadBalancerInventory lbInv = createLoadBalancer {
            name = "test-lb"
            vipUuid = vip.uuid
            sessionId = sessionInventory1.uuid
        }

        for (int i = 0; i < 5; i++) {
            createLoadBalancerListener {
                protocol = "http"
                loadBalancerUuid = lbInv.uuid
                loadBalancerPort = 100 + i
                instancePort = 100 + i
                name = "test-listener" + i
                sessionId = sessionInventory1.uuid
            }
        }

        expect (AssertionError.class){
            createLoadBalancerListener {
                protocol = "http"
                loadBalancerUuid = lbInv.uuid
                loadBalancerPort = 106
                instancePort = 106
                name = "test-listener-6"
                sessionId = sessionInventory1.uuid
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }

}
