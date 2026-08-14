package org.zstack.sns;

/**
 * Created by Qi Le on 2019-07-15
 */
public class SNSCanonicalEvents {
    public static final String SEND_SMS_FAILED_PATH = "/sns/send-sms/failed";
    public static final String UPDATE_SNS_GLOBAL_PROPERTY_PATH = "/sns/global-property/updated";

    public static class SNSSendSmsFailedData {
        private String phoneNumber;
        private String endpointUuid;
        private String errCode;
        private String errMessage;
//        private SNSSmsEndpointInventory inventory;

        public String getErrCode() {
            return errCode;
        }

        public void setErrCode(String errCode) {
            this.errCode = errCode;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getErrMessage() {
            return errMessage;
        }

        public void setErrMessage(String errMessage) {
            this.errMessage = errMessage;
        }

//        public SNSSmsEndpointInventory getInventory() {
//            return inventory;
//        }
//
//        public void setInventory(SNSSmsEndpointInventory inventory) {
//            this.inventory = inventory;
//        }

        public String getEndpointUuid() {
            return endpointUuid;
        }

        public void setEndpointUuid(String endpointUuid) {
            this.endpointUuid = endpointUuid;
        }
    }
}
