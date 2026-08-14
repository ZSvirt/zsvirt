package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = PrimaryStorageEO.class, needView = false)
@AutoDeleteTag
public class ExternalPrimaryStorageVO extends PrimaryStorageVO implements ToInventory {
    @Column
    private String identity;

    @Column
    private String config;

    @Column
    private String addonInfo;

    @Column
    //@Convert(converter = PasswordConverter.class)
    private String password;

    @Column
    private String defaultProtocol;

    @Column
    @OneToMany(fetch= FetchType.EAGER)
    @JoinColumn(name="primaryStorageUuid", insertable=false, updatable=false)
    private Set<PrimaryStorageOutputProtocolRefVO> outputProtocols = new HashSet<>();

    public ExternalPrimaryStorageVO(PrimaryStorageVO vo) {
        super(vo);
    }

    public ExternalPrimaryStorageVO() {
        super();
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public Set<PrimaryStorageOutputProtocolRefVO> getOutputProtocols() {
        return outputProtocols;
    }

    public void setOutputProtocols(Set<PrimaryStorageOutputProtocolRefVO> outputProtocols) {
        this.outputProtocols = outputProtocols;
    }

    public String getDefaultProtocol() {
        return defaultProtocol;
    }

    public void setDefaultProtocol(String defaultProtocol) {
        this.defaultProtocol = defaultProtocol;
    }

    public String getAddonInfo() {
        return addonInfo;
    }

    public void setAddonInfo(String addonInfo) {
        this.addonInfo = addonInfo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
