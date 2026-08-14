package org.zstack.appliancevm;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: shixin.ruan
 * Time: 2019/05/12
 * To change this template use File | Settings | File Templates.
 */
public class ApplianceVmHaSpec implements Serializable {
    private String haUuid;
    private String affinityGroupUuid;

    public String getHaUuid() {
        return haUuid;
    }

    public void setHaUuid(String haUuid) {
        this.haUuid = haUuid;
    }

    public String getAffinityGroupUuid() {
        return affinityGroupUuid;
    }

    public void setAffinityGroupUuid(String affinityGroupUuid) {
        this.affinityGroupUuid = affinityGroupUuid;
    }
}
