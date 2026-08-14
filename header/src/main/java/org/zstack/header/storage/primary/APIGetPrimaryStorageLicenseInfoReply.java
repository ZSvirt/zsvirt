package org.zstack.header.storage.primary;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIGetPrimaryStorageLicenseInfoReply extends APIReply {
    private String uuid;
    private String name;
    private String expireTime;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }

    public static APIGetPrimaryStorageLicenseInfoReply __example__() {
        APIGetPrimaryStorageLicenseInfoReply reply = new APIGetPrimaryStorageLicenseInfoReply();
        reply.setUuid(uuid());
        reply.setName("Ceph");
        reply.setExpireTime("");
        return reply;
    }
}