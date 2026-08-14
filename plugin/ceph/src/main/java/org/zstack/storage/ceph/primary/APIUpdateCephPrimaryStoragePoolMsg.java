package org.zstack.storage.ceph.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * Created by AlanJager on 2017/9/4.
 */
@RestRequest(
        path = "/primary-storage/ceph/pools/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateCephPrimaryStoragePoolEvent.class
)
public class APIUpdateCephPrimaryStoragePoolMsg extends APIMessage implements PrimaryStorageMessage {
    @APINoSee
    private String primaryStorageUuid;
    @APIParam(resourceType = CephPrimaryStoragePoolVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String aliasName;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public static APIUpdateCephPrimaryStoragePoolMsg __example__() {
        APIUpdateCephPrimaryStoragePoolMsg msg = new APIUpdateCephPrimaryStoragePoolMsg();

        msg.setUuid(uuid());
        msg.setAliasName("alias");
        msg.setDescription("description");

        return msg;
    }
}
