package org.zstack.ovf.message;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by Qi Le on 2022/3/7
 */
public class ParseOvfMsg extends NeedReplyMessage {
    private String xmlBase64;

    public String getXmlBase64() {
        return xmlBase64;
    }

    public void setXmlBase64(String xmlBase64) {
        this.xmlBase64 = xmlBase64;
    }
}
