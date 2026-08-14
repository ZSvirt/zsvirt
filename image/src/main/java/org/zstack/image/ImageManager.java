package org.zstack.image;

import org.zstack.header.image.AddImageMessage;
import org.zstack.header.image.ImageVO;

import java.util.function.Consumer;

public interface ImageManager {
    ImageVO createImageInDb(AddImageMessage msg, Consumer<ImageVO> updater);
}
