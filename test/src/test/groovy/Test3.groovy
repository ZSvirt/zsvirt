import org.zstack.header.vm.APIStopVmInstanceMsg
import org.zstack.testlib.DiskOfferingSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test
import org.zstack.testlib.VmSpec
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/2/12.
 * 1. 2
 * 3
 */
class Test3 extends Test {
    boolean success
    DiskOfferingSpec diskOfferingSpec
    EnvSpec envSpec

    @Override
    void setup() {
        spring {
            nfsPrimaryStorage()
            kvm()
            vyos()
            eip()
            sftpBackupStorage()
            portForwarding()
            lb()
            ceph()
            smp()
            localStorage()
            securityGroup()
        }
    }

    @Override
    void environment() {
        envSpec = env {
            account {
                name = "xin"
                password = "password"
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(10)
                useAccount("xin")
            }

            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
                useAccount("xin")
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
                    useAccount("xin")
                }

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
                    useAccount("xin")
                }
            }

            cephBackupStorage {
                name = "ceph-bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost:23", "root:password@127.0.0.1:23"]

                image {
                    name = "image2"
                    url  = "http://zstack.org/download/test.qcow2"
                    useAccount("xin")
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
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachPrimaryStorage("nfs", "ceph-pri", "local", "smp")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }

                cephPrimaryStorage {
                    name = "ceph-pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777", "root:password@127.0.0.1/?monPort=7777"]
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                smpPrimaryStorage {
                    name = "smp"
                    url = "/smp"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"
                        useAccount("xin")

                        service {
                            provider = "vrouter"
                            types = ["DHCP", "DNS", "Eip", "SNAT", "PortForwarding", "LoadBalancer", "IPsec"]
                        }

                        service {
                            provider = "SecurityGroup"
                            types = ["SecurityGroup"]
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
                        useAccount("xin")

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
                    useAccount("xin")
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip"
                    useVip("pubL3")
                    useVmNic("vm", "l3")
                    useAccount("xin")
                }

                portForwarding {
                    name = "pf"
                    vipPortStart = 22
                    privatePortStart = 22
                    protocolType = "TCP"
                    useVip("pubL3")
                    useVmNic("vm", "l3")
                    useAccount("xin")
                }

                lb {
                    name = "lb"
                    useVip("pubL3")
                    useAccount("xin")

                    listener {
                        protocol = "tcp"
                        loadBalancerPort = 22
                        instancePort = 22
                        useVmNic("vm", "l3")
                    }
                }

                securityGroup {
                    name = "sg"
                    attachL3Network("l3")
                    useAccount("xin")

                    rule {
                        type = "Ingress"
                        startPort = 100
                        endPort = 110
                        protocol = "TCP"
                    }

                    useVmNic("vm", "l3")
                }
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useAccount("xin")
            }
        }

        envSpec.create()
    }

    @Override
    void test() {
        VmSpec vm = envSpec.specByName("vm")

        envSpec.message(APIStopVmInstanceMsg.class) {
            throw new Exception("on purpose")
        }

        try {
            stopVmInstance {
                uuid = vm.inventory.uuid
            }
        } catch (Throwable e){
            logger.warn(e.message, e)
        }

        envSpec.cleanSimulatorAndMessageHandlers()

        stopVmInstance {
            uuid = vm.inventory.uuid
        }
    }
}
