package org.zstack.storage.encrypt;

final class ZbsVolumeEncryptionConstants {
    static final String KVM_HOST_LUKS_CLONE_PATH = "/zbs/primarystorage/kvmhost/luksclone";
    static final String KVM_HOST_LUKS_CREATE_EMPTY_PATH = "/zbs/primarystorage/kvmhost/lukscreateempty";
    static final String KVM_HOST_LUKS_ENCRYPT_IN_PLACE_PATH = "/zbs/primarystorage/kvmhost/encryptinplace";
    static final String KVM_HOST_LUKS_CONVERT_PATH = "/zbs/primarystorage/kvmhost/luksconvert";
    static final String KVM_HOST_LUKS_RESIZE_PATH = "/zbs/primarystorage/kvmhost/luksresize";

    private ZbsVolumeEncryptionConstants() {
    }
}
