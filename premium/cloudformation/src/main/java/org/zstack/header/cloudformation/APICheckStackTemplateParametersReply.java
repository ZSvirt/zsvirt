package org.zstack.header.cloudformation;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/6/20.
 */
@RestResponse(fieldsTo = {"parameters", "preparameters"})
public class APICheckStackTemplateParametersReply extends APIReply {
    private List<StackParameters> parameters = new ArrayList<>();
    private List<StackParameters> preparameters = new ArrayList<>();

    public List<StackParameters> getParameters() {
        return parameters;
    }

    public void setParameters(List<StackParameters> parameters) {
        this.parameters = parameters;
    }

    public List<StackParameters> getPreparameters() {
        return preparameters;
    }

    public void setPreparameters(List<StackParameters> preparameters) {
        this.preparameters = preparameters;
    }

    public static APICheckStackTemplateParametersReply __example__() {
        APICheckStackTemplateParametersReply reply = new APICheckStackTemplateParametersReply();
        StackParameters parameters = new StackParameters();
        parameters.setParamName("imageUuid");
        parameters.setType("String");
        parameters.setDescription("Image Uuid, represents the image resource to startup one vm instance");
        parameters.setDefaultValue(uuid());
        parameters.setResourceType("Image");
        reply.setParameters(asList(parameters));
        return reply;
    }
}
