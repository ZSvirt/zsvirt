package org.zstack.sso.header;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @Author: DaoDao
 * @Date: 2022/9/6
 */
@Entity
@Table
public class SSORedirectTemplateVO extends ResourceVO {
    @Column
    private String name;
    @Column
    private String description;
    @Column
    @ForeignKey(parentEntityClass = ThirdPartyAccountSourceVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String clientUuid;
    @Column
    private String redirectTemplate;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clientUuid", referencedColumnName = "uuid", insertable = false, updatable = false)
    @NoView
    private ThirdPartyAccountSourceVO ssoClient;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public String getRedirectTemplate() {
        return redirectTemplate;
    }

    public void setRedirectTemplate(String redirectTemplate) {
        this.redirectTemplate = redirectTemplate;
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

    public ThirdPartyAccountSourceVO getSsoClient() {
        return ssoClient;
    }

    public void setSsoClient(ThirdPartyAccountSourceVO ssoClient) {
        this.ssoClient = ssoClient;
    }
}
