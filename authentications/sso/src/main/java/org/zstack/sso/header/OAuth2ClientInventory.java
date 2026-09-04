package org.zstack.sso.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.search.Inventory;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceInventory;
import org.zstack.utils.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2022/8/23
 */
@PythonClassInventory
@Inventory(mappingVOClass = OAuth2ClientVO.class, collectionValueOfMethod = "valueOf1")
public class OAuth2ClientInventory extends ThirdPartyAccountSourceInventory {
    private String clientId;
    @NoLogging
    private String clientSecret;
    private String grantType;
    private String loginMNUrl;
    private String redirectUrl;
    private String authorizationUrl;
    private String tokenUrl;
    private String userinfoUrl;
    private String logoutUrl;
    private String usernameProperty;

    public static OAuth2ClientInventory valueOf(OAuth2ClientVO vo) {
        OAuth2ClientInventory inventory = new OAuth2ClientInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setName(vo.getResourceName());
        inventory.setType(vo.getType());
        inventory.setDescription(vo.getDescription());
        inventory.setCreateAccountStrategy(vo.getCreateAccountStrategy().toString());
        inventory.setDeleteAccountStrategy(vo.getDeleteAccountStrategy().toString());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        inventory.setClientId(vo.getClientId());
        inventory.setClientSecret(vo.getClientSecret());
        inventory.setGrantType(vo.getGrantType());
        inventory.setLoginMNUrl(vo.getLoginMNUrl());
        inventory.setRedirectUrl(vo.getRedirectUrl());
        inventory.setAuthorizationUrl(vo.getAuthorizationUrl());
        inventory.setTokenUrl(vo.getTokenUrl());
        inventory.setUserinfoUrl(vo.getUserinfoUrl());
        inventory.setLogoutUrl(vo.getLogoutUrl());
        inventory.setUsernameProperty(vo.getUsernameProperty());
        return inventory;
    }

    public static List<OAuth2ClientInventory> valueOf1(Collection<OAuth2ClientVO> vos) {
        return CollectionUtils.transform(vos, OAuth2ClientInventory::valueOf);
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
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

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getUserinfoUrl() {
        return userinfoUrl;
    }

    public void setUserinfoUrl(String userinfoUrl) {
        this.userinfoUrl = userinfoUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public String getUsernameProperty() {
        return usernameProperty;
    }

    public void setUsernameProperty(String usernameProperty) {
        this.usernameProperty = usernameProperty;
    }

    public static OAuth2ClientInventory __example__() {
        OAuth2ClientInventory inventory = new OAuth2ClientInventory();
        inventory.setUuid("27fa448b03fd5a379882c831bddcf796");
        return inventory;
    }
}
