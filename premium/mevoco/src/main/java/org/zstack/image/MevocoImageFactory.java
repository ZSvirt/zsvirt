package org.zstack.image;

import org.zstack.header.image.Image;
import org.zstack.header.image.ImageVO;

/**
 * Created by mingjian.deng on 17/1/4.
 */
public class MevocoImageFactory extends DefaultImageFactory {
    @Override
    public Image getImage(ImageVO vo) {
        return new MevocoImageBase(vo);
    }
}
