package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
@TagResourceType(StackTemplateVO.class)
@RestRequest(
        path = "/cloudformation/template",
        method = HttpMethod.POST,
        responseClass = APIAddStackTemplateEvent.class,
        parameterName = "params"
)
public class APIAddStackTemplateMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(validValues = {"zstack"}, required = false)
    private String type = "zstack";
    @APIParam(required = false)
    @APINoSee // not support yet
    private String url;
    @APIParam(required = false, maxLength = 4194304)
    private String templateContent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddStackTemplateEvent)rsp).getInventory().getUuid() : "", StackTemplateVO.class);
    }

    public static APIAddStackTemplateMsg __example__() {
        APIAddStackTemplateMsg msg = new APIAddStackTemplateMsg();

        msg.setName("stack");
        msg.setDescription("description");
        msg.setType("zstack");
        msg.setTemplateContent("{  \"ZStackTemplateFormatVersion\" : \"2018-06-18\",  \"Description\": \"Example for create a group vm instance in zstack.\",  \"Parameters\" : {    \"imageUuid\": {      \"Type\" : \"String\",      \"Description\": \"Image Uuid, represents the image resource to startup one vm instance\"    },    \"instanceOfferingUuid\": {      \"Type\": \"String\",      \"DefaultValue\" : \"instanceoffering-123\",      \"Description\": \"The instance offering uuid\"    },    \"l3NetworkUuid\": {      \"Type\": \"String\",      \"Description\": \"The l3 network uuid\"    },    \"DiskOfferingUuid\": {      \"Type\": \"String\",      \"Description\": \"DiskOffering for empty disk\"    },    \"PrimaryStorageUuid\": {      \"Type\": \"String\",      \"Description\": \"primarystorage for initial disk\"    },    \"HostUuid\": {      \"Type\": \"String\",      \"Description\": \"host for initial disk\"    }  },  \"Resources\" : {    \"WebServer1\": {      \"Type\": \"ZStack::Resource::VmInstance\",      \"Properties\": {        \"name\" : \"vm\",        \"imageUuid\" : {\"Ref\": \"imageUuid\"},        \"instanceOfferingUuid\":  {\"Ref\": \"instanceOfferingUuid\"},        \"l3NetworkUuids\": [{\"Ref\": \"l3NetworkUuid\"}]      },      \"DeletionPolicy\": \"Retain\"    },    \"WebServer2\": {      \"Type\": \"ZStack::Resource::VmInstance\",      \"Properties\": {        \"name\" : \"vm-2\",        \"imageUuid\" : {\"Ref\": \"imageUuid\"},        \"instanceOfferingUuid\":  {\"Ref\": \"instanceOfferingUuid\"},        \"l3NetworkUuids\": [{\"Ref\": \"l3NetworkUuid\"}]      },      \"DependsOn\": [{\"Ref\": \"WebServer1\"}]    },    \"EmptyVolume\": {      \"Type\": \"ZStack::Resource::DataVolume\",      \"Properties\": {        \"name\" : \"empty-volume\",        \"diskOfferingUuid\": {\"Ref\": \"DiskOfferingUuid\"},        \"primaryStorageUuid\": {\"Ref\": \"PrimaryStorageUuid\"},        \"systemTags\": [{\"Fn::Join\": [\"::\", [\"localStorage\", \"hostUuid\", {\"Ref\": \"HostUuid\"}]]}]      },      \"DependsOn\": [{\"Ref\": \"WebServer2\"}]    },    \"AttachDataVolumeToVm\": {      \"Type\": \"ZStack::Action::AttachDataVolumeToVm\",      \"Properties\": {        \"vmInstanceUuid\": {\"Fn::GetAtt\" : [\"WebServer1\", \"uuid\"]},        \"volumeUuid\": {\"Fn::GetAtt\" : [\"EmptyVolume\", \"uuid\"]}      }    }  },  \"Outputs\": {    \"VmInstance\": {      \"Description\" : \"print vm instance\",      \"Value\" : {\"Ref\": \"WebServer1\"}    }  }}");

        return msg;
    }
}
