package org.zstack.autoscaling.template;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Create by weiwang at 2018/8/16
 */
@AutoQuery(replyClass = APIQueryAutoScalingVmTemplateReply.class, inventoryClass = AutoScalingVmTemplateInventory.class)
@RestRequest(
        path = "/autoscaling/vmtemplate",
        optionalPaths = {"/autoscaling/vmtemplate/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAutoScalingVmTemplateReply.class
)
public class APIQueryAutoScalingVmTemplateMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
