package org.zstack.header.keyprovider;

import org.zstack.header.message.NeedJsonSchema;

public class KeyProviderCanonicalEvents {
    public static final String KEY_PROVIDER_DEFAULT_SERVICE_UNAVAILABLE_PATH = "/keyProvider/default/service/unavailable";
    public static final String KEY_PROVIDER_DEFAULT_SERVICE_RECOVERED_PATH = "/keyProvider/default/service/recovered";
    public static final String KEY_PROVIDER_RESOURCE_SERVICE_UNAVAILABLE_PATH = "/keyProvider/resource/service/unavailable";
    public static final String KEY_PROVIDER_RESOURCE_SERVICE_RECOVERED_PATH = "/keyProvider/resource/service/recovered";
    public static final String KEY_PROVIDER_CERTIFICATE_EXPIRING_PATH = "/keyProvider/certificate/expiring";
    public static final String KEY_PROVIDER_CERTIFICATE_RECOVERED_PATH = "/keyProvider/certificate/recovered";

    @NeedJsonSchema
    public static class KeyProviderServiceUnavailableData {
        private String keyProviderUuid;
        private String keyProviderName;
        private String keyProviderType;
        private String statusCode;
        private String statusMessage;
        private String defaultProvider;

        public String getKeyProviderUuid() {
            return keyProviderUuid;
        }

        public void setKeyProviderUuid(String keyProviderUuid) {
            this.keyProviderUuid = keyProviderUuid;
        }

        public String getKeyProviderName() {
            return keyProviderName;
        }

        public void setKeyProviderName(String keyProviderName) {
            this.keyProviderName = keyProviderName;
        }

        public String getKeyProviderType() {
            return keyProviderType;
        }

        public void setKeyProviderType(String keyProviderType) {
            this.keyProviderType = keyProviderType;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(String statusCode) {
            this.statusCode = statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public void setStatusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
        }

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }
    }

    @NeedJsonSchema
    public static class KeyProviderServiceRecoveredData {
        private String keyProviderUuid;
        private String keyProviderName;
        private String keyProviderType;
        private String statusCode;
        private String statusMessage;
        private String defaultProvider;

        public String getKeyProviderUuid() {
            return keyProviderUuid;
        }

        public void setKeyProviderUuid(String keyProviderUuid) {
            this.keyProviderUuid = keyProviderUuid;
        }

        public String getKeyProviderName() {
            return keyProviderName;
        }

        public void setKeyProviderName(String keyProviderName) {
            this.keyProviderName = keyProviderName;
        }

        public String getKeyProviderType() {
            return keyProviderType;
        }

        public void setKeyProviderType(String keyProviderType) {
            this.keyProviderType = keyProviderType;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(String statusCode) {
            this.statusCode = statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public void setStatusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
        }

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }
    }

    @NeedJsonSchema
    public static class KeyProviderCertificateExpiringData {
        private String keyProviderUuid;
        private String keyProviderName;
        private String keyProviderType;
        private String certificateType;
        private String expiredDate;
        private String daysLeft;
        private String reportDate;
        private String defaultProvider;

        public String getKeyProviderUuid() {
            return keyProviderUuid;
        }

        public void setKeyProviderUuid(String keyProviderUuid) {
            this.keyProviderUuid = keyProviderUuid;
        }

        public String getKeyProviderName() {
            return keyProviderName;
        }

        public void setKeyProviderName(String keyProviderName) {
            this.keyProviderName = keyProviderName;
        }

        public String getKeyProviderType() {
            return keyProviderType;
        }

        public void setKeyProviderType(String keyProviderType) {
            this.keyProviderType = keyProviderType;
        }

        public String getCertificateType() {
            return certificateType;
        }

        public void setCertificateType(String certificateType) {
            this.certificateType = certificateType;
        }

        public String getExpiredDate() {
            return expiredDate;
        }

        public void setExpiredDate(String expiredDate) {
            this.expiredDate = expiredDate;
        }

        public String getDaysLeft() {
            return daysLeft;
        }

        public void setDaysLeft(String daysLeft) {
            this.daysLeft = daysLeft;
        }

        public String getReportDate() {
            return reportDate;
        }

        public void setReportDate(String reportDate) {
            this.reportDate = reportDate;
        }

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }
    }

    @NeedJsonSchema
    public static class KeyProviderCertificateRecoveredData {
        private String keyProviderUuid;
        private String keyProviderName;
        private String keyProviderType;
        private String certificateType;
        private String expiredDate;
        private String daysLeft;
        private String reportDate;
        private String defaultProvider;

        public String getKeyProviderUuid() {
            return keyProviderUuid;
        }

        public void setKeyProviderUuid(String keyProviderUuid) {
            this.keyProviderUuid = keyProviderUuid;
        }

        public String getKeyProviderName() {
            return keyProviderName;
        }

        public void setKeyProviderName(String keyProviderName) {
            this.keyProviderName = keyProviderName;
        }

        public String getKeyProviderType() {
            return keyProviderType;
        }

        public void setKeyProviderType(String keyProviderType) {
            this.keyProviderType = keyProviderType;
        }

        public String getCertificateType() {
            return certificateType;
        }

        public void setCertificateType(String certificateType) {
            this.certificateType = certificateType;
        }

        public String getExpiredDate() {
            return expiredDate;
        }

        public void setExpiredDate(String expiredDate) {
            this.expiredDate = expiredDate;
        }

        public String getDaysLeft() {
            return daysLeft;
        }

        public void setDaysLeft(String daysLeft) {
            this.daysLeft = daysLeft;
        }

        public String getReportDate() {
            return reportDate;
        }

        public void setReportDate(String reportDate) {
            this.reportDate = reportDate;
        }

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }
    }

}
