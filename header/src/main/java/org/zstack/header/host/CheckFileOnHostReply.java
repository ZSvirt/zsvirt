package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.Map;

/**
 * Created by Wenhao.Zhang on 21/09/06
 */
public class CheckFileOnHostReply extends MessageReply {
    /**
     * key : path,  value : md5 (if 'md5Return' in CheckFileOnHostMsg is set, otherwise empty string)
     */
    Map<String, String> existPaths;

    public Map<String, String> getExistPaths() {
        return existPaths;
    }

    public void setExistPaths(Map<String, String> existPaths) {
        this.existPaths = existPaths;
    }
}
