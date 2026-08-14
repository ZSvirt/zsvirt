package org.zstack.header.vpc;

import org.zstack.header.configuration.PythonClassInventory;

import java.io.Serializable;

/**
 * Created by weiwang on 02/12/2017
 */

public class VpcConnectionEntry implements Serializable, Cloneable{
    private String srcL2Type;
    private String dstL2Type;
    private String srcMac;
    private String dstMac;
    private Integer srcL2Vni;
    private Integer dstL2Vni;
    private String opDate;
    private String status;

    public String getSrcL2Type() {
        return srcL2Type;
    }

    public void setSrcL2Type(String srcL2Type) {
        this.srcL2Type = srcL2Type;
    }

    public String getDstL2Type() {
        return dstL2Type;
    }

    public void setDstL2Type(String dstL2Type) {
        this.dstL2Type = dstL2Type;
    }

    public String getSrcMac() {
        return srcMac;
    }

    public void setSrcMac(String srcMac) {
        this.srcMac = srcMac;
    }

    public String getDstMac() {
        return dstMac;
    }

    public void setDstMac(String dstMac) {
        this.dstMac = dstMac;
    }

    public Integer getL2SrcVni() {
        return srcL2Vni;
    }

    public void setL2SrcVni(Integer srcVni) {
        this.srcL2Vni = srcVni;
    }

    public Integer getL2DstVni() {
        return dstL2Vni;
    }

    public void setL2DstVni(Integer dstVni) {
        this.dstL2Vni = dstVni;
    }

    public String getOpDate() {
        return opDate;
    }

    public void setOpDate(String opDate) {
        this.opDate = opDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
