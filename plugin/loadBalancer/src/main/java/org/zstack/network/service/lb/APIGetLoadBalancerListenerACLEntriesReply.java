package org.zstack.network.service.lb;

import org.zstack.header.acl.AccessControlListEntryInventory;
import org.zstack.header.acl.AccessControlListEntryVO;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;


@RestResponse(allTo = "inventories")
public class APIGetLoadBalancerListenerACLEntriesReply extends APIReply {
    private Map<String, List<LoadBalancerACLEntry>> inventories;

    public Map<String, List<LoadBalancerACLEntry>> getInventories() {
        return inventories;
    }

    public void setInventories(Map<String, List<LoadBalancerACLEntry>> inventories) {
        this.inventories = inventories;
    }

    public static APIGetLoadBalancerListenerACLEntriesReply __example__() {
        APIGetLoadBalancerListenerACLEntriesReply reply = new APIGetLoadBalancerListenerACLEntriesReply();
        AccessControlListEntryInventory inv = new AccessControlListEntryInventory();
        inv.setUuid(uuid());
        inv.setUrl("/test");
        inv.setDomain("zstack.io");
        inv.setName("test");
        inv.setAclUuid(uuid());
        inv.setType("redirect");
        return reply;
    }

    static class LoadBalancerACLEntry {
        private String uuid;
        private String aclUuid;

        private String type;

        private String name;
        private String domain;
        private String url;

        private String ipEntries;

        private String description;

        private Timestamp createDate;

        private Timestamp lastOpDate;

        private List<LoadBalancerServerGroupInventory> serverGroups;

        public void valueOf(AccessControlListEntryVO vo) {
            this.uuid = vo.getUuid();
            this.aclUuid = vo.getAclUuid();
            this.domain = vo.getDomain();
            this.url = vo.getUrl();
            this.name = vo.getName();
            this.type = vo.getType();
            this.description = vo.getDescription();
            this.ipEntries = vo.getIpEntries();
            this.createDate = vo.getCreateDate();
            this.lastOpDate = vo.getLastOpDate();
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getAclUuid() {
            return aclUuid;
        }

        public void setAclUuid(String aclUuid) {
            this.aclUuid = aclUuid;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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

        public String getIpEntries() {
            return ipEntries;
        }

        public void setIpEntries(String ipEntries) {
            this.ipEntries = ipEntries;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Timestamp getCreateDate() {
            return createDate;
        }

        public void setCreateDate(Timestamp createDate) {
            this.createDate = createDate;
        }

        public Timestamp getLastOpDate() {
            return lastOpDate;
        }

        public void setLastOpDate(Timestamp lastOpDate) {
            this.lastOpDate = lastOpDate;
        }

        public List<LoadBalancerServerGroupInventory> getServerGroups() {
            return serverGroups;
        }

        public void setServerGroups(List<LoadBalancerServerGroupInventory> serverGroups) {
            this.serverGroups = serverGroups;
        }
    }
}
