package org.zstack.autoscaling.group;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Create by weiwang at 2018/8/16
 */
@AutoQuery(replyClass = APIQueryAutoScalingGroupReply.class, inventoryClass = AutoScalingGroupInventory.class)
@RestRequest(
        path = "/autoscaling/groups",
        optionalPaths = {"/autoscaling/groups/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAutoScalingGroupReply.class
)
public class APIQueryAutoScalingGroupMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
