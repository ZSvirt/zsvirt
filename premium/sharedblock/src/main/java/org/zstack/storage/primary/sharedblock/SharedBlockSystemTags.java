package org.zstack.storage.primary.sharedblock;

import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

@TagDefinition
public class SharedBlockSystemTags {
    public static String SHARED_BLOCK_FORCE_WIPE_TOKEN = "forceWipe";
    public static PatternedSystemTag SHARED_BLOCK_FORCE_WIPE = new PatternedSystemTag(
            String.format("%s", SHARED_BLOCK_FORCE_WIPE_TOKEN), PrimaryStorageVO.class);

    public static SystemTag SHARED_BLOCK_TAKEOVER = new SystemTag("takeover", PrimaryStorageVO.class);

    public static String SHARED_BLOCK_REMOVE_FORCE_WIPE_TOKEN = "removeForceWipe";
    public static PatternedSystemTag SHARED_BLOCK_REMOVE_FORCE_WIPE = new PatternedSystemTag(
            String.format("%s", SHARED_BLOCK_REMOVE_FORCE_WIPE_TOKEN), PrimaryStorageVO.class);

    public static String SHARED_BLOCK_DISABLED_HOST_TOKEN = "sharedBlockDisabledHost";
    public static PatternedSystemTag SHARED_BLOCK_DISABLED_HOST = new PatternedSystemTag(
            String.format("%s", SHARED_BLOCK_DISABLED_HOST_TOKEN), HostVO.class);

    public static String SHARED_BLOCK_NOT_INITIALIZED_TOKEN = "notInitialized";
    public static PatternedSystemTag SHARED_BLOCK_NOT_INITIALIZED = new PatternedSystemTag(
            String.format("%s", SHARED_BLOCK_NOT_INITIALIZED_TOKEN), PrimaryStorageVO.class);

    public static String THIN_PROVISIONING_INITIALIZE_SIZE_TAG_TOKEN = "thinProvisioningInitializeSize";
    public static PatternedSystemTag THIN_PROVISIONING_INITIALIZE_SIZE_TAG = PatternedSystemTag.makeEphemeralTag(
            String.format("%s::{%s}", THIN_PROVISIONING_INITIALIZE_SIZE_TAG_TOKEN, THIN_PROVISIONING_INITIALIZE_SIZE_TAG_TOKEN));
}
