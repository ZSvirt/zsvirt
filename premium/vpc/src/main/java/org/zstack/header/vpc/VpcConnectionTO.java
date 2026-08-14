package org.zstack.header.vpc;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.exception.CloudRuntimeException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by weiwang on 04/12/2017
 */
@PythonClassInventory
public class VpcConnectionTO implements Serializable, Cloneable {
    private String sourceL2NetworkType;
    private String destinationL2NetworkType;
    private String sourceMac;
    private String destinationMac;
    private Integer sourceL2NetworkVni;
    private Integer destinationL2NetworkVni;
    private String lastOpDate;
    private String status;

    public static VpcConnectionTO valueOf(VpcConnectionEntry e) {
        VpcConnectionTO to = new VpcConnectionTO();

        to.setSourceL2NetworkType(e.getSrcL2Type());
        to.setDestinationL2NetworkType(e.getDstL2Type());
        to.setSourceL2NetworkVni(e.getL2SrcVni());
        to.setDestinationL2NetworkVni(e.getL2DstVni());
        to.setSourceMac(e.getSrcMac());
        to.setDestinationMac(e.getDstMac());
        to.setLastOpDate(e.getOpDate());
        to.setStatus(e.getStatus());

        return to;
    }

    public static Map<String, Object> valueOf1(Map<String, Object> m) {
        Map<String, Object> r = new HashMap<>();
        for (Map.Entry<String, Object> e : m.entrySet()) {
            if (!e.getValue().getClass().equals(VpcConnectionEntry.class)) {
                throw new CloudRuntimeException("only support value from a Map<string, VpcConnectionEntry>");
            }
            r.put(e.getKey(), valueOf((VpcConnectionEntry) e.getValue()));
        }
        return r;
    }

    public String getSourceL2NetworkType() {
        return sourceL2NetworkType;
    }

    public void setSourceL2NetworkType(String sourceL2NetworkType) {
        this.sourceL2NetworkType = sourceL2NetworkType;
    }

    public String getDestinationL2NetworkType() {
        return destinationL2NetworkType;
    }

    public void setDestinationL2NetworkType(String destinationL2NetworkType) {
        this.destinationL2NetworkType = destinationL2NetworkType;
    }

    public String getSourceMac() {
        return sourceMac;
    }

    public void setSourceMac(String sourceMac) {
        this.sourceMac = sourceMac;
    }

    public String getDestinationMac() {
        return destinationMac;
    }

    public void setDestinationMac(String destinationMac) {
        this.destinationMac = destinationMac;
    }

    public Integer getSourceL2NetworkVni() {
        return sourceL2NetworkVni;
    }

    public void setSourceL2NetworkVni(Integer sourceL2NetworkVni) {
        this.sourceL2NetworkVni = sourceL2NetworkVni;
    }

    public Integer getDestinationL2NetworkVni() {
        return destinationL2NetworkVni;
    }

    public void setDestinationL2NetworkVni(Integer destinationL2NetworkVni) {
        this.destinationL2NetworkVni = destinationL2NetworkVni;
    }

    public String getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(String lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
