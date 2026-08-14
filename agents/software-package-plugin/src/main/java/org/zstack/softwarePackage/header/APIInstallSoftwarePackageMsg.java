package org.zstack.softwarePackage.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/software-package/install/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIInstallSoftwarePackageEvent.class,
        isAction = true
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 2)
public class APIInstallSoftwarePackageMsg extends APIMessage {
    @APIParam(maxLength = 255, resourceType = SoftwarePackageVO.class)
    private String uuid;
    @NoLogging()
    @APIParam(required = false)
    private String config;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    @APINoSee
    private SoftwarePackageVO softwarePackageVO;

    public SoftwarePackageVO getSoftwarePackageVO() {
        return softwarePackageVO;
    }

    public void setSoftwarePackageVO(SoftwarePackageVO softwarePackageVO) {
        this.softwarePackageVO = softwarePackageVO;
    }

    public static APIInstallSoftwarePackageMsg buildAPIInstallSoftwarePackageMsg(APIInstallSoftwarePackageMsg msg) {
        APIInstallSoftwarePackageMsg amsg = new APIInstallSoftwarePackageMsg();
        amsg.setUuid(msg.getUuid());
        amsg.setConfig(msg.getConfig());
        return amsg;
    }

    public static APIInstallSoftwarePackageMsg __example__() {
        APIInstallSoftwarePackageMsg msg = new APIInstallSoftwarePackageMsg();
        msg.setUuid(uuid(SoftwarePackageVO.class));
        msg.setConfig("{}");
        return msg;
    }
}
