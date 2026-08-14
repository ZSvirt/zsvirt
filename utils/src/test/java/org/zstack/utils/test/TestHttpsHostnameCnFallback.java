package org.zstack.utils.test;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.zstack.utils.HTTPS;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class TestHttpsHostnameCnFallback {
    @Test
    public void testCommonNameEqualsForTelemetryCa() throws Exception {
        File caFile = new File("../conf/telemetry/ca.pem");
        Assume.assumeTrue("telemetry CA not present in this checkout", caFile.isFile());

        X509Certificate cert;
        try (InputStream in = Files.newInputStream(caFile.toPath())) {
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
        Assert.assertTrue(HTTPS.commonNameEquals(cert, "telemetry.local"));
        Assert.assertTrue(HTTPS.commonNameEquals(cert, "Telemetry.Local"));
        Assert.assertFalse(HTTPS.commonNameEquals(cert, "172.20.19.124"));
        Assert.assertFalse(HTTPS.commonNameEquals(cert, "other.local"));
    }
}
