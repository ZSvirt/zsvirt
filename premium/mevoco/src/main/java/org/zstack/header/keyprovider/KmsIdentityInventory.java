package org.zstack.header.keyprovider;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = KmsIdentityVO.class, collectionValueOfMethod = "valueOf1")
@PythonClassInventory
public class KmsIdentityInventory implements Serializable {
    private String uuid;
    private String kmsUuid;
    private String identityType;
    private String clientCertPem;
    @GsonTransient
    @APINoSee
    private String clientKeyPem;
    private String csrPem;
    private Timestamp certExpiredDate;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected KmsIdentityInventory(KmsIdentityVO vo) {
        setUuid(vo.getUuid());
        setKmsUuid(vo.getKmsUuid());
        setIdentityType(vo.getIdentityType() == null ? null : vo.getIdentityType().toString());
        setClientCertPem(vo.getClientCertPem());
        setClientKeyPem(vo.getClientKeyPem());
        setCsrPem(vo.getCsrPem());
        setCertExpiredDate(vo.getCertExpiredDate());
        setCreateDate(vo.getCreateDate());
        setLastOpDate(vo.getLastOpDate());
    }

    public KmsIdentityInventory() {
    }

    public static KmsIdentityInventory valueOf(KmsIdentityVO vo) {
        return new KmsIdentityInventory(vo);
    }

    public static List<KmsIdentityInventory> valueOf1(Collection<KmsIdentityVO> vos) {
        return CollectionUtils.transform(vos, KmsIdentityInventory::valueOf);
    }

    public static KmsIdentityInventory __example__() {
        KmsIdentityInventory inv = new KmsIdentityInventory();
        inv.setUuid(DocUtils.createFixedUuid(KmsIdentityVO.class));
        inv.setKmsUuid(DocUtils.createFixedUuid(KmsVO.class));
        inv.setIdentityType(KmsIdentityType.CSR.toString());
        inv.setClientCertPem("-----BEGIN CERTIFICATE-----\\n...\\n-----END CERTIFICATE-----");
        inv.setCsrPem("-----BEGIN CERTIFICATE REQUEST-----\\n...\\n-----END CERTIFICATE REQUEST-----");
        inv.setCertExpiredDate(DocUtils.timestamp());
        inv.setCreateDate(new Timestamp(DocUtils.date));
        inv.setLastOpDate(new Timestamp(DocUtils.date));
        return inv;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getKmsUuid() {
        return kmsUuid;
    }

    public void setKmsUuid(String kmsUuid) {
        this.kmsUuid = kmsUuid;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getClientCertPem() {
        return clientCertPem;
    }

    public void setClientCertPem(String clientCertPem) {
        this.clientCertPem = clientCertPem;
    }

    public String getClientKeyPem() {
        return clientKeyPem;
    }

    public void setClientKeyPem(String clientKeyPem) {
        this.clientKeyPem = clientKeyPem;
    }

    public String getCsrPem() {
        return csrPem;
    }

    public void setCsrPem(String csrPem) {
        this.csrPem = csrPem;
    }

    public Timestamp getCertExpiredDate() {
        return certExpiredDate;
    }

    public void setCertExpiredDate(Timestamp certExpiredDate) {
        this.certExpiredDate = certExpiredDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
