package org.zstack.storage.volume;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class DeduplicateVolumeGcGlobalProperty {
    @GlobalProperty(name = "deduplicateVolumeGc", defaultValue = "false")
    public static boolean DEDUPLICATEVOLUMEGC;
}