package org.zstack.header.vm;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.MessageReply;

/**
 * @author shanshan.ning
 * @date 2023-09-11
 */
public class TakeVmConsoleScreenshotReply extends MessageReply {
    @NoLogging(type = NoLogging.Type.LongText)
    private String imageData;

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
}
