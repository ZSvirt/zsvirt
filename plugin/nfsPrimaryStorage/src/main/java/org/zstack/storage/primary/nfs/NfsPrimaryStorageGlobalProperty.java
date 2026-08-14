package org.zstack.storage.primary.nfs;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class NfsPrimaryStorageGlobalProperty {
    @GlobalProperty(name="NfsPrimaryStorage.bitsDeletion.gc.interval", defaultValue = "300")
    public static int BITS_DELETION_GC_INTERVAL;
}
