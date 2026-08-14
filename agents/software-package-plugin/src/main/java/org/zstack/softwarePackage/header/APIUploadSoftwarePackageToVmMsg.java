package org.zstack.softwarePackage.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/software-packages/uploadtovm",
        method = HttpMethod.POST,
        responseClass = APIUploadSoftwarePackageToVmEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APIUploadSoftwarePackageToVmMsg extends APIMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String type;

    @NoLogging(type = NoLogging.Type.Uri)
    @APIParam(maxLength = 1024)
    private String url;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APINoSee
    private String hostUuid;

    @APINoSee
    private String targetIp;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public static APIUploadSoftwarePackageToVmMsg __example__() {
        APIUploadSoftwarePackageToVmMsg msg = new APIUploadSoftwarePackageToVmMsg();
        msg.setType("ZMigrate");
        msg.setUrl("upload://VMware-vix-disklib-8.0.1-21562716.x86_64.tar.gz");
        msg.setVmInstanceUuid(uuid(VmInstanceVO.class));
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(vmInstanceUuid, VmInstanceVO.class);
    }
}
