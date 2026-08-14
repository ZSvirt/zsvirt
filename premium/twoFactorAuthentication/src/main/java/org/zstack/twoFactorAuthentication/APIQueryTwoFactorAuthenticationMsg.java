package org.zstack.twoFactorAuthentication;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;


@AutoQuery(replyClass = APIQueryTwoFactorAuthenticationReply.class, inventoryClass = TwoFactorAuthenticationSecretInventory.class)
@RestRequest(
        path = "/twofactorauthentication/secrets",
        optionalPaths = {"/twofactorauthentication/secrets/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryTwoFactorAuthenticationReply.class
)
public class APIQueryTwoFactorAuthenticationMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }

}
