package org.zstack.sso.header;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceInventory;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.utils.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2022/9/6
 */
@Inventory(mappingVOClass = SSORedirectTemplateVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "ssoClient", inventoryClass = ThirdPartyAccountSourceInventory.class,
                foreignKey = "clientUuid", expandedInventoryKey = "uuid")
})
public class SSORedirectTemplateInventory {
    private String uuid;
    private String name;
    private String description;
    private String clientUuid;
    private String redirectTemplate;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static SSORedirectTemplateInventory valueOf(SSORedirectTemplateVO vo) {
        SSORedirectTemplateInventory inv = new SSORedirectTemplateInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setClientUuid(vo.getClientUuid());
        inv.setRedirectTemplate(vo.getRedirectTemplate());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<SSORedirectTemplateInventory> valueOf(Collection<SSORedirectTemplateVO> vos) {
        return CollectionUtils.transform(vos, SSORedirectTemplateInventory::valueOf);
    }

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
}
