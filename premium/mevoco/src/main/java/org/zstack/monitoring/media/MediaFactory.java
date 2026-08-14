package org.zstack.monitoring.media;

/**
 * Created by xing5 on 2017/6/11.
 */
public interface MediaFactory {
    MediaVO createMedia(MediaVO vo, APICreateMediaMsg msg);

    MediaInventory getMediaInventory(MediaVO vo);

    MediaInventory getMediaInventory(String uuid);

    String getMediaType();
}
