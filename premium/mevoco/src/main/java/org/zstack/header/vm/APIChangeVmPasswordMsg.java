package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.log.NoLogging;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by mingjian.deng on 16/10/17.
 */
@TagResourceType(VmInstanceVO.class)
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeVmPasswordEvent.class,
        isAction = true
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APIChangeVmPasswordMsg extends APIMessage implements VmInstanceMessage {

    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    @APIParam(noTrim = true, validRegexValues = VmInstanceConstant.USER_VM_REGEX_PASSWORD, maxLength = 32, password = true)
    @NoLogging
    private String password;

    @APIParam(noTrim = true, nonempty = true)
    private String account;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setVmInstanceUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public static APIChangeVmPasswordMsg __example__() {
        APIChangeVmPasswordMsg msg = new APIChangeVmPasswordMsg();
        msg.setUuid(uuid());
        msg.setVmInstanceUuid(uuid());
        msg.setAccount("root");
        msg.setPassword("password");

        return msg;
    }
}


