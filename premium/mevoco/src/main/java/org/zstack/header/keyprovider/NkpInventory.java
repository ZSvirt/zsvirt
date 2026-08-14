package org.zstack.header.keyprovider;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = NkpVO.class, collectionValueOfMethod = "valueOf1")
@PythonClassInventory
public class NkpInventory extends KeyProviderInventory {
    private String kdf;
    private String saltPolicy;
    private boolean backedUp;
    private Integer currentVersion;

    protected NkpInventory(NkpVO vo) {
        super(vo);
        setKdf(vo.getKdf());
        setSaltPolicy(vo.getSaltPolicy());
        setBackedUp(vo.isBackedUp());
        setCurrentVersion(vo.getCurrentVersion());
    }

    public NkpInventory() {
    }

    public static NkpInventory valueOf(NkpVO vo) {
        return new NkpInventory(vo);
    }

    public static List<NkpInventory> valueOf1(Collection<NkpVO> vos) {
        return CollectionUtils.transform(vos, NkpInventory::valueOf);
    }

    public static NkpInventory __example__() {
        NkpInventory inv = new NkpInventory();
        inv.setUuid(DocUtils.createFixedUuid(NkpVO.class));
        inv.setName("nkp-1");
        inv.setDescription("example");
        inv.setType(KeyProviderType.NKP.toString());
        inv.setConnected(true);
        inv.setKdf(NkpKdf.HKDF_SHA256.toString());
        inv.setSaltPolicy(NkpSaltPolicy.PROVIDER_NAME.toString());
        inv.setBackedUp(true);
        inv.setCurrentVersion(1);
        inv.setCreateDate(new Timestamp(DocUtils.date));
        inv.setLastOpDate(new Timestamp(DocUtils.date));
        return inv;
    }

    public String getKdf() {
        return kdf;
    }

    public void setKdf(String kdf) {
        this.kdf = kdf;
    }

    public String getSaltPolicy() {
        return saltPolicy;
    }

    public void setSaltPolicy(String saltPolicy) {
        this.saltPolicy = saltPolicy;
    }

    public boolean isBackedUp() {
        return backedUp;
    }

    public void setBackedUp(boolean backedUp) {
        this.backedUp = backedUp;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }
}
