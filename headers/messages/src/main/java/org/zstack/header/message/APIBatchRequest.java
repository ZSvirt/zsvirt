package org.zstack.header.message;

public interface APIBatchRequest {
    class Result {
        private int totalCount;
        private int successCount;

        public Result() {
        }

        public Result(int totalCount, int successCount) {
            this.totalCount = totalCount;
            this.successCount = successCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }
    }

    APIBatchRequest.Result collectResult(APIMessage message, APIEvent rsp);
}
