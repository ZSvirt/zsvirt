package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wenhao.Zhang on 23/10/16
 */
public class UpdateVmOnHypervisorReply extends MessageReply {
    private Integer cpuUpdatedTo;
    private Long memoryUpdatedTo;
    private String nameUpdatedTo;
    private final List<ErrorCode> ignoredErrors = new ArrayList<>();

    public boolean isCpuUpdated() {
        return cpuUpdatedTo != null;
    }

    public boolean isMemoryUpdated() {
        return memoryUpdatedTo != null;
    }

    public boolean isNameUpdated() {
        return nameUpdatedTo != null;
    }

    public boolean hasAnythingUpdated() {
        return isCpuUpdated() || isMemoryUpdated() || isNameUpdated();
    }

    public Integer getCpuUpdatedTo() {
        return cpuUpdatedTo;
    }

    public void setCpuUpdatedTo(Integer cpuUpdatedTo) {
        this.cpuUpdatedTo = cpuUpdatedTo;
    }

    public Long getMemoryUpdatedTo() {
        return memoryUpdatedTo;
    }

    public void setMemoryUpdatedTo(Long memoryUpdatedTo) {
        this.memoryUpdatedTo = memoryUpdatedTo;
    }

    public String getNameUpdatedTo() {
        return nameUpdatedTo;
    }

    public void setNameUpdatedTo(String nameUpdatedTo) {
        this.nameUpdatedTo = nameUpdatedTo;
    }

    public List<ErrorCode> getIgnoredErrors() {
        return ignoredErrors;
    }
}
