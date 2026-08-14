package org.zstack.autoscaling.group.instance;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.group.APIQueryAutoScalingGroupReply;
import org.zstack.autoscaling.group.AutoScalingGroupInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Create by lining at 2018/9/28
 */
@AutoQuery(replyClass = APIQueryAutoScalingGroupInstanceReply.class, inventoryClass = AutoScalingGroupInstanceInventory.class)
@RestRequest(
        path = "/autoscaling/groups/instances",
        optionalPaths = {"/autoscaling/groups/instances/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAutoScalingGroupInstanceReply.class
)
public class APIQueryAutoScalingGroupInstanceMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
