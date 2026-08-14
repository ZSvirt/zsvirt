package org.zstack.zwatch.namespace.event;

import org.zstack.header.message.NeedJsonSchema;

public class ThirdpartyAlertCanonicalEvents {
    public static final String THIRDPARTY_ALERT_PATH = "/thirdparty/alert";

    @NeedJsonSchema
    public static class ThirdpartyAlertData {
        private String uuid;
        private String platformUuid;
        private String product;
        private String service;
        private String metric;
        private String alertLevel;
        private String alertTime;
        private String dimensions;
        private String message;
        private String dataSource;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getMetric() {
            return metric;
        }

        public void setMetric(String metric) {
            this.metric = metric;
        }

        public String getAlertLevel() {
            return alertLevel;
        }

        public void setAlertLevel(String alertLevel) {
            this.alertLevel = alertLevel;
        }

        public String getAlertTime() {
            return alertTime;
        }

        public void setAlertTime(String alertTime) {
            this.alertTime = alertTime;
        }

        public String getDimensions() {
            return dimensions;
        }

        public void setDimensions(String dimensions) {
            this.dimensions = dimensions;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getDataSource() {
            return dataSource;
        }

        public void setDataSource(String dataSource) {
            this.dataSource = dataSource;
        }

        public String getPlatformUuid() {
            return platformUuid;
        }

        public void setPlatformUuid(String platformUuid) {
            this.platformUuid = platformUuid;
        }
    }
}
