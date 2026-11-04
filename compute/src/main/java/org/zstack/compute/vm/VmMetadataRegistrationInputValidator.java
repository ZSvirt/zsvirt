package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.vm.metadata.VmInstanceMetadataDTO;
import org.zstack.utils.gson.JSONObjectUtil;

import java.nio.charset.StandardCharsets;

public final class VmMetadataRegistrationInputValidator {
    private VmMetadataRegistrationInputValidator() {
    }

    public static void validate(String metadataPath, String metadata, long payloadLimit) {
        if ((metadataPath != null) == (metadata != null)) {
            throw new IllegalArgumentException("exactly one of metadataPath or metadata must be provided");
        }
        if (metadataPath != null) {
            if (StringUtils.isBlank(metadataPath)) {
                throw new IllegalArgumentException("metadataPath cannot be blank");
            }
            return;
        }
        if (StringUtils.isBlank(metadata)) {
            throw new IllegalArgumentException("metadata cannot be blank");
        }
        int size = metadata.getBytes(StandardCharsets.UTF_8).length;
        if (size > payloadLimit) {
            throw new IllegalArgumentException(String.format(
                    "metadata payload size[%s bytes] exceeds threshold[%s bytes]", size, payloadLimit));
        }
        VmInstanceMetadataDTO dto;
        try {
            dto = JSONObjectUtil.toObject(metadata, VmInstanceMetadataDTO.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("metadata must be valid VM metadata JSON", e);
        }
        if (dto == null || dto.getVm() == null || StringUtils.isBlank(dto.getVm().getResourceUuid())) {
            throw new IllegalArgumentException("metadata must contain a VM resourceUuid");
        }
    }
}
