package org.zstack.test.integration.storage.backup.addon

import org.zstack.header.core.Completion
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.image.ImageInventory
import org.zstack.header.storage.addon.ImageDescriptor
import org.zstack.header.storage.addon.StorageCapacity
import org.zstack.header.storage.addon.StorageHealthy
import org.zstack.header.storage.addon.backup.BackupStorageController
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.sdk.ExternalBackupStorageInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class ExternalBackupStorageCase extends SubCase {

    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
        spring {
            include("ExternalBackupStorage.xml")
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

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
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

                    attachL2Network("l2")
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

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
            }
        }
    }

    @Override
    void test() {
        env.create {
            //testAddExternalBackupStorage()
        }
    }

    /*
    void testAddExternalBackupStorage() {
        PluginRegistry pluginRegistry = bean(PluginRegistry.class)
        pluginRegistry.defineDynamicExtension(BackupStorageController.class,
                new BackupStorageController() {
                    @Override
                    String getIdentity() {
                        return "zbd"
                    }

                    @Override
                    void connect(boolean newAdded, String url, Completion comp) {

                    }

                    @Override
                    void reportCapacity(ReturnValueCompletion<StorageCapacity> comp) {

                    }

                    @Override
                    void reportHealthy(ReturnValueCompletion<StorageHealthy> comp) {

                    }

                    @Override
                    void importImage(ImageInventory image, ReturnValueCompletion<ImageDescriptor> comp) {

                    }

                    @Override
                    void cancelImport(ImageInventory image, Completion comp) {

                    }
                })

        def bs = addExternalBackupStorage {
            name = "mybs"
            identity = "zbd"
            url = "zbd:pool/vol:/etc/foo.conf"
        } as ExternalBackupStorageInventory

        assert bs.name == "mybs"
        assert bs.identity == "zbd"

        deleteBackupStorage {
            uuid = bs.uuid
        }
    }
     */
}
