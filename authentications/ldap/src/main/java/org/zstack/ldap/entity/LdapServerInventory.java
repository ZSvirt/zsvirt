package org.zstack.ldap.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceInventory;
import org.zstack.utils.CollectionUtils;

import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = LdapServerVO.class, collectionValueOfMethod = "valueOf1")
@PythonClassInventory
public class LdapServerInventory extends ThirdPartyAccountSourceInventory {
    private String url;
    private String base;
    private String username;
    private String serverType;
    private String encryption;
    private String filter;
    private String usernameProperty;

    public static LdapServerInventory valueOf(LdapServerVO vo) {
        LdapServerInventory inv = new LdapServerInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getResourceName());
        inv.setDescription(vo.getDescription());
        inv.setUrl(vo.getUrl());
        inv.setBase(vo.getBase());
        inv.setUsername(vo.getUsername());
        inv.setEncryption(vo.getEncryption());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setType(vo.getType());
        inv.setServerType(vo.getServerType().toString());
        inv.setFilter(vo.getFilter());
        inv.setUsernameProperty(vo.getUsernameProperty());
        inv.setCreateAccountStrategy(vo.getCreateAccountStrategy().toString());
        inv.setDeleteAccountStrategy(vo.getDeleteAccountStrategy().toString());
        return inv;
    }

    public static List<LdapServerInventory> valueOf1(Collection<LdapServerVO> vos) {
        return CollectionUtils.transform(vos, LdapServerInventory::valueOf);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getUsernameProperty() {
        return usernameProperty;
    }

    public void setUsernameProperty(String usernameProperty) {
        this.usernameProperty = usernameProperty;
    }
}
