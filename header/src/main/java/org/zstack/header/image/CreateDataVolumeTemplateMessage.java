package org.zstack.header.image;

/**
 * Created by MaJin on 2021/3/17.
 */
public interface CreateDataVolumeTemplateMessage extends AddImageMessage {
    @Override
    default String getMediaType() {
        return ImageConstant.ImageMediaType.DataVolumeTemplate.toString();
    }

    @Override
    default String getGuestOsType() {
        return null;
    }

    @Override
    default String getArchitecture() {
        return null;
    }

    @Override
    default String getPlatform() {
        return null;
    }

    @Override
    default boolean isSystem() {
        return false;
    }

    @Override
    default void setArchitecture(String architecture) {}

    @Override
    default void setGuestOsType(String guestOsType) {}

    @Override
    default void setPlatform(String platform) {}

    @Override
    default void setVirtio(boolean virtio) {}

    @Override
    default boolean isVirtio() {return false;}
}
