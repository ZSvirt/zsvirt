package org.zstack.testlib

import org.zstack.sdk.ImageInventory
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/2/15.
 */
class ImageSpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    @SpecParam
    Long size = SizeUnit.GIGABYTE.toByte(10)
    @SpecParam
    Long actualSize = SizeUnit.GIGABYTE.toByte(1)
    @SpecParam
    String md5sum
    @SpecParam(required = true)
    String url
    @SpecParam
    String mediaType = "RootVolumeTemplate"
    @SpecParam
    String platform = "Linux"
    @SpecParam
    String format = "qcow2"
    @SpecParam
    String guestOsType = "CentOS"
    @SpecParam
    boolean system = false
    @SpecParam
    String architecture = "x86_64"
    @SpecParam
    boolean virtio = false

    ImageInventory inventory

    ImageSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addImage {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.guestOsType = guestOsType
            delegate.format = format
            delegate.mediaType = mediaType
            delegate.platform = platform
            delegate.url = url
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.sessionId = sessionId
            delegate.system = system
            delegate.architecture = architecture
            delegate.virtio = virtio
            delegate.backupStorageUuids = [(parent as BackupStorageSpec).inventory.uuid]
        }

        postCreate {
            inventory = queryImage {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteImage {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
