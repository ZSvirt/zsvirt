package org.zstack.sso.header;

import org.zstack.header.identity.AccountVO;
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
        String tokenStr = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJGV3NnY2k4cENJQkZFY1hoVkZmNkhzVUkwU1FmekN2SGJ0LVFFOHc4OXJZIn0.eyJleHAiOjE2NjM1NzQ3NjcsImlhdCI6MTY2MzU3NDQ2NywiYXV0aF90aW1lIjoxNjYzNTc0NDY3LCJqdGkiOiI2MzQwOTRjNy00ZjA3LTRlZGMtYjMxMS05MmE5NDc0NWIwYTkiLCJpc3MiOiJodHRwczovLzE3Mi4yMC4xNi4xODg6ODQ0My9hdXRoL3JlYWxtcy9jbG91ZCIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJkNjYyYWNiYi0yMjYxLTQ2ODctYjNkZS0zOTI2OTA0ZWRlNzQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJjbG91ZF9zc28iLCJzZXNzaW9uX3N0YXRlIjoiNTM1MjM0ZDUtOTEyYi00NzY2LThhMjktN2Q5MTdkYjE2YTBmIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJkZWZhdWx0LXJvbGVzLWNsb3VkIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwic2lkIjoiNTM1MjM0ZDUtOTEyYi00NzY2LThhMjktN2Q5MTdkYjE2YTBmIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoicWl1eXUgemhhbmciLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJxaXV5dSIsImdpdmVuX25hbWUiOiJxaXV5dSIsImZhbWlseV9uYW1lIjoiemhhbmciLCJlbWFpbCI6IjI0OTQ0OTcyNkBxcS5jb20ifQ.Mun8NSAnoPW3IjRLFySji8r243ydDYCARMXRstTllM1vsmVp56E3wpfJy8LAf_9PPAo-ryoTkuj1O2Z_nDV4F5lMQ_QASwqIMoBKzg3_Umf-D2pPlTC9BjMIuCeK0qoFiuJq23IS7IN9lYAHDRMdyP6MLUjkyqW-sWgq1l3boT7FcHfkl6cGBzFDLigpsckkm_b7yiN8lDxBdw7SNTcg_AO6D7ZipPG09up8hiatq_4fY1vnhfiBbL9pa8vX4wKoGGR8B_-uO6PrcALOt08QrW06YB084n56dzjD3hCRe9fA9tHB7krTCMnHZN-Bf0QptsYEzwU5zUvIRke9SM88Kg";
        OAuth2TokenInventory inventory = new OAuth2TokenInventory();
        inventory.setUuid(uuid());
        inventory.setClientUuid(uuid());
        inventory.setUserUuid(uuid());
        inventory.setIdToken(tokenStr);
        inventory.setAccessToken(tokenStr);
        inventory.setRefreshToken(tokenStr);
        reply.setInventory(inventory);
        return reply;
    }
}
