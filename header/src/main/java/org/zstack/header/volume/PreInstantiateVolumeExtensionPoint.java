package org.zstack.header.volume;

/**
 * Created by miao on 2/6/17.
 */
public interface PreInstantiateVolumeExtensionPoint {
    void preInstantiateVolume(InstantiateVolumeMsg msg);
}
