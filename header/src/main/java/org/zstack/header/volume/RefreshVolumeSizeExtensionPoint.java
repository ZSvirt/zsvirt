package org.zstack.header.volume;

import java.util.List;

public interface RefreshVolumeSizeExtensionPoint {
    List<String> getNeedRefreshVolumeSizeVolume();
}
