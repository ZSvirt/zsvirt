package org.zstack.storage.encrypt;

class ZbsVolumeEncryptionMaterial {
    final String hostUuid;
    final String encryptedDek;

    ZbsVolumeEncryptionMaterial(String hostUuid, String encryptedDek) {
        this.hostUuid = hostUuid;
        this.encryptedDek = encryptedDek;
    }
}
