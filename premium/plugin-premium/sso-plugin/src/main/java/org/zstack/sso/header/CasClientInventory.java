package org.zstack.sso.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceInventory;
import org.zstack.utils.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2022/8/24
 */
@PythonClassInventory
@Inventory(mappingVOClass = CasClientVO.class, collectionValueOfMethod = "valueOf1")
public class CasClientInventory extends ThirdPartyAccountSourceInventory {
    private String loginMNUrl;
    private String redirectUrl;
    private String casServerLoginUrl;
    private String casServerUrlPrefix;
    private String serverName;
    private String state;
    private String usernameProperty;

    public static CasClientInventory valueOf(CasClientVO vo) {
        CasClientInventory inventory = new CasClientInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setName(vo.getResourceName());
        inventory.setType(vo.getType());
        inventory.setDescription(vo.getDescription());
        inventory.setCreateAccountStrategy(vo.getCreateAccountStrategy().toString());
        inventory.setDeleteAccountStrategy(vo.getDeleteAccountStrategy().toString());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        inventory.setLoginMNUrl(vo.getLoginMNUrl());
        inventory.setRedirectUrl(vo.getRedirectUrl());
        inventory.setCasServerLoginUrl(vo.getCasServerLoginUrl());
        inventory.setCasServerUrlPrefix(vo.getCasServerUrlPrefix());
        inventory.setServerName(vo.getServerName());
        inventory.setState(vo.getState().toString());
        inventory.setUsernameProperty(vo.getUsernameProperty());
        return inventory;
    }

    public static List<OAuth2ClientInventory> valueOf1(Collection<OAuth2ClientVO> vos) {
        return CollectionUtils.transform(vos, OAuth2ClientInventory::valueOf);
    }

    public String getLoginMNUrl() {
        return loginMNUrl;
    }

    public void setLoginMNUrl(String loginMNUrl) {
        this.loginMNUrl = loginMNUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getCasServerLoginUrl() {
        return casServerLoginUrl;
    }

    public void setCasServerLoginUrl(String casServerLoginUrl) {
        this.casServerLoginUrl = casServerLoginUrl;
    }

    public String getCasServerUrlPrefix() {
        return casServerUrlPrefix;
    }

    public void setCasServerUrlPrefix(String casServerUrlPrefix) {
        this.casServerUrlPrefix = casServerUrlPrefix;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUsernameProperty() {
        return usernameProperty;
    }

    public void setUsernameProperty(String usernameProperty) {
        this.usernameProperty = usernameProperty;
    }

    public static CasClientInventory __example__() {
        CasClientInventory inventory = new CasClientInventory();
        inventory.setUuid("dc6cd27ea6c25cafba684d19a01107f9");
        return inventory;
    }
}
