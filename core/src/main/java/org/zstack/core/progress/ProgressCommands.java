package org.zstack.core.progress;

import java.util.List;
import java.util.Map;

/**
 * Created by mingjian.deng on 16/12/10.
 */
public class ProgressCommands {
    public static class AgentResponse {
        private boolean success = true;
        private String error;
        public boolean isSuccess() {
            return success;
        }
        public void setSuccess(boolean success) {
            this.success = success;
        }
        public String getError() {
            return error;
        }
        public void setError(String error) {
            this.error = error;
        }
    }

    public static class ProgressReportCmd {
        private Map<String, String> threadContextMap;
        private List<String> threadContextStack;
        private String serverUuid;
        private String serverType;
        private String resourceUuid;
        private String progress;
        private String processType;
        private Map<String, String> detail;

        public Map<String, String> getDetail() {
            return detail;
        }

        public void setDetail(Map<String, String> detail) {
            this.detail = detail;
        }

        public Map<String, String> getThreadContextMap() {
            return threadContextMap;
        }

        public void setThreadContextMap(Map<String, String> threadContextMap) {
            this.threadContextMap = threadContextMap;
        }

        public List<String> getThreadContextStack() {
            return threadContextStack;
        }

        public void setThreadContextStack(List<String> threadContextStack) {
            this.threadContextStack = threadContextStack;
        }

        public String getServerUuid() {
            return serverUuid;
        }

        public void setServerUuid(String serverUuid) {
            this.serverUuid = serverUuid;
        }

        public String getServerType() {
            return serverType;
        }

        public void setServerType(String serverType) {
            this.serverType = serverType;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public String getProgress() {
            return progress;
        }

        public void setProgress(String progress) {
            this.progress = progress;
        }

        public String getProcessType() {
            return processType;
        }

        public void setProcessType(String processType) {
            this.processType = processType;
        }
    }

    public static class ProgressReportResponse extends AgentResponse {
    }
}
