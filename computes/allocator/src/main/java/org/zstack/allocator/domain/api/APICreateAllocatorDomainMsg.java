package org.zstack.allocator.domain.api;

import org.springframework.http.HttpMethod;
import org.zstack.allocator.domain.entity.AllocatorDomainRuleParam;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.header.allocator.AllocatorDomainScene;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DocUtils;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.zone.ZoneVO;

import java.util.ArrayList;
import java.util.List;

@TagResourceType(AllocatorDomainVO.class)
@RestRequest(
        path = "/allocator-domains",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateAllocatorDomainEvent.class
)
public class APICreateAllocatorDomainMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    /**
     * @desc ordered allocator-domain rules. List order is the evaluation order and must be preserved.
     * On persist, the list index is stored as priority starting at 0.
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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
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

    public static APICreateAllocatorDomainMsg __example__() {
        APICreateAllocatorDomainMsg msg = new APICreateAllocatorDomainMsg();
        msg.setName("domain-1");
        msg.setDescription("example allocator domain");
        msg.setZoneUuid(DocUtils.createFixedUuid(ZoneVO.class));
        List<AllocatorDomainRuleParam> rules = new ArrayList<>();
        rules.add(AllocatorDomainRuleParam.__example__());
        msg.setRules(rules);
        List<String> scenes = new ArrayList<>();
        scenes.add(AllocatorDomainScene.NewCreate.toString());
        scenes.add(AllocatorDomainScene.Start.toString());
        scenes.add(AllocatorDomainScene.Migrate.toString());
        scenes.add(AllocatorDomainScene.Ha.toString());
        scenes.add(AllocatorDomainScene.HostMaintenance.toString());
        scenes.add(AllocatorDomainScene.Drs.toString());
        msg.setScenes(scenes);
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String resUuid = "";
        if (rsp.isSuccess()) {
            APICreateAllocatorDomainEvent evt = (APICreateAllocatorDomainEvent) rsp;
            resUuid = evt.getInventory().getUuid();
        }
        return new Result(resUuid, AllocatorDomainVO.class);
    }
}
