package org.zstack.externalapiadapter;

import com.aliyun.ak.sdk.core.model.AccessKeyInfo;
import com.aliyun.ak.sdk.core.model.AccessKeyStatus;
import com.aliyun.ak.sdk.proxy.AkClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UmmAkFastjsonCompatibilityTest {
    @Test
    public void akSdkParsesResponseWithFastjson2CompatibilityArtifact() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = ("{\"@type\":\"java.lang.Exception\","
                    + "\"accesskeyid\":\"ak-test\",\"accesskeysecret\":\"secret\","
                    + "\"status\":\"enabled\",\"accesskeytype\":\"symmetric\",\"userid\":42}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();

        try {
            AkClient client = new AkClient();
            client.setKey("umm-key");
            client.setSecret("umm-secret");
            client.setBaseUrl(String.format("http://127.0.0.1:%s", server.getAddress().getPort()));

            AccessKeyInfo info = client.getAccessKeyInfo("ak-test");

            assertNotNull("AK SDK should return parsed access key info", info);
            assertEquals("ak-test", info.getKey());
            assertEquals("secret", info.getSecret());
            assertEquals(42L, info.getOwner());
            assertEquals(AccessKeyStatus.ENABLED, info.getStatus());
        } finally {
            server.stop(0);
        }
    }
}
