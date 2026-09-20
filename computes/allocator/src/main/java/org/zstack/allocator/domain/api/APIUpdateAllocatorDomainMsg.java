package org.zstack.allocator.domain.api;

import org.springframework.http.HttpMethod;
import org.zstack.allocator.domain.entity.AllocatorDomainRuleParam;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.header.allocator.AllocatorDomainScene;
import org.zstack.allocator.domain.header.AllocatorDomainState;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(
        path = "/allocator-domains/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateAllocatorDomainEvent.class
)
public class APIUpdateAllocatorDomainMsg extends APIMessage implements AllocatorDomainMessage {
    @APIParam(resourceType = AllocatorDomainVO.class)
    private String uuid;

    @APIParam(required = false, maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false, validEnums = AllocatorDomainState.class)
    private String state;

    /**
     * @desc ordered allocator-domain rules. List order is the evaluation order and must be preserved.
     * When this field is set, the domain's rules are rewritten as this list; list index is stored as
     * priority starting at 0.
     *
     * Host/Cluster, PrimaryStorage, and L3 are separate chains. In each chain every matching
     * rule is applied top-down and a later rule overrides the earlier verdict for that
     * candidate. Hard (Include / Exclude) decides membership; soft (Recommend / NotRecommend)
     * only affects sort order, only if the candidate is already included, and does not stop
     * the hard track. A later soft rule also overrides an earlier soft rule.
     *
     * If any Include exists in a chain, that chain is a whitelist; otherwise it is a blacklist.
     * Overlapping selectors therefore depend on order. Example: Exclude host H, then Include
     * cluster C that contains H → H is allowed. Reverse the two → H is denied. Put exceptions
     * after broader rules. PrimaryStorage resourceQualifier Root / Data scopes that rule;
     * empty applies to both.
     */
    @APIParam(required = false)
    private List<AllocatorDomainRuleParam> rules;

    @APIParam(required = false, validEnums = AllocatorDomainScene.class)
    private List<String> scenes;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<AllocatorDomainRuleParam> getRules() {
        return rules;
    }

    public void setRules(List<AllocatorDomainRuleParam> rules) {
        this.rules = rules;
    }

    public List<String> getScenes() {
        return scenes;
    }

    public void setScenes(List<String> scenes) {
        this.scenes = scenes;
    }

    @Override
    public String getAllocatorDomainUuid() {
        return uuid;
    }

    public static APIUpdateAllocatorDomainMsg __example__() {
        APIUpdateAllocatorDomainMsg msg = new APIUpdateAllocatorDomainMsg();
        msg.setUuid(DocUtils.createFixedUuid(AllocatorDomainVO.class));
        msg.setName("domain-1");
        msg.setState(AllocatorDomainState.Enabled.toString());
        return msg;
    }
}
