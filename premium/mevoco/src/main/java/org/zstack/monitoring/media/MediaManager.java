package org.zstack.monitoring.media;

/**
 * Created by xing5 on 2017/6/11.
 */
public interface MediaManager {
    MediaFactory getMediaFactory(String type);
}
