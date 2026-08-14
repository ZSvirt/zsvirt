package org.zstack.storage.primary.license;

import java.util.List;

public class XskyLicenseReturnStruct {
    private List<License> licenses;

    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    public List<License> getLicenses() {
        return licenses;
    }

    public class License {
        private boolean active;
        private String expired_time;

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean getActive() {
            return active;
        }

        public String getExpired_time() {
            return expired_time;
        }

        public void setExpired_time(String expired_time) {
            this.expired_time = expired_time;
        }
    }
}