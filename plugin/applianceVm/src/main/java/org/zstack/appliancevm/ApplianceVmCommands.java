package org.zstack.appliancevm;

import org.zstack.header.agent.AgentCommand;
import org.zstack.header.agent.AgentResponse;

import java.util.List;

/**
 */
public class ApplianceVmCommands {
    public static class RefreshFirewallCmd extends AgentCommand {
        private List<ApplianceVmFirewallRuleTO> rules;

        public List<ApplianceVmFirewallRuleTO> getRules() {
            return rules;
        }

        public void setRules(List<ApplianceVmFirewallRuleTO> rules) {
            this.rules = rules;
        }
    }

    public static class RefreshFirewallRsp extends AgentResponse {
    }

    public static class InitCmd extends AgentCommand {
    }

    public static class InitRsp extends AgentResponse {
    }

    public static class ApplianceVmAbnormalFilesCmd extends AgentCommand {
        private String applianceVmUuid;
        private List<AbnormalFile> abnormalFiles;
        private String diskTotal;
        private String diskUsed;
        private String diskUsedutilization;

        public String getApplianceVmUuid() {
            return applianceVmUuid;
        }

        public void setApplianceVmUuid(String virtualRouterUuid) {
            this.applianceVmUuid = virtualRouterUuid;
        }

        static class AbnormalFile {
            private String filePath;
            private String fileSize;

            public String getFilePath() {
                return filePath;
            }

            public void setFilePath(String filePath) {
                this.filePath = filePath;
            }

            public String getFileSize() {
                return fileSize;
            }

            public void setFileSize(String fileSize) {
                this.fileSize = fileSize;
            }
        }

        public List<AbnormalFile> getAbnormalFiles() {
            return abnormalFiles;
        }

        public void setAbnormalFiles(List<AbnormalFile> abnormalFiles) {
            this.abnormalFiles = abnormalFiles;
        }

        public String getDiskTotal() {
            return diskTotal;
        }

        public void setDiskTotal(String diskTotal) {
            this.diskTotal = diskTotal;
        }

        public String getDiskUsed() {
            return diskUsed;
        }

        public void setDiskUsed(String diskUsed) {
            this.diskUsed = diskUsed;
        }

        public String getDiskUsedutilization() {
            return diskUsedutilization;
        }

        public void setDiskUsedutilization(String diskUsedutilization) {
            this.diskUsedutilization = diskUsedutilization;
        }
    }
}
