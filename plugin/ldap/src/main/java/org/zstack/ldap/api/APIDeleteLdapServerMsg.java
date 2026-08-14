package org.zstack.ldap.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.ldap.entity.LdapServerVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;


@RestRequest(
        path = "/ldap/servers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteLdapServerEvent.class
)
public class APIDeleteLdapServerMsg extends APIDeleteMessage {
    @APIParam(resourceType = LdapServerVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public List<String> getDeletedResourceUuidList() {
        return list(getUuid());
    }

    public static APIDeleteLdapServerMsg __example__() {
        APIDeleteLdapServerMsg msg = new APIDeleteLdapServerMsg();
        msg.setUuid(uuid());

        return msg;
    }

}
