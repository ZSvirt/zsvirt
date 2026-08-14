package org.zstack.header.volume;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

/**
 */
@RestResponse(allTo = "formats")
public class APIGetVolumeFormatReply extends APIReply {
    public static class VolumeFormatReplyStruct {
        private String format;
        private String masterHypervisorType;
        private List<String> supportingHypervisorTypes;

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getMasterHypervisorType() {
            return masterHypervisorType;
        }

        public void setMasterHypervisorType(String masterHypervisorType) {
            this.masterHypervisorType = masterHypervisorType;
        }

        public List<String> getSupportingHypervisorTypes() {
            return supportingHypervisorTypes;
        }

        public void setSupportingHypervisorTypes(List<String> supportingHypervisorTypes) {
            this.supportingHypervisorTypes = supportingHypervisorTypes;
        }

        public VolumeFormatReplyStruct(VolumeFormat vformat) {
            format = vformat.toString();
            masterHypervisorType = vformat.getMasterHypervisorType() == null ? null : vformat.getMasterHypervisorType().toString();
            supportingHypervisorTypes = vformat.getHypervisorTypesSupportingThisVolumeFormatInString();
        }

        public VolumeFormatReplyStruct() {
        }
    }

    private List<VolumeFormatReplyStruct> formats;

    public List<VolumeFormatReplyStruct> getFormats() {
        return formats;
    }

    public void setFormats(List<VolumeFormatReplyStruct> formats) {
        this.formats = formats;
    }
 
    public static APIGetVolumeFormatReply __example__() {
        APIGetVolumeFormatReply reply = new APIGetVolumeFormatReply();
        VolumeFormatReplyStruct struct = new VolumeFormatReplyStruct();
        struct.setFormat("iso");
        struct.setMasterHypervisorType("KVM");
        List<String> supportingHypervisorTypes = new ArrayList<>();
        supportingHypervisorTypes.add("ESX");
        supportingHypervisorTypes.add("KVM");
        struct.setSupportingHypervisorTypes(supportingHypervisorTypes);

        List<VolumeFormatReplyStruct> structs = new ArrayList<>();
        structs.add(struct);

        reply.setFormats(structs);

        return reply;
    }

}
