package org.zstack.header.longjob;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APILongJobAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vo.ResourceVO;
import org.zstack.utils.DebugUtils;

/**
 * Created by GuoYi on 11/13/17.
 */
@TagResourceType(LongJobVO.class)
@RestRequest(
        path = "/longjobs",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APISubmitLongJobEvent.class
)
public class APISubmitLongJobMsg extends APICreateMessage implements APILongJobAuditor {
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(maxLength = 255)
    private String jobName;
    @APIParam
    private String jobData;
    @APIParam(maxLength = 32, required = false, resourceType = ResourceVO.class)
    private String targetResourceUuid;

    @APINoSee
    private String jobUuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    public static APISubmitLongJobMsg __example__() {
        APISubmitLongJobMsg msg = new APISubmitLongJobMsg();
        msg.setName("migrate-volume");
        msg.setDescription("migrate volume to another Ceph primary storage");
        msg.setJobName("APIPrimaryStorageMigrateVolumeMsg");
        msg.setJobData("{\"volumeUuid\":\"45a53d3d93384433add8ead7616586cf\", \"dstPrimaryStorageUuid\":\"70a0618804864b3dabe8be9824c8028c\"}");
        msg.setTargetResourceUuid("45a53d3d93384433add8ead7616586cf");
        return msg;
    }

    private String getResourceUuid(LongJob job) {
        return job.getAuditResourceUuid() == null ? "" : job.getAuditResourceUuid();
    }

    @Override
    public APIAuditor.Result longJobAudit(LongJob job, LongJobVO vo, APIEvent rsp) {
        Class resourceType = job.getAuditType();
        DebugUtils.Assert(resourceType != null, String.format("resourceType mustn't be null from longjob: %s", job.getClass().getSimpleName()));

        return new APIAuditor.Result(getResourceUuid(job) != null ? getResourceUuid(job) : vo.getTargetResourceUuid(), resourceType);
    }
}
