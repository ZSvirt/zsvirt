package org.zstack.sso.header;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2023/2/3
 */
@RestResponse(fieldsTo = "inventory")
public class APIGetOAuth2TokenReply extends APIReply {
    private OAuth2TokenInventory inventory;

    public OAuth2TokenInventory getInventory() {
        return inventory;
    }

    public void setInventory(OAuth2TokenInventory inventory) {
        this.inventory = inventory;
    }

    public static APIGetOAuth2TokenReply __example__() {
        APIGetOAuth2TokenReply reply = new APIGetOAuth2TokenReply();
        reply.setInventory(OAuth2TokenInventory.__example__());
        return reply;
    }
}
