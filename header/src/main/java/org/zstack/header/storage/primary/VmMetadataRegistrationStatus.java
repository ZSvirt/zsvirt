package org.zstack.header.storage.primary;

/** Registration relationship at scan time; the registration API revalidates the request. */
public enum VmMetadataRegistrationStatus {
    UNREGISTERED,
    REGISTERED,
    UUID_CONFLICT
}
