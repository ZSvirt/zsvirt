package org.zstack.test.integration.networkservice.provider.flat.eip

import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.AttachEipAction
import org.zstack.sdk.EipInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.UsedIpInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.network.IPv6Constants

/**
 * Created by liangbo.zhou on 17-6-24.
 */
class VipOverlapWithVmIpCase extends SubCase{

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
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "12.168.100.10"
                            endIp = "12.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "12.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3-1"

                        ip {
                            startIp = "12.168.101.10"
                            endIp = "12.168.101.100"
                            netmask = "255.255.255.0"
                            gateway = "12.168.101.1"
                        }
                    }

                    l3Network {
                        name = "pubL3-2"

                        ip {
                            startIp = "12.168.102.10"
                            endIp = "12.168.102.100"
                            netmask = "255.255.255.0"
                            gateway = "12.168.102.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip"
                    useVip("pubL3")
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
            testVipOverlapWithVmIp()
            testAttach2EipsToVmNicOfFlatNetwork()
        }
    }
    
    void testVipOverlapWithVmIp(){
        def eip = env.inventoryByName("eip") as EipInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory

        try {
            AttachEipAction action = new AttachEipAction()
            action.eipUuid = eip.uuid
            action.vmNicUuid = vm.getVmNics().get(0).getUuid()
            action.sessionId = loginAsAdmin()
            action.call()
        }catch (ApiMessageInterceptionException e){
            //there will throw the other exception "IllegalArgumentException" before fix
            //add "http://dev.zstack.io:9080/zstackio/zstack/merge_requests/1112" to obersve it
        }
    }

    void testAttach2EipsToVmNicOfFlatNetwork(){
        def publ3_1 = env.inventoryByName("pubL3-1") as L3NetworkInventory
        def publ3_2 = env.inventoryByName("pubL3-2") as L3NetworkInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def image = env.inventoryByName("image") as ImageInventory
        def offerging = env.inventoryByName("instanceOffering") as InstanceOfferingInventory

        VipInventory vip1 = createVip {
            name = "vip1"
            l3NetworkUuid = publ3_1.uuid
        }
        EipInventory eip1 = createEip{
            name = "eip1"
            vipUuid = vip1.uuid
        }

        VipInventory vip2 = createVip {
            name = "vip2"
            l3NetworkUuid = publ3_2.uuid
        }
        EipInventory eip2 = createEip{
            name = "eip2"
            vipUuid = vip2.uuid
        }

        List<VmNicInventory> vmnics = getEipAttachableVmNics {
            eipUuid = eip1.uuid
        }
        assert vmnics.size() == 1
        vmnics = getEipAttachableVmNics {
            eipUuid = eip2.uuid
        }
        assert vmnics.size() == 1

        VmInstanceInventory vm2 = createVmInstance {
            name = "vm2"
            instanceOfferingUuid = offerging.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        vmnics = getEipAttachableVmNics {
            eipUuid = eip1.uuid
        }
        assert vmnics.size() == 2
        vmnics = getEipAttachableVmNics {
            eipUuid = eip2.uuid
        }
        assert vmnics.size() == 2

        attachEip {
            eipUuid = eip1.uuid
            vmNicUuid = vm2.vmNics.get(0).uuid
        }

        /* after eip1 attached to vm2, vm2 will not be a candidate for eip2 */
        vmnics = getEipAttachableVmNics {
            eipUuid = eip2.uuid
        }
        assert vmnics.size() == 1

        /* bound eip2 to vm2 which has already has eip will assert fail */
        expect(AssertionError.class) {
            attachEip {
                eipUuid = eip2.uuid
                vmNicUuid = vm2.vmNics.get(0).uuid
            }
        }

        /* add ipv6 range to public1 */
        IpRangeInventory ipr6 = addIpv6RangeByNetworkCidr {
            name = "ip61"
            l3NetworkUuid = publ3_1.uuid
            networkCidr = "2020:09:08:1::/64"
            addressMode = IPv6Constants.Stateful_DHCP
        }

        VipInventory vip6 = createVip {
            name = "vip6"
            l3NetworkUuid = publ3_1.uuid
            ipRangeUuid = ipr6.uuid
        }
        EipInventory eip6 = createEip{
            name = "eip6"
            vipUuid = vip6.uuid
        }

        /* add ipv6 range to l3 */
        IpRangeInventory ipr62 = addIpv6RangeByNetworkCidr {
            name = "ip62"
            l3NetworkUuid = l3.uuid
            networkCidr = "2020:09:08:2::/64"
            addressMode = IPv6Constants.Stateful_DHCP
        }
        /* reboot vm will allocate a ipv6 address for vmnic */
        vm2 = rebootVmInstance {
            uuid = vm2.uuid
        }
        UsedIpInventory ip6 = null
        for (UsedIpInventory ip : vm2.vmNics.get(0).usedIps) {
            if (ip.ipVersion == IPv6Constants.IPv6) {
                ip6 = ip
            }
        }
        assert ip6 != null

        vmnics = getEipAttachableVmNics {
            eipUuid = eip6.uuid
        }
        assert vmnics.size() == 1

        attachEip {
            eipUuid = eip6.uuid
            vmNicUuid = vm2.vmNics.get(0).uuid
            usedIpUuid = ip6.uuid
        }

        /* add ipv6 range to public1 */
        IpRangeInventory ipr63 = addIpv6RangeByNetworkCidr {
            name = "ip63"
            l3NetworkUuid = publ3_2.uuid
            networkCidr = "2020:09:08:3::/64"
            addressMode = IPv6Constants.Stateful_DHCP
        }

        /* vm2 nic can only attach 1 eip, getEipAttachableVmNics will return empty */
        VipInventory vip63 = createVip {
            name = "vip63"
            l3NetworkUuid = publ3_2.uuid
            ipRangeUuid = ipr63.uuid
        }
        EipInventory eip63 = createEip{
            name = "eip63"
            vipUuid = vip63.uuid
        }
        vmnics = getEipAttachableVmNics {
            eipUuid = eip63.uuid
        }
        assert vmnics.size() == 0
    }

    @Override
    void clean() {
        env.delete()
    }
}
