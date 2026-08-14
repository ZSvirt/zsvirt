package org.zstack.autoscaling.group.activity;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Create by lining at 2018/9/28
 */
@AutoQuery(replyClass = APIQueryAutoScalingGroupActivityReply.class, inventoryClass = AutoScalingGroupActivityInventory.class)
@RestRequest(
        path = "/autoscaling/groups/activities",
        optionalPaths = {"/autoscaling/groups/activities/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAutoScalingGroupActivityReply.class
)
public class APIQueryAutoScalingGroupActivityMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
