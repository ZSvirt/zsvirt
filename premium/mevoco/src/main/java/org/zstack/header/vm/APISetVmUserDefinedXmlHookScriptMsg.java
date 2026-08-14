package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmUserDefinedXmlHookScriptEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APISetVmUserDefinedXmlHookScriptMsg extends APICreateMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam
    private String xmlHookScriptBase64;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getXmlHookScriptBase64() {
        return xmlHookScriptBase64;
    }

    public void setXmlHookScriptBase64(String xmlHookScriptBase64) {
        this.xmlHookScriptBase64 = xmlHookScriptBase64;
    }

    public static APISetVmUserDefinedXmlHookScriptMsg __example__() {
        APISetVmUserDefinedXmlHookScriptMsg msg = new APISetVmUserDefinedXmlHookScriptMsg();
        msg.setVmInstanceUuid(uuid());
        msg.setXmlHookScriptBase64("ZGVmIGNvbmZpZ19tYWNfYWRkcmVzc19ieV9iYW5kd2lkdGgocm9vdCwgaG9vaywgbWFjX2FkZHJlc3MsIGV4cGVjdF9iYW5kd2lkdGhfbWJwcyk6CiAgICBkZWYgY29uZmlnX21hY19hZGRyZXNzKGludGVyZmFjZSk6CiAgICAgICAgZm9yIG1hYyBpbiBpbnRlcmZhY2UuZmluZGFsbCgibWFjIik6CiAgICAgICAgICAgIGhvb2subW9kaWZ5X3ZhbHVlX29mX2F0dHJpYnV0ZShtYWMsICJhZGRyZXNzIiwgbWFjX2FkZHJlc3MpCgogICAgIyBmaW5kIG5ldHdvcmsgKGludGVyZmFjZSkgY2FyZCB3aXRoIHNwZWNpZmljIGJhbmR3aWR0aAogICAgZm9yIGRldmljZXMgaW4gcm9vdC5maW5kYWxsKCJkZXZpY2VzIik6CiAgICAgICAgZm9yIGludGVyZmFjZSBpbiBkZXZpY2VzLmZpbmRhbGwoImludGVyZmFjZSIpOgogICAgICAgICAgICBmb3IgYmFuZHdpZHRoIGluIGludGVyZmFjZS5maW5kYWxsKCJiYW5kd2lkdGgiKToKICAgICAgICAgICAgICAgIGluYm91bmQgPSBpbnQoaG9vay5nZXRfdmFsdWVfb2ZfYXR0cmlidXRlX2Zyb21fcGFyZW50KGJhbmR3aWR0aCwgImluYm91bmQiLCAiYXZlcmFnZSIpKS8xMjgKICAgICAgICAgICAgICAgIG91dGJvdW5kID0gaW50KGhvb2suZ2V0X3ZhbHVlX29mX2F0dHJpYnV0ZV9mcm9tX3BhcmVudChiYW5kd2lkdGgsICJvdXRib3VuZCIsICJhdmVyYWdlIikpLzEyOAogICAgICAgICAgICAgICAgaWYgaW5ib3VuZCA9PSBleHBlY3RfYmFuZHdpZHRoX21icHMgYW5kIG91dGJvdW5kID09IGV4cGVjdF9iYW5kd2lkdGhfbWJwczoKICAgICAgICAgICAgICAgICAgICBjb25maWdfbWFjX2FkZHJlc3MoaW50ZXJmYWNlKQoKY29uZmlnX21hY19hZGRyZXNzX2J5X2JhbmR3aWR0aChyb290LCBob29rLCAiMzc6MWE6ZDA6NGU6M2I6MWUiLCA2NjYpCg==");
        return msg;
    }
}
