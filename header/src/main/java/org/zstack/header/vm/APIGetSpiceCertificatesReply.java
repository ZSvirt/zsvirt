package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * author:kaicai.hu
 * Date:2019/9/16
 */
@RestResponse(fieldsTo = {"certificateStr"})
public class APIGetSpiceCertificatesReply extends APIReply {
    private String certificateStr;

    public String getCertificateStr() {
        return certificateStr;
    }

    public void setCertificateStr(String certificateStr) {
        this.certificateStr = certificateStr;
    }

    public static APIGetSpiceCertificatesReply __example__() {
        APIGetSpiceCertificatesReply reply = new APIGetSpiceCertificatesReply();
        reply.setCertificateStr("-----BEGIN CERTIFICATE-----\nMIIDVTCCAj2gAwIBAgIJALGiHgRCxwkwMA0GCSqGSIb3DQEBCwUAeEExCzAJBgNV\nBAYTAkNOMREwDwYDVQQHDAhTaGFuZ2hhaTEPMA0GA1UECgwGWlN0YWNrMQ4wDAYD\nVQQDDAVteSBDQTAeFw0xOTA5MjQwNjMxMzBaFw0yOTA5MjEwNjMxMzBaMEExCzAJ\nBgNVBAYTAkNOMREwDwYDVQQHDAhTaGFuZ2hhaTEPMA0GA1UECgwGWlN0YWNrMQ4w\nDAYDVQQDDAVteSBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALQq\nHgcza7xK7zQ+zPNrzR3q1JJHkQ/OPF+DH5gheGlwfruWaGtAwVX9w0vfKDHCJ0BB\ndMQ4m1r2OX0yONVXNmlUnwq5VMdieqyO/1r41cHauqUTP6MkJ5/yD7ipfZP0r7sB\ngFAUIwaSdZk4hwnPou2ah4Uk/9yd0thBHKYCYZoVldLyPIe7aUHmkdkRY+MMdzqj\nGnspV1K7ux8xP3zG7CUG9Ns64rrjGNLRu81IJTYaJSigZ1ykZu1eYsUcGHB2QDcE\n4d9fw/8YaRVJOsBsABEzj9Qvi81i6VdTCGg2oFjBrSgcF7tNVQDufZLgXw3rlIsE\n+1+pwh5CgcviRcNTue0CAwEAAaNQME4wHQYDVR0OBBYEFMNx0BYaStgcpq1s30tW\nmAwjlqXaMB8GA1UdIwQYMBaAFMNx0BYaStgcpq1s30tWmAwjlqXaMAwGA1UdEwQF\nMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAAxDDzO8LDwI31sCr2K2t2P3n/hkxh0w\nnycRxKho9DFTnKSmMlusSd1AY/SjWO8+6dSi4wzmkJ/Zr0CeOBYhbDCFxG/OKO1K\nSfuR+gqZOAx8gB58Zk51KUxyYV3HKoRPftIh4nFO+mFUMXDuttuh12L7kbSQ6U3v\nL0uFg/59PnYe+aU+R/K1xXyZGViZJbgDqPDsCZYug0IRupu9j2Sces3XcX2TMCGR\nxPo2ZzBrOs3rJCK2nx16tHS5Zr5VP7zdn1YCS1/0HYUHRpi3HJPZAUR6dzDuK0IU\nPb0ufTldT6UOgJTof6TPMVl8dZATlJqMrjPtAOe3PtOKo8TPdjtQ15I=\n-----END CERTIFICATE-----");
        return reply;
    }
}
