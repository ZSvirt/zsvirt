package org.zstack.header.cloudformation;

import org.zstack.cloudformation.StackEventStatus;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/6/14.
 */
@RestResponse(allTo = "inventories")
public class APIQueryEventFromResourceStackReply extends APIQueryReply {
    private List<CloudFormationStackEventInventory> inventories = new ArrayList<>();

    public List<CloudFormationStackEventInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<CloudFormationStackEventInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryEventFromResourceStackReply __example__() {
        APIQueryEventFromResourceStackReply reply = new APIQueryEventFromResourceStackReply();
        String stackUuid = uuid();
        CloudFormationStackEventInventory inventory = new CloudFormationStackEventInventory();
        inventory.setDescription("description");
        inventory.setActionStatus(StackEventStatus.Start.toString());
        inventory.setAction("CreateVmInstanceAction");
        inventory.setContent("{\\n  \"l3NetworkUuids\": [\\n    \"1245de5c2d28454bb63e60575ec611cb\"\\n  ],\\n  \"name\": \"vm\",\\n  \"description\": \"test\\nenter\",\\n  \"systemTags\": [\\n    \"userdata::I2Nsb3VkLWNvbmZpZwp1c2VyczoKIC0gbmFtZTogcm9vdAogICBzaGVsbDogL2Jpbi9iYXNoCiAgIGdyb3Vwczogcm9vdAogICBzdWRvOiBbJ0FMTD0oQUxMKSBOT1BBU1NXRDpBTEwnXQogICBzc2gtYXV0aG9yaXplZC1rZXlzOgogICAgICAgLSBzc2gtcnNhIEFBQUFCM056YUMxeWMyRUFBQUFEQVFBQkFBQUJBUURmZ2dMQVRrM0prVW5uazczT1F6b1dOdzN4UFdtb1FNVjV6bUZWekEwYVFyWHZoT00xakk3bXJiLzdKVTJTK0t3Nm1xUFp5QVUvTWg3WEc5Smw4REh3NzJEZWlzOEVWYm8yanA5dkU1dHRmdXY0K3Rvb1o2Sm9STVNDOEdvcGlkd2RZYWw3Y3o2Vk9TYzgyWkFyR3VlVUxzMzFqWEUzclIxNk96V0tTRzFVL1RsbXA5V0Rlamxyd1dZMCtPZzA4WHBORWVjMnFkUnpvV3lHMHJ5WEpDbUQrdmxCSXErWnVMQVRMZWZQUk1uNGZOVlROM1JmZ0Q0aVEvR2Jaa3RJK1BwZ1ppRkdMVW0zVnJwMjNJckVzSTdjUkszV01lZ2RNSlVrQmFzR05STjB1d082OXNvM3lBbi9NZTZ0b1hmd2JOaC9MWEpPRkh2RFo5bmtscWwydnA0MyByb290QDEwLjAuMTIxLjE3NQp3cml0ZV9maWxlczoKIC0gcGF0aDogL3RtcC9aU3RhY2tfY29uZmlnCiAgIGNvbnRlbnQ6IHwKICAgICAgIEhlbGxvLHdvcmxkIQpob3N0bmFtZToga292ZW4tdGVzdApkaXNhYmxlX3Jvb3Q6IGZhbHNlCmNocGFzc3dkOgogIGxpc3Q6IHwKICAgICAgcm9vdDpwYXNzd29yZAogIGV4cGlyZTogRmFsc2UKcnVuY21kOgogLSBjdXJsIGh0dHA6Ly9zb2Z0LnZwc2VyLm5ldC9sbm1wL2xubXAxLjQudGFyLmd6IC1vIGxubXAxLjQudGFyLmd6ICYmIHRhciB6eGYgbG5tcDEuNC50YXIuZ3ogJiYgY2QgbG5tcDEuNCAmJiBlY2hvICIifC4vaW5zdGFsbC5zaCBsbm1w\"\\n  ],\\n  \"instanceOfferingUuid\": \"751f662a32184933aff487f5c6e167a6\",\\n  \"imageUuid\": \"8fcfe758a7eb13118d7344a08ff790a5\"\\n}");
        inventory.setId(1);
        inventory.setResourceName("WebServer1");
        inventory.setStackUuid(stackUuid);

        CloudFormationStackEventInventory inventory2 = new CloudFormationStackEventInventory();
        inventory2.setDescription("description");
        inventory2.setActionStatus(StackEventStatus.Finish.toString());
        inventory2.setAction("CreateVmInstanceAction");
        inventory2.setContent("{\\n  \"inventory\": {\\n    \"uuid\": \"cebf83ef8ec04f7a9fbc80a54043b749\",\\n    \"name\": \"vm\",\\n    \"description\": \"test\\nenter\",\\n    \"zoneUuid\": \"78c2900f033344a485e953bc02bd0010\",\\n    \"clusterUuid\": \"a60243d5048c4a5c8cc6a35c0c1a3c97\",\\n    \"imageUuid\": \"8fcfe758a7eb13118d7344a08ff790a5\",\\n    \"hostUuid\": \"9b57690de23f449e99c8f0da311e568e\",\\n    \"lastHostUuid\": \"9b57690de23f449e99c8f0da311e568e\",\\n    \"instanceOfferingUuid\": \"751f662a32184933aff487f5c6e167a6\",\\n    \"rootVolumeUuid\": \"17c96f79a28347128f1b147c3ef0d12b\",\\n    \"platform\": \"Linux\",\\n    \"defaultL3NetworkUuid\": \"1245de5c2d28454bb63e60575ec611cb\",\\n    \"type\": \"UserVm\",\\n    \"hypervisorType\": \"KVM\",\\n    \"memorySize\": 1073741824,\\n    \"cpuNum\": 1,\\n    \"cpuSpeed\": 0,\\n    \"allocatorStrategy\": \"LeastVmPreferredHostAllocatorStrategy\",\\n    \"createDate\": \"Jun 21, 2018 11:31:46 AM\",\\n    \"lastOpDate\": \"Jun 21, 2018 11:31:47 AM\",\\n    \"state\": \"Running\",\\n    \"vmNics\": [\\n      {\\n        \"uuid\": \"a25592b3aced44dc889cede4f8368e52\",\\n        \"vmInstanceUuid\": \"cebf83ef8ec04f7a9fbc80a54043b749\",\\n        \"l3NetworkUuid\": \"1245de5c2d28454bb63e60575ec611cb\",\\n        \"ip\": \"10.75.0.98\",\\n        \"mac\": \"fa:91:99:e1:d1:00\",\\n        \"netmask\": \"255.0.0.0\",\\n        \"gateway\": \"10.0.0.1\",\\n        \"deviceId\": 0,\\n        \"createDate\": \"Jun 21, 2018 11:31:46 AM\",\\n        \"lastOpDate\": \"Jun 21, 2018 11:31:46 AM\"\\n      }\\n    ],\\n    \"allVolumes\": [\\n      {\\n        \"uuid\": \"17c96f79a28347128f1b147c3ef0d12b\",\\n        \"name\": \"ROOT-for-vm\",\\n        \"description\": \"Root volume for VM[uuid:cebf83ef8ec04f7a9fbc80a54043b749]\",\\n        \"primaryStorageUuid\": \"06c35e7f42264a74abb5b828367169fe\",\\n        \"vmInstanceUuid\": \"cebf83ef8ec04f7a9fbc80a54043b749\",\\n        \"rootImageUuid\": \"8fcfe758a7eb13118d7344a08ff790a5\",\\n        \"installPath\": \"/zstack_ps/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-17c96f79a28347128f1b147c3ef0d12b/17c96f79a28347128f1b147c3ef0d12b.qcow2\",\\n        \"type\": \"Root\",\\n        \"format\": \"qcow2\",\\n        \"size\": 1610612736,\\n        \"actualSize\": 1342308352,\\n        \"deviceId\": 0,\\n        \"state\": \"Enabled\",\\n        \"status\": \"Ready\",\\n        \"createDate\": \"Jun 21, 2018 11:31:46 AM\",\\n        \"lastOpDate\": \"Jun 21, 2018 11:31:46 AM\",\\n        \"isShareable\": false\\n      }\\n    ]\\n  }\\n}");
        inventory2.setId(2);
        inventory2.setResourceName("WebServer1");
        inventory2.setStackUuid(stackUuid);

        reply.setInventories(asList(inventory, inventory2));
        return reply;
    }
}
