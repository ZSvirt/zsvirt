package org.zstack.softwarePackage.header;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIUploadSoftwarePackageToVmEvent extends APIEvent {
    private String uploadTaskUuid;
    @NoLogging(type = NoLogging.Type.Uri)
    private String uploadUrl;

    public APIUploadSoftwarePackageToVmEvent() {
    }

    public APIUploadSoftwarePackageToVmEvent(String apiId) {
        super(apiId);
    }

    public String getUploadTaskUuid() {
        return uploadTaskUuid;
    }

    public void setUploadTaskUuid(String uploadTaskUuid) {
        this.uploadTaskUuid = uploadTaskUuid;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public static APIUploadSoftwarePackageToVmEvent __example__() {
        APIUploadSoftwarePackageToVmEvent event = new APIUploadSoftwarePackageToVmEvent();
        event.setUploadTaskUuid(uuid());
        event.setUploadUrl("http://192.168.0.10:7070/host/file/direct/upload");
        return event;
    }
}
