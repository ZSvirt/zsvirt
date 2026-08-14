package org.zstack.header.volume;

import org.zstack.header.storage.primary.PrimaryStorageDetachStruct;
import java.util.List;

public interface GetVmUuidFromShareableVolumeExtensionPoint {
    List<String> getVmUuidFromShareableVolumeByPrimaryStorage(PrimaryStorageDetachStruct s);
}
