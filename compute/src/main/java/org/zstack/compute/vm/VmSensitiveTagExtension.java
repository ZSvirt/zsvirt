package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.tag.SystemTagCopyExtensionPoint;
import org.zstack.header.tag.SystemTagPersistExtensionPoint;
import org.zstack.header.tag.SystemTagTokenDecryptExtensionPoint;
import org.zstack.header.vm.VmInstanceVO;

public class VmSensitiveTagExtension implements SystemTagPersistExtensionPoint,
        SystemTagTokenDecryptExtensionPoint, SystemTagCopyExtensionPoint {
    @Autowired
    private VmSensitiveTagEncryptor encryptor;

    @Override
    public String beforePersist(String resourceUuid, String resourceType, String tag) {
        if (!VmInstanceVO.class.getSimpleName().equals(resourceType)) {
            return tag;
        }
        return encryptor.encryptTagIfNeeded(resourceUuid, tag);
    }

    @Override
    public String decryptTokenValue(String resourceType, String tagHead, String tokenName, String tokenValue) {
        return encryptor.decryptTokenValue(resourceType, tagHead, tokenName, tokenValue);
    }

    @Override
    public String transformTagForCopy(String srcResourceUuid, String srcResourceType,
                                      String dstResourceUuid, String dstResourceType, String srcTag) {
        return encryptor.transformTagForCopy(srcResourceUuid, srcResourceType, dstResourceUuid, dstResourceType, srcTag);
    }
}
