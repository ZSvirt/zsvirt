package org.zstack.zwatch.thirdparty.entity;

import java.io.Serializable;

public class CompositePrimaryKeyForSNSEndpointThirdpartyAlertHistoryVO implements Serializable {
    private String alertUuid;
    private String endpointUuid;

    public String getAlertUuid() {
        return alertUuid;
    }

    public void setAlertUuid(String alertUuid) {
        this.alertUuid = alertUuid;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }

    @Override
    public int hashCode() {
        return (alertUuid + endpointUuid).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        CompositePrimaryKeyForSNSEndpointThirdpartyAlertHistoryVO other = (CompositePrimaryKeyForSNSEndpointThirdpartyAlertHistoryVO) obj;
        if (alertUuid == null) {
            if (other.alertUuid != null) {
                return false;
            }
        } else if (!alertUuid.equals(other.alertUuid)) {
            return false;
        }
        if (endpointUuid == null) {
            if (other.endpointUuid != null) {
                return false;
            }
        } else if (!endpointUuid.equals(other.endpointUuid)) {
            return false;
        }

        return true;
    }
}
