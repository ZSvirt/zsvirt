package org.zstack.header.keyprovider;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = KmsVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "activeIdentity", inventoryClass = KmsIdentityInventory.class,
                foreignKey = "activeIdentityUuid", expandedInventoryKey = "uuid"),
})
@PythonClassInventory
public class KmsInventory extends KeyProviderInventory {
    private String endpoint;
    private Integer port;
    private String kmipVersion;
    private String username;
    @GsonTransient
    @APINoSee
    private String password;
    private String trustState;
    private String activeIdentityUuid;
    private String serverCertPem;
    private CertificateInfo serverCertInfo;
    private KmsIdentityInventory activeIdentity;

    protected KmsInventory(KmsVO vo) {
        super(vo);
        setEndpoint(vo.getEndpoint());
        setPort(vo.getPort());
        setKmipVersion(vo.getKmipVersionString());
        setUsername(vo.getUsername());
        setPassword(vo.getPassword());
        setTrustState(vo.getTrustState() == null ? KmsTrustState.MUTUAL_UNTRUSTED.toString() : vo.getTrustState().toString());
        setActiveIdentityUuid(vo.getActiveIdentityUuid());
        setServerCertPem(vo.getServerCertPem());
        setServerCertInfo(KeyProviderUtils.parseCertificateInfo(vo.getServerCertPem()));
        if (vo.getActiveIdentity() != null) {
            setActiveIdentity(KmsIdentityInventory.valueOf(vo.getActiveIdentity()));
        }
    }

    public KmsInventory() {
    }

    public static KmsInventory valueOf(KmsVO vo) {
        return new KmsInventory(vo);
    }

    public static List<KmsInventory> valueOf1(Collection<KmsVO> vos) {
        return CollectionUtils.transform(vos, KmsInventory::valueOf);
    }

    public static KmsInventory __example__() {
        KmsInventory inv = new KmsInventory();
        inv.setUuid(DocUtils.createFixedUuid(KmsVO.class));
        inv.setName("kms-1");
        inv.setDescription("example");
        inv.setType(KeyProviderType.KMS.toString());
        inv.setConnected(true);
        inv.setEndpoint("kms.example.com");
        inv.setPort(5696);
        inv.setKmipVersion(KeyProviderConstant.DEFAULT_KMIP_VERSION);
        inv.setUsername("user");
        inv.setTrustState(KmsTrustState.MUTUAL_TRUSTED.toString());
        inv.setServerCertPem("-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----");
        inv.setServerCertInfo(new CertificateInfo("CN=kms.example.com,O=zstack", "CN=zstack-ca,O=zstack",
                "kms.example.com", java.util.Arrays.asList("kms.example.com"), java.util.Arrays.asList("127.0.0.1"), DocUtils.timestamp()));
        KmsIdentityInventory identity = KmsIdentityInventory.__example__();
        inv.setActiveIdentityUuid(identity.getUuid());
        inv.setActiveIdentity(identity);
        inv.setCreateDate(new Timestamp(DocUtils.date));
        inv.setLastOpDate(new Timestamp(DocUtils.date));
        return inv;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getKmipVersion() {
        return kmipVersion;
    }

    public void setKmipVersion(String kmipVersion) {
        this.kmipVersion = kmipVersion;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTrustState() {
        return trustState;
    }

    public void setTrustState(String trustState) {
        this.trustState = trustState;
    }

    public String getActiveIdentityUuid() {
        return activeIdentityUuid;
    }

    public void setActiveIdentityUuid(String activeIdentityUuid) {
        this.activeIdentityUuid = activeIdentityUuid;
    }

    public String getServerCertPem() {
        return serverCertPem;
    }

    public void setServerCertPem(String serverCertPem) {
        this.serverCertPem = serverCertPem;
    }

    public CertificateInfo getServerCertInfo() {
        return serverCertInfo;
    }

    public void setServerCertInfo(CertificateInfo serverCertInfo) {
        this.serverCertInfo = serverCertInfo;
    }

    public KmsIdentityInventory getActiveIdentity() {
        return activeIdentity;
    }

    public void setActiveIdentity(KmsIdentityInventory activeIdentity) {
        this.activeIdentity = activeIdentity;
    }
}
