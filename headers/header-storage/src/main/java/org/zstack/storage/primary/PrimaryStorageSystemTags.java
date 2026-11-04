package org.zstack.storage.primary;

import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 */
@TagDefinition
public class PrimaryStorageSystemTags {
    public static SystemTag CAPABILITY_SNAPSHOT = new SystemTag("capability:snapshot", PrimaryStorageVO.class);

    public static final String PRIMARY_STORAGE_ALLOCATOR_UUID_TAG_TOKEN = "uuid";
    public static SystemTag PRIMARY_STORAGE_ALLOCATOR_UUID_TAG = new SystemTag(String.format("primaryStorage::allocator::uuid::{%s}", PRIMARY_STORAGE_ALLOCATOR_UUID_TAG_TOKEN), PrimaryStorageVO.class);
    public static SystemTag PRIMARY_STORAGE_ALLOCATOR_USERTAG_TAG = new SystemTag("primaryStorage::allocator::userTag::{tag}", PrimaryStorageVO.class);
    public static SystemTag PRIMARY_STORAGE_ALLOCATOR_USERTAG_TAG_MANDATORY = new SystemTag("primaryStorage::allocator::userTag::{tag}::required", PrimaryStorageVO.class);

    public static final String CAPABILITY_HYPERVISOR_SNAPSHOT_TOKEN= "hypervisorType";
    public static SystemTag CAPABILITY_HYPERVISOR_SNAPSHOT = new SystemTag(String.format("capability::snapshot::hypervisor::{%s}", CAPABILITY_HYPERVISOR_SNAPSHOT_TOKEN), PrimaryStorageVO.class);

    public static final String PRIMARY_STORAGE_GATEWAY_TOKEN = "storageGateway";
    public static SystemTag PRIMARY_STORAGE_GATEWAY = new SystemTag(String.format("primaryStorage::gateway::cidr::{%s}", PRIMARY_STORAGE_GATEWAY_TOKEN), PrimaryStorageVO.class);

    public static final String PRIMARY_STORAGE_VENDOR_TOKEN = "vendor";
    public static SystemTag PRIMARY_STORAGE_VENDOR =  new SystemTag(String.format("primaryStorage::vendor::{%s}", PRIMARY_STORAGE_VENDOR_TOKEN), PrimaryStorageVO.class);

    public static String PRIMARY_STORAGE_MIGRATE_NETWORK_CIDR_TOKEN = "migrateCidr";
    public static SystemTag PRIMARY_STORAGE_MIGRATE_NETWORK_CIDR = new SystemTag(String.format(
            "primaryStorage::migrate::network::cidr::{%s}", PRIMARY_STORAGE_MIGRATE_NETWORK_CIDR_TOKEN), PrimaryStorageVO.class);
}