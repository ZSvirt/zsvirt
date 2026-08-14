package org.zstack.header.cloudformation;

import org.zstack.cloudformation.template.struct.ActionStruct;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.sdk.CreateVmInstanceAction;
import org.zstack.utils.CollectionDSL;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by mingjian.deng on 2018/6/6.
 */
@RestResponse(allTo = "preview")
public class APIPreviewResourceStackReply extends APIReply {
    private PreviewResourceStruct preview;

    public PreviewResourceStruct getPreview() {
        return preview;
    }

    public void setPreview(PreviewResourceStruct preview) {
        this.preview = preview;
    }

    public static APIPreviewResourceStackReply __example__() {
        APIPreviewResourceStackReply reply = new APIPreviewResourceStackReply();
        PreviewResourceStruct previews = new PreviewResourceStruct();
        ActionStruct action = new ActionStruct();
        action.setActionName("org.zstack.sdk.CreateVmInstanceAction");
        action.setResourceName("WebServer1");
        action.setRound(0);
        action.setInDegree(new HashSet<>());
        CreateVmInstanceAction vm = new CreateVmInstanceAction();
        vm.imageUuid = uuid();
        vm.l3NetworkUuids = CollectionDSL.list(uuid());
        vm.name = "vm";
        vm.strategy = "InstantStart";
        vm.instanceOfferingUuid = uuid();
        action.setActions(vm);

        Map<String, Boolean> cond = new HashMap<>();
        cond.put("WithDataVolume", false);

        previews.setActions(Collections.singletonList(action));
        previews.setConditions(cond);
        reply.setPreview(previews);
        return reply;
    }
}
