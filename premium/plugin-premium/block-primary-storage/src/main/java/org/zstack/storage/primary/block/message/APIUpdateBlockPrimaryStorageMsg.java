package org.zstack.storage.primary.block.message;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.storage.primary.block.BlockPrimaryStorageConstants;
import org.zstack.storage.primary.block.BlockPrimaryStorageVO;


/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/3/17 17:15
 */
@TagResourceType(PrimaryStorageVO.class)
@RestRequest(
        path = "/primary-storage/block/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateBlockPrimaryStorageEvent.class
)
public class APIUpdateBlockPrimaryStorageMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(required = false)
    private String vendorName;

    @APIParam(required = false)
    private String metadata;

    @APIParam(resourceType = BlockPrimaryStorageVO.class)
    private String uuid;

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIUpdateBlockPrimaryStorageMsg __example__ () {
        APIUpdateBlockPrimaryStorageMsg msg = new APIUpdateBlockPrimaryStorageMsg();
        msg.setVendorName("XStor");
        msg.setMetadata("{'ip':'127.0.0.1', 'port':8443, 'user':'test', 'password':'password'}");
        return msg;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }
}
