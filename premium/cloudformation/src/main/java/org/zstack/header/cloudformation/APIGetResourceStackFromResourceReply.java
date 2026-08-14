package org.zstack.header.cloudformation;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mingjian.deng on 2020/3/26.
 */
@RestResponse(fieldsTo = {"stack"})
public class APIGetResourceStackFromResourceReply extends APIReply {
    private Map<String, String> stack = new HashMap<>();

    public Map<String, String> getStack() {
        return stack;
    }

    public void setStack(Map<String, String> stack) {
        this.stack = stack;
    }

    public static APIGetResourceStackFromResourceReply __example__() {
        APIGetResourceStackFromResourceReply reply = new APIGetResourceStackFromResourceReply();
        Map<String, String> s = new HashMap<>();
        s.put("stackUuid", uuid());
        reply.setStack(s);
        return reply;
    }
}
