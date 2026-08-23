package org.zstack.allocator.domain.api;

import org.springframework.http.HttpMethod;
import org.zstack.allocator.domain.entity.AllocatorDomainRuleParam;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.allocator.domain.header.AllocatorDomainScene;
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
     * Rules are applied top-down like a Linux iptables / nftables chain. Hard (Include / Exclude)
     * and soft (Recommend / NotRecommend) are two tracks: the first matching hard rule is the
     * membership verdict and later hard rules are skipped for that candidate; the first matching
     * soft rule only affects sort order and does not stop the hard track.
     *
     * Overlapping selectors therefore depend on order. Example: Exclude host H, then Include cluster
     * C that contains H → H is denied. Reverse the two → H is allowed because the cluster Include
     * matches first. Put more specific rules before broader ones when you need exceptions, exactly
     * as you would put a host ACCEPT before a subnet DROP.
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
