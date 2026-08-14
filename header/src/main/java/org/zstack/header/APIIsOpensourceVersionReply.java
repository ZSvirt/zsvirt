package org.zstack.header;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/5/17.
 */
@RestResponse(fieldsTo = {"all"})
public class APIIsOpensourceVersionReply extends APIReply {
    private boolean opensource;

    public boolean isOpensource() {
        return opensource;
    }

    public void setOpensource(boolean opensource) {
        this.opensource = opensource;
    }
}
