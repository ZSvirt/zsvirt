package org.zstack.ldap.driver;

import java.security.cert.*;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/**
 * Created by miao on 16-9-14.
 */
public class DummyTrustManager implements X509TrustManager {
    public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        // do nothing
    }

    public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        // do nothing
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[0];
    }
}