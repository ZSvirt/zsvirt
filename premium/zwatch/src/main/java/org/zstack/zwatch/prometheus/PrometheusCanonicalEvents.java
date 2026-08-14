package org.zstack.zwatch.prometheus;

public class PrometheusCanonicalEvents {
    public static final String STATIC_CONFIG_WRITE = "/prometheus/static-config/write";

    public static class StaticConfigWriteData {
        private String uuid;
        private String ip;
        private String resourceType;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }
    }
}
