package org.zstack.storage.backup.imagestore;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2017/9/14.
 */
@RestRequest(
        path = "/backup-storage/image-store/disaster",
        method = HttpMethod.POST,
        responseClass = APIAddImageStoreBackupStorageEvent.class,
        parameterName = "params"
)
@Deprecated
public class APIAddDisasterImageStoreBackupStorageMsg extends APIAddImageStoreBackupStorageMsg {
    @APIParam(required = false)
    private String attachPoint;
    @APIParam(required = false)
    private String endPoint;

    public String getAttachPoint() {
        return attachPoint;
    }

    public void setAttachPoint(String attachPoint) {
        this.attachPoint = attachPoint;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public static APIAddDisasterImageStoreBackupStorageMsg __example__() {
        APIAddDisasterImageStoreBackupStorageMsg msg = new APIAddDisasterImageStoreBackupStorageMsg();

        msg.setName("ImageStore");
        msg.setUrl("/data/imagestore");
        msg.setHostname("192.168.1.8");
        msg.setUsername("admin");
        msg.setPassword("admin%pass");
        msg.setAttachPoint("/test-bs");
        msg.setEndPoint("test12345-snp99.cn-shanghai.nas.aliyuncs.com");

        return msg;
    }
}
