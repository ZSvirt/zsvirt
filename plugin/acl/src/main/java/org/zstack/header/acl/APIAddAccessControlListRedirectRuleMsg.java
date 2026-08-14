package org.zstack.header.acl;


import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

@TagResourceType(AccessControlListVO.class)
@RestRequest(
        path = "/access-control-lists/{aclUuid}/redirectRules",
        method = HttpMethod.POST,
        responseClass = APIAddAccessControlListEntryEvent.class,
        parameterName = "params"
)
public class APIAddAccessControlListRedirectRuleMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APINoSee
    private String criterion;

    @APINoSee
    private String matchMethod;

    @APIParam(required = false, maxLength = 255)
    private String domain;

    @APIParam(required = false, maxLength = 80)
    @NoLogging
    private String url;

    @APIParam(resourceType = AccessControlListVO.class)
    private String aclUuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getCriterion() {
        return criterion;
    }

    public void setCriterion(String criterion) {
        this.criterion = criterion;
    }

    public String getMatchMethod() {
        return matchMethod;
    }

    public void setMatchMethod(String matchMethod) {
        this.matchMethod = matchMethod;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAclUuid() {
        return aclUuid;
    }

    public void setAclUuid(String aclUuid) {
        this.aclUuid = aclUuid;
    }

    public static APIAddAccessControlListRedirectRuleMsg __example__() {
        APIAddAccessControlListRedirectRuleMsg msg = new APIAddAccessControlListRedirectRuleMsg();

        msg.setName("redirect-rule");
        msg.setAclUuid(uuid());
        msg.setCriterion("DomainAndUrl");
        msg.setDomain("zstack.io");
        msg.setMatchMethod("AccurateMatch");
        msg.setUrl("/cloud");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return null;
    }
}
