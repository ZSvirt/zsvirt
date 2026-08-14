package org.zstack.header.managementnode;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mei Lei <meilei007@gmail.com> on 11/1/16.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetCurrentTimeReply extends APIReply {
    private Map<String, Long> currentTime;

    public Map<String, Long> getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(Map<String, Long> currentTime) {
        this.currentTime = currentTime;
    }
 
    public static APIGetCurrentTimeReply __example__() {
        APIGetCurrentTimeReply reply = new APIGetCurrentTimeReply();
        Map<String, Long> ret = new HashMap<String, Long>();
        long currentTimeMillis = org.zstack.header.message.DocUtils.date;
        long currentTimeSeconds = org.zstack.header.message.DocUtils.date / 1000;
        ret.put("MillionSeconds", currentTimeMillis);
        ret.put("Seconds", currentTimeSeconds);

        reply.setCurrentTime(ret);
        return reply;
    }

}
