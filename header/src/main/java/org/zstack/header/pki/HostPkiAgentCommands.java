package org.zstack.header.pki;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HostPkiAgentCommands {
    public static class GenerateCsrCmd {
        private String usage;
        private String subject;
        private List<String> sanList = Collections.emptyList();
        private String keyAlgorithm;
        private List<String> roles = Collections.emptyList();

        public String getUsage() {
            return usage;
        }

        public void setUsage(String usage) {
            this.usage = usage;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public List<String> getSanList() {
            return sanList;
        }

        public void setSanList(List<String> sanList) {
            this.sanList = sanList == null ? Collections.emptyList() : sanList;
        }

        public String getKeyAlgorithm() {
            return keyAlgorithm;
        }

        public void setKeyAlgorithm(String keyAlgorithm) {
            this.keyAlgorithm = keyAlgorithm;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles == null ? Collections.emptyList() : roles;
        }
    }

    public static class GenerateCsrResponse {
        private boolean success = true;
        private String error;
        private Map<String, String> csrPemByRole;
        private Map<String, String> publicKeyFingerprintByRole;

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
            this.success = false;
        }

        public Map<String, String> getCsrPemByRole() {
            return csrPemByRole;
        }

        public void setCsrPemByRole(Map<String, String> csrPemByRole) {
            this.csrPemByRole = csrPemByRole;
        }

        public Map<String, String> getPublicKeyFingerprintByRole() {
            return publicKeyFingerprintByRole;
        }

        public void setPublicKeyFingerprintByRole(Map<String, String> publicKeyFingerprintByRole) {
            this.publicKeyFingerprintByRole = publicKeyFingerprintByRole;
        }
    }

    public static class InstallCmd {
        private String usage;
        private Map<String, String> certPemByRole;
        private String caChainPem;
        private String crlPem;
        private Map<String, String> expectedFingerprintByRole;

        public String getUsage() {
            return usage;
        }

        public void setUsage(String usage) {
            this.usage = usage;
        }

        public Map<String, String> getCertPemByRole() {
            return certPemByRole;
        }

        public void setCertPemByRole(Map<String, String> certPemByRole) {
            this.certPemByRole = certPemByRole;
        }

        public String getCaChainPem() {
            return caChainPem;
        }

        public void setCaChainPem(String caChainPem) {
            this.caChainPem = caChainPem;
        }

        public String getCrlPem() {
            return crlPem;
        }

        public void setCrlPem(String crlPem) {
            this.crlPem = crlPem;
        }

        public Map<String, String> getExpectedFingerprintByRole() {
            return expectedFingerprintByRole;
        }

        public void setExpectedFingerprintByRole(Map<String, String> expectedFingerprintByRole) {
            this.expectedFingerprintByRole = expectedFingerprintByRole;
        }
    }

    public static class InstallResponse {
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
            this.success = false;
        }
    }

    public static class StatusCmd {
        private String usage;

        public String getUsage() {
            return usage;
        }

        public void setUsage(String usage) {
            this.usage = usage;
        }
    }

    public static class StatusResponse {
        private boolean success = true;
        private String error;
        private boolean ready;
        private String fingerprint;
        private String notAfter;
        private List<String> sanList;
        private String path;

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
            this.success = false;
        }

        public boolean isReady() {
            return ready;
        }

        public void setReady(boolean ready) {
            this.ready = ready;
        }

        public String getFingerprint() {
            return fingerprint;
        }

        public void setFingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
        }

        public String getNotAfter() {
            return notAfter;
        }

        public void setNotAfter(String notAfter) {
            this.notAfter = notAfter;
        }

        public List<String> getSanList() {
            return sanList;
        }

        public void setSanList(List<String> sanList) {
            this.sanList = sanList;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class RevokeLocalCmd {
        private String usage;

        public String getUsage() {
            return usage;
        }

        public void setUsage(String usage) {
            this.usage = usage;
        }
    }

    public static class RevokeLocalResponse extends InstallResponse {
    }
}
