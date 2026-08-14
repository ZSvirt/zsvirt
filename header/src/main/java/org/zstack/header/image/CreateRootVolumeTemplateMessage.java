package org.zstack.header.image;

/**
 * Created by MaJin on 2021/3/17.
 */
public interface CreateRootVolumeTemplateMessage extends AddImageMessage {
    @Override
    default String getMediaType() {
        return ImageConstant.ImageMediaType.RootVolumeTemplate.toString();
    }
}
