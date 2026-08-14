package org.zstack.storage.ceph.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;

/**
 * Created by xing5 on 2017/2/28.
 */
@RestRequest(
        path = "/primary-storage/ceph/{primaryStorageUuid}/pools",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddCephPrimaryStoragePoolEvent.class
)
public class APIAddCephPrimaryStoragePoolMsg extends APICreateMessage implements PrimaryStorageMessage, APIAuditor {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;
    @APIParam(maxLength = 255)
    private String poolName;
    @APIParam(maxLength = 255, required = false)
    private String aliasName;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(validValues = {"Root", "Data"})
    private String type;

    private boolean isCreate;

    public boolean isCreate() {
        return isCreate;
    }

    public void setCreate(boolean create) {
        isCreate = create;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }


    public String getAliasName() {
        return aliasName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public static APIAddCephPrimaryStoragePoolMsg __example__() {
        APIAddCephPrimaryStoragePoolMsg msg = new APIAddCephPrimaryStoragePoolMsg();
        msg.setPoolName("highPerformance");
        msg.setAliasName("alias pool name");
        msg.setDescription("for high performance data volumes");
        msg.setPrimaryStorageUuid(uuid());
        msg.setType("Data");
        msg.setCreate(true);
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String resUuid = "";
        if (rsp.isSuccess()) {
            APIAddCephPrimaryStoragePoolEvent evt = (APIAddCephPrimaryStoragePoolEvent) rsp;
            resUuid = evt.getInventory().getUuid();
        }
        return new Result(resUuid, CephPrimaryStoragePoolVO.class);
    }
}
