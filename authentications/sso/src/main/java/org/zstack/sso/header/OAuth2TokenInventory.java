package org.zstack.sso.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2023/2/3
 */
@PythonClassInventory
@Inventory(mappingVOClass = OAuth2TokenVO.class, collectionValueOfMethod="valueOf1")
public class OAuth2TokenInventory extends SSOTokenInventory {
    private String accessToken;
    private String idToken;
    private String refreshToken;

    public OAuth2TokenInventory(OAuth2TokenVO vo) {
        super(vo);
        this.setAccessToken(vo.getAccessToken());
        this.setIdToken(vo.getIdToken());
        this.setRefreshToken(vo.getRefreshToken());
    }

    public OAuth2TokenInventory() {
    }

    public static OAuth2TokenInventory valueOf(OAuth2TokenVO vo) {
        return new OAuth2TokenInventory(vo);
    }

    public static List<OAuth2TokenInventory> valueOf1(Collection<OAuth2TokenVO> vos) {
        List<OAuth2TokenInventory> invs = new ArrayList<OAuth2TokenInventory>(vos.size());
        for (OAuth2TokenVO vo : vos) {
            invs.add(OAuth2TokenInventory.valueOf(vo));
        }
        return invs;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
