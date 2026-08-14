package org.zstack.header.cloudformation;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
@RestResponse(allTo = "inventories")
public class APIQueryStackTemplateReply extends APIQueryReply {
    private List<StackTemplateInventory> inventories = new ArrayList<>();

    public List<StackTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<StackTemplateInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryStackTemplateReply __example__() {
        APIQueryStackTemplateReply reply = new APIQueryStackTemplateReply();
        StackTemplateInventory inventory = new StackTemplateInventory();
        inventory.setContent("{  \"ZStackTemplateFormatVersion\" : \"2018-06-18\",  \"Description\": \"Example for create a group vm instance in zstack.\",  \"Parameters\" : {    \"imageUuid\": {      \"Type\" : \"String\",      \"Description\": \"Image Uuid, represents the image resource to startup one vm instance\"    },    \"instanceOfferingUuid\": {      \"Type\": \"String\",      \"Description\": \"The instance offering uuid\"    },    \"l3NetworkUuid\": {      \"Type\": \"String\",      \"Description\": \"The l3 network uuid\"    },    \"DiskOfferingUuid\": {      \"Type\": \"String\",      \"Description\": \"DiskOffering for empty disk\"    },    \"PrimaryStorageUuid\": {      \"Type\": \"String\",      \"Description\": \"primarystorage for initial disk\"    },    \"HostUuid\": {      \"Type\": \"String\",      \"Description\": \"host for initial disk\"    }  },  \"Resources\" : {    \"WebServer1\": {      \"Type\": \"ZStack::Resource::VmInstance\",      \"Properties\": {        \"name\" : \"vm\",        \"imageUuid\" : {\"Ref\": \"imageUuid\"},        \"instanceOfferingUuid\":  {\"Ref\": \"instanceOfferingUuid\"},        \"l3NetworkUuids\": [{\"Ref\": \"l3NetworkUuid\"}]      },      \"DeletionPolicy\": \"Retain\"    },    \"WebServer2\": {      \"Type\": \"ZStack::Resource::VmInstance\",      \"Properties\": {        \"name\" : \"vm-2\",        \"imageUuid\" : {\"Ref\": \"imageUuid\"},        \"instanceOfferingUuid\":  {\"Ref\": \"instanceOfferingUuid\"},        \"l3NetworkUuids\": [{\"Ref\": \"l3NetworkUuid\"}]      },      \"DependsOn\": [{\"Ref\": \"WebServer1\"}]    },    \"EmptyVolume\": {      \"Type\": \"ZStack::Resource::DataVolume\",      \"Properties\": {        \"name\" : \"empty-volume\",        \"diskOfferingUuid\": {\"Ref\": \"DiskOfferingUuid\"},        \"primaryStorageUuid\": {\"Ref\": \"PrimaryStorageUuid\"},        \"systemTags\": [{\"Fn::Join\": [\"::\", [\"localStorage\", \"hostUuid\", {\"Ref\": \"HostUuid\"}]]}]      },      \"DependsOn\": [{\"Ref\": \"WebServer2\"}]    },    \"AttachDataVolumeToVm\": {      \"Type\": \"ZStack::Action::AttachDataVolumeToVm\",      \"Properties\": {        \"vmInstanceUuid\": {\"Fn::GetAtt\" : [\"WebServer1\", \"uuid\"]},        \"volumeUuid\": {\"Fn::GetAtt\" : [\"EmptyVolume\", \"uuid\"]}      }    }  },  \"Outputs\": {    \"VmInstance\": {      \"Description\" : \"print vm instance\",      \"Value\" : {\"Ref\": \"WebServer1\"}    }  }}");
        inventory.setUuid(uuid());
        inventory.setName("test");
        inventory.setDescription("description");
        inventory.setVersion("2018-06-18");
        inventory.setType("zstack");
        inventory.setMd5sum(uuid());
        inventory.setState(true);
        reply.setInventories(asList(inventory));
        return reply;
    }
}
