package org.zstack.network.service.lb;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by shixin on 03/22/2018.
 */
@RestResponse(allTo = "inventories")
public class APIQueryCertificateReply extends APIQueryReply {
    private List<CertificateInventory> inventories;

    public List<CertificateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<CertificateInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryCertificateReply __example__() {
        APIQueryCertificateReply reply = new APIQueryCertificateReply();
        CertificateInventory cer = new CertificateInventory();

        cer.setName("www.domain.com");
        cer.setCertificate("-----BEGIN PRIVATE KEY-----" +
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDIpe/zscx2Qwun" +
                "yYPHusMCt/5n4LWSi4pzfsUfvzo4txTe7WwWZ4H3iEA7RwSkg49xx3Rn6jh9S0RM" +
                "ncqUjTxPUjlZKoN0w+nU2AsxXhbV8AiS3UoWJcZUtlfLAjeUKajy1F5hyxHsKJlm" +
                "OozAcXObunzuaWkKvjchqMSX54+E02h7JNtzv9lagr0MsB4hkraanlpQWr4mv3N7" +
                "D8kBz9wplMeJXeo4awls3kygiN63TnIQ5hzF7jNxR3uSFYQtUfLnzKcj4aIma62t" +
                "Df6pkpQ3S+SprGs/OkGvEldNus6FXTO7ixFyORM27ka3Rmv6SYQQquV+950xfb42" +
                "n9s2UE0pAgMBAAECggEAd0Ixye3O8ifNgLAE1K0MBcyouMNWCMFJzHX34nO9vkIL" +
                "dKk3imWBWUgxrUn713CezOjZJn6PjuEyujs7UmpUA22Cyp27PqbHkAvNM02SUxx1" +
                "bB19UIapSGaM7gvmTvkoAxT4+DVD9NL4wktgp22HYnNImEgs+AaxLB9sokEvC6mx" +
                "P23pv7LfnK4EDH2b+1pbE++VheVIBZxK+mTdxPRgwQBJv2VtK0LRDndnRU8gmB5O" +
                "0KBnep3sCsAKmWtAJf37L9lWgc9QC7LysnuMsZ10KuoeQndVetsAazBARHhUdLd1" +
                "fWQqbLklC6NtcLodoS+umuvHArKx6LungK4cmNyGZQKBgQD8abrSQXOJuNNx7N+E" +
                "Jjn3hjc3FOrSAgGY7LCiQ/qEP8EkWslxotRHKqKqz78oq4RApT/fBV17HqPe67IJ" +
                "5xhwYlIUASzs8WvigRV6ITSZv92G8iOSdW7ALZzF4q+tc2RgPvW/pZPaHoxfNU6T" +
                "C/QW35qAslPWab7Tel9cGb030wKBgQDLf+OLfUfhJUq8mZ3L8U1p1QLpSJNx282h" +
                "GVg+OXwpuQQZ5qrxkhzF/xBYCjmMQMEfxLtRJxYTPgkHcTp9ozl4ICXdmHpL8V+E" +
                "5PBQjbCBNCbE8kjXngCc0wiKsLJIUWmKoVRoq7dAaUz3INzkp0tujf8aa7DzC+1C" +
                "4+j5nvFlkwKBgCTAKClQyke1F2Qw/uI4xpvZeNSWQRJOpHjljVoy15jFx5NJfKcE" +
                "9C9gb8q68LQ1NM5MwR3xpAi3D1j3rDZw5UgHqLes7CObiv+xl7TufMAeBV0OiEtc" +
                "ucFVYswVE0sH0AeLYzCCJSLO69U457XVObbS26X9UOOZBeW4nYXFYZ1tAoGBAJgn" +
                "ShIcnObZRDUZwqMfC5uqud+E9UF3cBsY4SK9RnnHrSpUjtHKRps/5498LaURMZS4" +
                "OroluFqw0n1vCqWvqiOIHee+vwoTMjEiIBCKsEMapDYzVYVpzNl07HkOPm7V+Ey/" +
                "7WXJpl2RngtU1fRcpYjGwMuXY5mF/GM8FxC055bjAoGATYqGLqNhbZ0SVhgSs/2v" +
                "HMlJYaHwJLlYPS+9OWk5JycvYSbfa9/rc2jblieuE5MseHQFqU0BRVeHqY4dqpQp" +
                "eO45fC5h9vqC4Lp2LaXbwtxv5Z0cb/o8ecXuSXeF/G2PQvhK44IQFb9RSLuQBUMp" +
                "/gX6UNVS0dP+7dDpim5n2zY=" +
                "-----END PRIVATE KEY-----" +
                "-----BEGIN CERTIFICATE-----" +
                "MIID4zCCAsugAwIBAgIJAJWhNGBNfAtTMA0GCSqGSIb3DQEBCwUAMIGHMQswCQYD" +
                "VQQGEwJDTjELMAkGA1UECAwCU0gxCzAJBgNVBAcMAlNIMQ8wDQYDVQQKDAZaU1RB" +
                "Q0sxDDAKBgNVBAsMA0RldjEZMBcGA1UEAwwQc2hpeGluLnpzdGFjay5pbzEkMCIG" +
                "CSqGSIb3DQEJARYVc2hpeGluLnJ1YW5AenN0YWNrLmlvMB4XDTE4MDMyMDA0NDYw" +
                "M1oXDTE5MDMyMDA0NDYwM1owgYcxCzAJBgNVBAYTAkNOMQswCQYDVQQIDAJTSDEL" +
                "MAkGA1UEBwwCU0gxDzANBgNVBAoMBlpTVEFDSzEMMAoGA1UECwwDRGV2MRkwFwYD" +
                "VQQDDBBzaGl4aW4uenN0YWNrLmlvMSQwIgYJKoZIhvcNAQkBFhVzaGl4aW4ucnVh" +
                "bkB6c3RhY2suaW8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDIpe/z" +
                "scx2QwunyYPHusMCt/5n4LWSi4pzfsUfvzo4txTe7WwWZ4H3iEA7RwSkg49xx3Rn" +
                "6jh9S0RMncqUjTxPUjlZKoN0w+nU2AsxXhbV8AiS3UoWJcZUtlfLAjeUKajy1F5h" +
                "yxHsKJlmOozAcXObunzuaWkKvjchqMSX54+E02h7JNtzv9lagr0MsB4hkraanlpQ" +
                "Wr4mv3N7D8kBz9wplMeJXeo4awls3kygiN63TnIQ5hzF7jNxR3uSFYQtUfLnzKcj" +
                "4aIma62tDf6pkpQ3S+SprGs/OkGvEldNus6FXTO7ixFyORM27ka3Rmv6SYQQquV+" +
                "950xfb42n9s2UE0pAgMBAAGjUDBOMB0GA1UdDgQWBBTg8PjTZJmWSDA9GPTJ7K5w" +
                "WTPVsjAfBgNVHSMEGDAWgBTg8PjTZJmWSDA9GPTJ7K5wWTPVsjAMBgNVHRMEBTAD" +
                "AQH/MA0GCSqGSIb3DQEBCwUAA4IBAQDH5POu4FmmAsHXe49gL6Y6Kdcti2FTBYse" +
                "7ru05V4URGsU5Dab25mATqp7z7WCiv9pTdlC0KoJieML7rpLiLskBBLpToU8bUig" +
                "X96q5dmMtDbLSmGeYfhHj9tHeYuGv0U2eRcN2Jo6xlHrl6X3RazO/h/9mCW6sLAG" +
                "gaJ9MyQAiqRfYaO+ToBqdbHmBEwmueaOO7wFy9UbU7F/CdeEzblKdWRMKQgf5yxA" +
                "6pXYghjPWWNAqElxnnXskBmjMhYaDfGCQuRK5Ma362ax0i8UGqYfMnflBgy1qX8+" +
                "f7VjyWokK4tcjep72TTYkIVBGbwBMqk2U2v5qslBRmM5+pmAESJq" +
                "-----END CERTIFICATE-----");

        reply.setInventories(Arrays.asList(cer));
        return reply;
    }

}
