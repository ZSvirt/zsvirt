package org.zstack.test.unittest.storage.encrypt

import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.keyprovider.EncryptedResourceKeyManager
import org.zstack.header.volume.InstantiateVolumeMsg
import org.zstack.header.volume.VolumeVO
import org.zstack.storage.encrypt.VolumeEncryptedInitialExtension
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend
import org.zstack.storage.encrypt.VolumeEncryptedSecretHelper
import org.zstack.storage.encrypt.VolumeSnapshotEncryptionHelper

import java.lang.reflect.Field
import java.nio.charset.StandardCharsets

class VolumeEncryptedInitialExtensionTest {
    private VolumeEncryptedInitialExtension extension
    private DatabaseFacade dbf
    private VolumeEncryptedResourceKeyBackend keyBackend
    private VolumeEncryptedSecretHelper secretHelper
    private VolumeSnapshotEncryptionHelper snapshotEncryptionHelper

    @Before
    void setUp() {
        extension = new VolumeEncryptedInitialExtension()
        dbf = Mockito.mock(DatabaseFacade.class)
        keyBackend = Mockito.mock(VolumeEncryptedResourceKeyBackend.class)
        secretHelper = Mockito.mock(VolumeEncryptedSecretHelper.class)
        snapshotEncryptionHelper = Mockito.mock(VolumeSnapshotEncryptionHelper.class)

        setField("dbf", dbf)
        setField("volumeEncryptedResourceKeyBackend", keyBackend)
        setField("secretHelper", secretHelper)
        setField("snapshotEncryptionHelper", snapshotEncryptionHelper)
    }

    @Test
    void testPreInstantiateMaterializesEncryptedVolumeWithoutHostUuid() {
        String volumeUuid = "volume-uuid"
        String keyProviderUuid = "key-provider-uuid"
        VolumeVO volume = new VolumeVO()
        volume.uuid = volumeUuid
        volume.encrypted = true

        EncryptedResourceKeyManager.ResourceKeyResult keyResult =
                new EncryptedResourceKeyManager.ResourceKeyResult()
        keyResult.dekBase64 = Base64.encoder.encodeToString(
                "0123456789abcdef".getBytes(StandardCharsets.UTF_8))

        Mockito.when(dbf.findByUuid(volumeUuid, VolumeVO.class)).thenReturn(volume)
        Mockito.when(keyBackend.findKeyProviderUuidByVolume(volumeUuid)).thenReturn(null)
        Mockito.when(keyBackend.defaultKeyProviderUuid()).thenReturn(keyProviderUuid)
        Mockito.when(secretHelper.materializeDek(volumeUuid, keyProviderUuid)).thenReturn(keyResult)

        InstantiateVolumeMsg msg = new InstantiateVolumeMsg()
        msg.volumeUuid = volumeUuid

        extension.preInstantiateVolume(msg)

        Mockito.verify(snapshotEncryptionHelper).inheritFromTemporarySnapshotImageKeyIfPossible(volume)
        Mockito.verify(keyBackend).attachKeyProviderToVolume(volumeUuid, keyProviderUuid)
        Mockito.verify(secretHelper).materializeDek(volumeUuid, keyProviderUuid)
    }

    private void setField(String name, Object value) {
        Field field = VolumeEncryptedInitialExtension.class.getDeclaredField(name)
        field.accessible = true
        field.set(extension, value)
    }
}
