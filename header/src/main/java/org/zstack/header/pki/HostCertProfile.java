package org.zstack.header.pki;

import java.util.Collections;
import java.util.List;

public class HostCertProfile {
    private String usage;
    private String subjectDn;
    private List<String> subjectAlternativeNames = Collections.emptyList();
    private List<String> roles = Collections.emptyList();
    private int validityDays = 365;

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }

    public List<String> getSubjectAlternativeNames() {
        return subjectAlternativeNames;
    }

    public void setSubjectAlternativeNames(List<String> subjectAlternativeNames) {
        this.subjectAlternativeNames = subjectAlternativeNames == null ? Collections.emptyList() : subjectAlternativeNames;
    }

    public int getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(int validityDays) {
        this.validityDays = validityDays;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles == null ? Collections.emptyList() : roles;
    }
}
