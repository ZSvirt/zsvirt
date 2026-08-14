package org.zstack.header.rest;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class DefaultSSLVerifier {
    private static final CLogger logger = Utils.getLogger(DefaultSSLVerifier.class);

    public static boolean verify(String s, SSLSession sslSession) {
        return true;
    }

    // a trust manager that does not validate certificate chains
    final static public TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
    };

    public static SSLContext getSSLContext(TrustManager[] trustManagers) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new java.security.SecureRandom());
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.warn("getSSLFactory failed", e);
            return null;
        }
    }

    public static SSLSocketFactory getSSLFactory(TrustManager[] trustManagers) {
        SSLContext sslContext = getSSLContext(trustManagers);

        if (sslContext != null) {
            return sslContext.getSocketFactory();
        }

        return null;
    }
}
