package org.zstack.storage.primary;


import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class FstabDeviceToUuidUpdaterGlobalProperty {
    @GlobalProperty(name = "fstabDeviceToUuidUpdater", defaultValue = "false")
    public static boolean FSTAB_DEVICE_TO_UUID_UPDATER;
}