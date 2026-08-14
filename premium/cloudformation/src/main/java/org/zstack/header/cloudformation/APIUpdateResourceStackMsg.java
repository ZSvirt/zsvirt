package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/6/12.
 */
@RestRequest(
        path = "/cloudformation/stack/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateResourceStackEvent.class,
        isAction = true
)
public class APIUpdateResourceStackMsg extends APIMessage {
    @APIParam(resourceType = ResourceStackVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(required = false)
    private Boolean rollback;
    @APIParam(required = false, maxLength = 4194304)
    private String templateContent;
    @APIParam(required = false, maxLength = 524288)
    private String parameters;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public Boolean getRollback() {
        return rollback;
    }

    public void setRollback(Boolean rollback) {
        this.rollback = rollback;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public static APIUpdateResourceStackMsg __example__() {
        APIUpdateResourceStackMsg msg = new APIUpdateResourceStackMsg();

        msg.setUuid(uuid());
        msg.setName("stack");
        msg.setDescription("description");
        msg.setTemplateContent("{  \"ZStackTemplateFormatVersion\" : \"2018-06-18\",  \"Description\": \"Example for create a group vm instance in zstack.\",  \"Parameters\" : {    \"imageUuid\": {      \"Type\" : \"String\",      \"Description\": \"Image Uuid, represents the image resource to startup one vm instance\"    },    \"instanceOfferingUuid\": {      \"Type\": \"String\",      \"DefaultValue\" : \"instanceoffering-123\",      \"Description\": \"The instance offering uuid\"    },    \"l3NetworkUuid\": {      \"Type\": \"String\",      \"Description\": \"The l3 network uuid\"    },    \"DiskOfferingUuid\": {      \"Type\": \"String\",      \"Description\": \"DiskOffering for empty disk\"    },    \"PrimaryStorageUuid\": {      \"Type\": \"String\",      \"Description\": \"primarystorage for initial disk\"    },    \"HostUuid\": {      \"Type\": \"String\",      \"Description\": \"host for initial disk\"    }  },  \"Resources\" : {    \"WebServer1\": {      \"Type\": \"ZStack::Resource::VmInstance\",      \"Properties\": {        \"name\" : \"vm\",        \"imageUuid\" : {\"Ref\": \"imageUuid\"},        \"instanceOfferingUuid\":  {\"Ref\": \"instanceOfferingUuid\"},        \"l3NetworkUuids\": [{\"Ref\": \"l3NetworkUuid\"}]      },      \"DeletionPolicy\": \"Retain\"    },    \"WebServer2\": {      \"Type\": \"ZStack::Resource::VmInstance\",      \"Properties\": {        \"name\" : \"vm-2\",        \"imageUuid\" : {\"Ref\": \"imageUuid\"},        \"instanceOfferingUuid\":  {\"Ref\": \"instanceOfferingUuid\"},        \"l3NetworkUuids\": [{\"Ref\": \"l3NetworkUuid\"}]      },      \"DependsOn\": [{\"Ref\": \"WebServer1\"}]    },    \"EmptyVolume\": {      \"Type\": \"ZStack::Resource::DataVolume\",      \"Properties\": {        \"name\" : \"empty-volume\",        \"diskOfferingUuid\": {\"Ref\": \"DiskOfferingUuid\"},        \"primaryStorageUuid\": {\"Ref\": \"PrimaryStorageUuid\"},        \"systemTags\": [{\"Fn::Join\": [\"::\", [\"localStorage\", \"hostUuid\", {\"Ref\": \"HostUuid\"}]]}]      },      \"DependsOn\": [{\"Ref\": \"WebServer2\"}]    },    \"AttachDataVolumeToVm\": {      \"Type\": \"ZStack::Action::AttachDataVolumeToVm\",      \"Properties\": {        \"vmInstanceUuid\": {\"Fn::GetAtt\" : [\"WebServer1\", \"uuid\"]},        \"volumeUuid\": {\"Fn::GetAtt\" : [\"EmptyVolume\", \"uuid\"]}      }    }  },  \"Outputs\": {    \"VmInstance\": {      \"Description\" : \"print vm instance\",      \"Value\" : {\"Ref\": \"WebServer1\"}    }  }}");
        msg.setRollback(true);
        msg.setParameters("{  \"imageUuid\": \"8fcfe758a7eb13118d7344a08ff790a5\",  \"instanceOfferingUuid\": \"751f662a32184933aff487f5c6e167a6\",  \"l3NetworkUuid\": \"1245de5c2d28454bb63e60575ec611cb\",  \"DiskOfferingUuid\": \"ad0b4ea4c747401c92a7c990f7375cf1\",  \"PrimaryStorageUuid\": \"06c35e7f42264a74abb5b828367169fe\",  \"HostUuid\": \"9b57690de23f449e99c8f0da311e568e\"}");

        return msg;
    }
}
