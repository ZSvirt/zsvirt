package org.zstack.header.core.progress;

import org.apache.logging.log4j.ThreadContext;
import org.zstack.header.message.DocUtils;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Map;

import static org.zstack.header.Constants.THREAD_CONTEXT_TASK_NAME;

public class TaskProgressInventory {
    private String apiId;
    private String content;
    private Map<String, Object> opaque;
    private long createTime;
    private long lastOpTime;
    private long currentStep;
    private long totalStep;

    // for compatibility
    @Deprecated
    private String taskUuid;
    @Deprecated
    private String taskName;
    @Deprecated
    private String parentUuid;
    @Deprecated
    private String type = "Progress";
    @Deprecated
    private Long time;
    @Deprecated
    private String arguments;

    @SuppressWarnings("unchecked")
    public static TaskProgressInventory valueOf(TaskProgressVO vo) {
        TaskProgressInventory inv = new TaskProgressInventory();
        inv.setApiId(vo.getApiId());
        inv.setContent(vo.getContent());
        inv.setOpaque(JSONObjectUtil.rehashObject(vo.getOpaque() == null ? "{}" : vo.getOpaque(), Map.class));
        inv.setCreateTime(vo.getCreateTime());
        inv.setLastOpTime(vo.getLastOpTime());
        inv.setCurrentStep(vo.getCurrentStep());
        inv.setTotalStep(vo.getTotalStep());

        inv.setTaskUuid(String.format("%032d", vo.getId()));
        String taskName = ThreadContext.get(THREAD_CONTEXT_TASK_NAME);
        inv.setTaskName(taskName == null ? vo.getContent() : taskName);
        inv.setTime(inv.lastOpTime);
        return inv;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getOpaque() {
        return opaque;
    }

    public void setOpaque(Map<String, Object> opaque) {
        this.opaque = opaque;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastOpTime() {
        return lastOpTime;
    }

    public void setLastOpTime(long lastOpTime) {
        this.lastOpTime = lastOpTime;
    }

    public long getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(long currentStep) {
        this.currentStep = currentStep;
    }

    public long getTotalStep() {
        return totalStep;
    }

    public void setTotalStep(long totalStep) {
        this.totalStep = totalStep;
    }

    public String getTaskUuid() {
        return taskUuid;
    }

    public void setTaskUuid(String taskUuid) {
        this.taskUuid = taskUuid;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public static TaskProgressInventory __example__() {
        TaskProgressInventory inv = new TaskProgressInventory();
        inv.setApiId(DocUtils.createFixedUuid(VmInstanceEO.class));
        inv.setContent("instantiate-volume-d6c9714e779b46c799309e0ec51f831b-local-primary-storage-131fe6aecc6841358b5163eb0ad3677a: instantiate-volume-on-host");
        inv.setOpaque(null);
        inv.setCreateTime(DocUtils.date);
        inv.setLastOpTime(DocUtils.date);
        inv.setCurrentStep(0);
        inv.setTotalStep(1);
        return inv;
    }
}
