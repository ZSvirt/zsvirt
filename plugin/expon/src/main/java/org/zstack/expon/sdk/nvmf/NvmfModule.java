package org.zstack.expon.sdk.nvmf;


/**
 * @example
 * {
 * "id": "e209db89-992c-4a1d-a4d4-303efd78d2df",
 * "name": "nvmf_1",
 * "nqn": "nqn.2022-03.com.sds.wds:nvmf",
 * "sn": "ETWDS34aac3e7",
 * "vendor": "ET_WDS"
 *  }
 */
public class NvmfModule {
    private String id;
    private String name;
    private String nqn;
    private String sn;
    private String vendor;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNqn() {
        return nqn;
    }

    public String getSn() {
        return sn;
    }

    public String getVendor() {
        return vendor;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNqn(String nqn) {
        this.nqn = nqn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
}
