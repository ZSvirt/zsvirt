package org.zstack.premium.externalservice.loki;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.AbstractAnsibleAgentDeployArguments;

public class PromtailDeployArguments extends AbstractAnsibleAgentDeployArguments {
    @SerializedName("src_promtail_bin")
    private String srcPromtailBin;
    @SerializedName("dst_promtail_bin")
    private String dstPromtailBin;
    @SerializedName("pkg_promtail")
    private String pkgPromtail;

    public String getSrcPromtailBin() {
        return srcPromtailBin;
    }

    public void setSrcPromtailBin(String srcPromtailBin) {
        this.srcPromtailBin = srcPromtailBin;
    }

    public String getDstPromtailBin() {
        return dstPromtailBin;
    }

    public void setDstPromtailBin(String dstPromtailBin) {
        this.dstPromtailBin = dstPromtailBin;
    }

    public String getPkgPromtail() {
        return pkgPromtail;
    }

    public void setPkgPromtail(String pkgPromtail) {
        this.pkgPromtail = pkgPromtail;
    }

    @Override
    public String getPackageName() {
        return null;
    }
}
