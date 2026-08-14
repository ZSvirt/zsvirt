package org.zstack.header.core;

public interface I18nMessage {
    String getDetails();
    String getI18nDetails();

    class DetailRecord implements I18nMessage {
        public final String details;
        public final String i18nDetails;

        private DetailRecord(String details, String i18nDetails) {
            this.details = details;
            this.i18nDetails = i18nDetails;
        }

        @Override
        public String toString() {
            return details;
        }

        @Override
        public String getDetails() {
            return details;
        }

        @Override
        public String getI18nDetails() {
            return i18nDetails;
        }
    }

    static DetailRecord valueOf(String details, String i18nDetails) {
        return new DetailRecord(details, i18nDetails);
    }
}
