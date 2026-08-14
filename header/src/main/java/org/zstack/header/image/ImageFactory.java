package org.zstack.header.image;

public interface ImageFactory {
    ImageType getType();

    ImageVO createImage(ImageVO vo);

    Image getImage(ImageVO vo);
}
