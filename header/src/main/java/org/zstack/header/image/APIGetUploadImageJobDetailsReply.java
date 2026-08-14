package org.zstack.header.image;

import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.data.SizeUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2021/3/29.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetUploadImageJobDetailsReply extends APIReply {
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

    public static class JobDetails {
        private String longJobUuid;
        private String longJobState;
        private String imageUuid;
        private String imageUploadUrl;
        private long offset;

        public String getLongJobUuid() {
            return longJobUuid;
        }

        public void setLongJobUuid(String longJobUuid) {
            this.longJobUuid = longJobUuid;
        }

        public String getLongJobState() {
            return longJobState;
        }

        public void setLongJobState(String longJobState) {
            this.longJobState = longJobState;
        }

        public String getImageUploadUrl() {
            return imageUploadUrl;
        }

        public void setImageUploadUrl(String imageUploadUrl) {
            this.imageUploadUrl = imageUploadUrl;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public long getOffset() {
            return offset;
        }

        public void setOffset(long offset) {
            this.offset = offset;
        }
    }

    public static APIGetUploadImageJobDetailsReply __example__() {
        APIGetUploadImageJobDetailsReply reply = new APIGetUploadImageJobDetailsReply();
        JobDetails detail = new JobDetails();
        detail.imageUuid = uuid();
        detail.imageUploadUrl = "http://127.0.0.1:8001/imagestore/upload";
        detail.longJobUuid = uuid();
        detail.longJobState = LongJobState.Suspended.toString();
        detail.offset = SizeUnit.MEGABYTE.toByte(16) * 27;
        reply.existingJobDetails = Collections.singletonList(detail);
        return reply;
    }
}