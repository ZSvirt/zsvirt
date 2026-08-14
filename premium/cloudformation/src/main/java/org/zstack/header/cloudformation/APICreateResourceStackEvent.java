package org.zstack.header.cloudformation;

import org.zstack.cloudformation.ResourceStackStatus;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/6/4.
 */
@RestResponse(allTo = "inventory")
public class APICreateResourceStackEvent extends APIEvent {
    private ResourceStackInventory inventory;

    public APICreateResourceStackEvent() {
    }

    public APICreateResourceStackEvent(String apiId) {
        super(apiId);
    }

    public ResourceStackInventory getInventory() {
        return inventory;
    }

    public void setInventory(ResourceStackInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateResourceStackEvent __example__() {
        APICreateResourceStackEvent event = new APICreateResourceStackEvent();

        ResourceStackInventory inventory = new ResourceStackInventory();
        inventory.setTemplateContent("{  \"ZStackTemplateFormatVersion\" : \"2018-06-18\",  \"Description\": \"Example for create a group vm instance in zstack.\",  \"Parameters\" : {    \"imageUuid\": {      \"Type\" : \"String\",      \"Description\": \"Image Uuid, represents the image resource to startup one vm instance\"    },    \"instanceOfferingUuid\": {      \"Type\": \"String\",      \"Description\": \"The instance offering uuid\"    },    \"l3NetworkUuid\": {      \"Type\": \"String\",      \"Description\": \"The l3 network uuid\"    },    \"DiskOfferingUuid\": {      \"Type\": \"String\",      \"Description\": \"DiskOffering for empty disk\"    },    \"PrimaryStorageUuid\": {      \"Type\": \"String\",      \"Description\": \"primarystorage for initial disk\"    },    \"HostUuid\": {      \"Type\": \"String\",      \"Description\": \"host for initial disk\"    }  },  \"Resources\" : {    \"WebServer1\": {      \"Type\": \"ZStack::Resource::VmInstance\",      \"Properties\": {        \"name\" : \"vm\",        \"imageUuid\" : {\"Ref\": \"imageUuid\"},        \"instanceOfferingUuid\":  {\"Ref\": \"instanceOfferingUuid\"},        \"l3NetworkUuids\": [{\"Ref\": \"l3NetworkUuid\"}]      },      \"DeletionPolicy\": \"Retain\"    },    \"WebServer2\": {      \"Type\": \"ZStack::Resource::VmInstance\",      \"Properties\": {        \"name\" : \"vm-2\",        \"imageUuid\" : {\"Ref\": \"imageUuid\"},        \"instanceOfferingUuid\":  {\"Ref\": \"instanceOfferingUuid\"},        \"l3NetworkUuids\": [{\"Ref\": \"l3NetworkUuid\"}]      },      \"DependsOn\": [{\"Ref\": \"WebServer1\"}]    },    \"EmptyVolume\": {      \"Type\": \"ZStack::Resource::DataVolume\",      \"Properties\": {        \"name\" : \"empty-volume\",        \"diskOfferingUuid\": {\"Ref\": \"DiskOfferingUuid\"},        \"primaryStorageUuid\": {\"Ref\": \"PrimaryStorageUuid\"},        \"systemTags\": [{\"Fn::Join\": [\"::\", [\"localStorage\", \"hostUuid\", {\"Ref\": \"HostUuid\"}]]}]      },      \"DependsOn\": [{\"Ref\": \"WebServer2\"}]    },    \"AttachDataVolumeToVm\": {      \"Type\": \"ZStack::Action::AttachDataVolumeToVm\",      \"Properties\": {        \"vmInstanceUuid\": {\"Fn::GetAtt\" : [\"WebServer1\", \"uuid\"]},        \"volumeUuid\": {\"Fn::GetAtt\" : [\"EmptyVolume\", \"uuid\"]}      }    }  },  \"Outputs\": {    \"VmInstance\": {      \"Description\" : \"print vm instance\",      \"Value\" : {\"Ref\": \"WebServer1\"}    }  }}");
        inventory.setUuid(uuid());
        inventory.setName("test");
        inventory.setDescription("description");
        inventory.setVersion("2018-06-18");
        inventory.setType("zstack");
        inventory.setStatus(ResourceStackStatus.Created.toString());
        inventory.setReason(null);
        inventory.setParamContent("{  \"imageUuid\": \"8fcfe758a7eb13118d7344a08ff790a5\",  \"instanceOfferingUuid\": \"751f662a32184933aff487f5c6e167a6\",  \"l3NetworkUuid\": \"1245de5c2d28454bb63e60575ec611cb\",  \"DiskOfferingUuid\": \"ad0b4ea4c747401c92a7c990f7375cf1\",  \"PrimaryStorageUuid\": \"06c35e7f42264a74abb5b828367169fe\",  \"HostUuid\": \"9b57690de23f449e99c8f0da311e568e\"}");
        inventory.setEnableRollback(true);
        event.setInventory(inventory);
        return event;
    }
}
