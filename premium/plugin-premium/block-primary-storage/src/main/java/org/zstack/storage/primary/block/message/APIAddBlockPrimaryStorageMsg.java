package org.zstack.storage.primary.block.message;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.log.NoLogging;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.storage.primary.block.BlockPrimaryStorageConstants;


/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/3/17 17:15
 */
@TagResourceType(PrimaryStorageVO.class)
@RestRequest(
        path = "/primary-storage/block",
        method = HttpMethod.POST,
        responseClass = APIAddPrimaryStorageEvent.class,
        parameterName = "param"
)
public class APIAddBlockPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
    @APIParam()
    private String vendorName;

    @APIParam()
    private String metadata;

    @Override
    public String getType() {
        return BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE;
    }

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

    public static APIAddBlockPrimaryStorageMsg __example__ () {
        APIAddBlockPrimaryStorageMsg msg = new APIAddBlockPrimaryStorageMsg();
        msg.setVendorName("XStor");
        msg.setMetadata("{'ip':'127.0.0.1', 'port':8443, 'user':'test', 'password':'password'}");
        return msg;
    }
}
