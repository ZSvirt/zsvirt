package org.zstack.network.service.virtualrouter;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.utils.DebugUtils;

/**
 * Created by frank on 7/31/2015.
 */
public class DefaultVirtualRouterOfferingSelector {
    private String offeringUuid;
    private String zoneUuid;
    private Boolean preferToBeDefault;
    private boolean created;

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }

    public String getOfferingUuid() {
        return offeringUuid;
    }

    public void setOfferingUuid(String offeringUuid) {
        this.offeringUuid = offeringUuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public Boolean isPreferToBeDefault() {
        return preferToBeDefault;
    }

    public void setPreferToBeDefault(Boolean preferToBeDefault) {
        this.preferToBeDefault = preferToBeDefault;
    }

    public void selectDefaultOffering() {
        DebugUtils.Assert(zoneUuid != null, "zoneUuid cannot be null");
        DebugUtils.Assert(offeringUuid != null, "offeringUuid cannot be null");

        new SQLBatch() {
            @Override
            protected void scripts() {
                VirtualRouterOfferingVO offering = findByUuid(offeringUuid, VirtualRouterOfferingVO.class);

                if (!Q.New(VirtualRouterOfferingVO.class).eq(VirtualRouterOfferingVO_.zoneUuid, zoneUuid)
                        .notEq(VirtualRouterOfferingVO_.uuid, offeringUuid).isExists()
                        && created) {
                    // the first offering is always the default one
                    offering.setDefault(true);
                    merge(offering);
                    return;
                }

                if (preferToBeDefault != null && preferToBeDefault) {
                    sql(VirtualRouterOfferingVO.class).set(VirtualRouterOfferingVO_.isDefault, false)
                            .eq(VirtualRouterOfferingVO_.zoneUuid, zoneUuid).update();
                    reload(offering);
                    offering.setDefault(true);
                    merge(offering);

                } else if (preferToBeDefault != null) {
                    offering.setDefault(false);
                    merge(offering);
                }

                flush();
            }
        }.execute();
    }
}
