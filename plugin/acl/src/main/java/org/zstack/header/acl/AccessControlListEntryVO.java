package org.zstack.header.acl;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-05
 **/
@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = AccessControlListVO.class, myField = "aclUuid", targetField = "uuid"),
        }
)
public class AccessControlListEntryVO implements ToInventory {
    @Id
    @Column
    @Index
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = AccessControlListVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String aclUuid;

    @Column
    private String type;

    @Column
    private String name;
    @Column
    private String matchMethod;
    @Column
    private String criterion;
    @Column
    private String domain;
    @Column
    private String url;
    @Column
    private String redirectRule;

    @Column
    private String ipEntries;
    @Column
    private String description;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMatchMethod() {
        return matchMethod;
    }

    public void setMatchMethod(String matchMethod) {
        this.matchMethod = matchMethod;
    }

    public String getCriterion() {
        return criterion;
    }

    public void setCriterion(String criterion) {
        this.criterion = criterion;
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

    public String getRedirectRule() {
        return redirectRule;
    }

    public void setRedirectRule(String redirectRule) {
        this.redirectRule = redirectRule;
    }
}
