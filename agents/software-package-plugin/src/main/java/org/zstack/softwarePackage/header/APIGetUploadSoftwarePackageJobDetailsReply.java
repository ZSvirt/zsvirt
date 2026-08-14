package org.zstack.softwarePackage.header;

import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.utils.data.SizeUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetUploadSoftwarePackageJobDetailsReply extends APIReply {
    private List<JobDetails> existingJobDetails;

    public List<JobDetails> getExistingJobDetails() {
        return existingJobDetails;
    }

    public void setExistingJobDetails(List<JobDetails> existingJobDetails) {
        this.existingJobDetails = existingJobDetails;
    }

    public void addExistingJobDetails(JobDetails detail) {
        if (existingJobDetails == null) {
            existingJobDetails = new ArrayList<>();
        }
        this.existingJobDetails.add(detail);
    }

    public static APIGetUploadSoftwarePackageJobDetailsReply __example__() {
        APIGetUploadSoftwarePackageJobDetailsReply reply = new APIGetUploadSoftwarePackageJobDetailsReply();
        JobDetails detail = new JobDetails();
        detail.setSoftwarePackageUuid(uuid(SoftwarePackageVO.class));
        detail.setSoftwarePackageUploadUrl("http://127.0.0.1:8001/host/file/upload");
        detail.setLongJobUuid(uuid(LongJobVO.class));
        detail.setLongJobState(LongJobState.Running.toString());
        detail.setOffset(SizeUnit.MEGABYTE.toByte(16) * 27);
        reply.setExistingJobDetails(new ArrayList<>(Collections.singletonList(detail)));
        return reply;
    }
}