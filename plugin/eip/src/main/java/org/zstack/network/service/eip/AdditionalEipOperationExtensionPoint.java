package org.zstack.network.service.eip;

public interface AdditionalEipOperationExtensionPoint {
    void preAttachEip(EipStruct struct);

    EipStruct getAdditionalEipStruct(EipStruct struct);
}
