package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.encrypt.EncryptFacade;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmCanonicalEvents;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Encrypts VM sensitive system tags (consolePassword / sshkey / userdata) when vmEncryption is on.
 * Uses the platform-global EncryptFacade key; ciphertext is not bound to a specific VM uuid.
 */
public class VmSensitiveTagEncryptor {
    private static final CLogger logger = Utils.getLogger(VmSensitiveTagEncryptor.class);

    public static final String ENC_PREFIX = "ENC:";
    public static final int MAX_TAG_LENGTH = 65535;

    @Autowired
    private EncryptFacade encryptFacade;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;

    private static final class SensitiveTagContext {
        private final PatternedSystemTag patternedTag;
        private final String tagHead;
        private final Map<String, String> tokens;

        private SensitiveTagContext(PatternedSystemTag patternedTag, String tagHead, Map<String, String> tokens) {
            this.patternedTag = patternedTag;
            this.tagHead = tagHead;
            this.tokens = new HashMap<>(tokens);
        }

        private String tokenValue() {
            return tokens.get(tagHead);
        }

        private String withTokenValue(String value) {
            tokens.put(tagHead, value);
            return patternedTag.instantiateTag(tokens);
        }
    }

    public boolean isVmEncryptionEnabled(String vmUuid) {
        return VmSystemTags.VM_ENCRYPTION.hasTag(vmUuid, VmInstanceVO.class);
    }

    public boolean isSensitiveVmTag(String tag) {
        return VmSystemTags.isSensitiveVmTag(tag);
    }

    public boolean isEncryptedValue(String value) {
        if (value == null || !value.startsWith(ENC_PREFIX)) {
            return false;
        }
        String ciphertext = value.substring(ENC_PREFIX.length());
        String decrypted = encryptFacade.decrypt(ciphertext);
        return !ciphertext.equals(decrypted);
    }

    public String decryptTokenValue(String resourceType, String tagHead, String tokenName, String tokenValue) {
        if (!VmInstanceVO.class.getSimpleName().equals(resourceType)) {
            return tokenValue;
        }
        if (!VmSystemTags.isSensitiveTagHead(tagHead)) {
            return tokenValue;
        }
        if (!tokenName.equals(tagHead)) {
            return tokenValue;
        }
        return decryptRawTokenValue(tokenValue);
    }

    public String decryptRawTokenValue(String tokenValue) {
        if (!isEncryptedValue(tokenValue)) {
            return tokenValue;
        }
        return encryptFacade.decrypt(tokenValue.substring(ENC_PREFIX.length()));
    }

    public String transformTagForCopy(String srcResourceUuid, String srcResourceType,
                                      String dstResourceUuid, String dstResourceType, String srcTag) {
        if (!VmInstanceVO.class.getSimpleName().equals(srcResourceType)
                || !VmInstanceVO.class.getSimpleName().equals(dstResourceType)) {
            return srcTag;
        }
        if (!isSensitiveVmTag(srcTag)) {
            return srcTag;
        }

        SensitiveTagContext ctx = parseSensitiveTag(srcTag);
        if (ctx == null) {
            return srcTag;
        }

        String rawValue = ctx.tokenValue();
        if (rawValue == null) {
            return srcTag;
        }

        String plainValue = decryptRawTokenValue(rawValue);
        if (isVmEncryptionEnabled(dstResourceUuid)) {
            String encryptedValue = encryptValueForTag(ctx.tagHead, plainValue, dstResourceUuid);
            return ctx.withTokenValue(encryptedValue != null ? encryptedValue : plainValue);
        }
        return ctx.withTokenValue(plainValue);
    }

    public String encryptTagIfNeeded(String vmUuid, String tag) {
        if (tag == null || !isSensitiveVmTag(tag)) {
            return tag;
        }
        if (!isVmEncryptionEnabled(vmUuid)) {
            return tag;
        }

        return encryptPlainTagForPersist(vmUuid, tag);
    }

    private String encryptExistingDbTagIfNeeded(String vmUuid, String tag) {
        if (tag == null || !isSensitiveVmTag(tag) || !isVmEncryptionEnabled(vmUuid)) {
            return tag;
        }

        SensitiveTagContext ctx = parseSensitiveTag(tag);
        if (ctx == null) {
            return tag;
        }

        String value = ctx.tokenValue();
        if (value == null || isEncryptedValue(value)) {
            return tag;
        }

        String encryptedValue = encryptValueForTag(ctx.tagHead, value, vmUuid);
        if (encryptedValue == null) {
            return tag;
        }

        return ctx.withTokenValue(encryptedValue);
    }

    private String encryptPlainTagForPersist(String vmUuid, String tag) {
        if (tag == null || !isSensitiveVmTag(tag)) {
            return tag;
        }

        SensitiveTagContext ctx = parseSensitiveTag(tag);
        if (ctx == null) {
            return tag;
        }

        String value = ctx.tokenValue();
        if (value == null || isEncryptedValue(value)) {
            return tag;
        }

        String encryptedValue = encryptValueForTag(ctx.tagHead, value, vmUuid);
        if (encryptedValue == null) {
            return tag;
        }

        return ctx.withTokenValue(encryptedValue);
    }

    public void mirrorVmEncryptionFromSource(String srcVmUuid, String dstVmUuid) {
        if (!isVmEncryptionEnabled(srcVmUuid) || isVmEncryptionEnabled(dstVmUuid)) {
            return;
        }
        enableVmEncryption(dstVmUuid);
    }

    public void enableVmEncryption(String vmUuid) {
        if (isVmEncryptionEnabled(vmUuid)) {
            encryptExistingTagsForVm(vmUuid);
            return;
        }

        SystemTagCreator creator = VmSystemTags.VM_ENCRYPTION.newSystemTagCreator(vmUuid);
        creator.setTagByTokens(map(e(VmSystemTags.VM_ENCRYPTION_TOKEN, "true")));
        creator.recreate = true;
        creator.inherent = false;
        creator.create();

        encryptExistingTagsForVm(vmUuid);
    }

    public void disableVmEncryption(String vmUuid) {
        if (!isVmEncryptionEnabled(vmUuid)) {
            return;
        }

        decryptExistingTagsForVm(vmUuid);
        VmSystemTags.VM_ENCRYPTION.delete(vmUuid, VmInstanceVO.class);
    }

    public void encryptExistingTagsForVm(String vmUuid) {
        if (!isVmEncryptionEnabled(vmUuid)) {
            return;
        }

        for (PatternedSystemTag tag : sensitivePatternedTags()) {
            List<String> tags = tag.getTags(vmUuid, VmInstanceVO.class);
            if (tags == null || tags.isEmpty()) {
                continue;
            }
            for (String existingTag : tags) {
                String encryptedTag = encryptExistingDbTagIfNeeded(vmUuid, existingTag);
                if (encryptedTag.equals(existingTag)) {
                    continue;
                }
                updateTagString(existingTag, encryptedTag, vmUuid);
            }
        }
    }

    private void decryptExistingTagsForVm(String vmUuid) {
        for (PatternedSystemTag tag : sensitivePatternedTags()) {
            List<String> tags = tag.getTags(vmUuid, VmInstanceVO.class);
            if (tags == null || tags.isEmpty()) {
                continue;
            }
            for (String existingTag : tags) {
                String plainTag = decryptExistingDbTagIfNeeded(existingTag);
                if (plainTag.equals(existingTag)) {
                    continue;
                }
                updateTagString(existingTag, plainTag, vmUuid);
            }
        }
    }

    private String decryptExistingDbTagIfNeeded(String tag) {
        if (tag == null || !isSensitiveVmTag(tag)) {
            return tag;
        }

        SensitiveTagContext ctx = parseSensitiveTag(tag);
        if (ctx == null) {
            return tag;
        }

        String value = ctx.tokenValue();
        if (value == null || !isEncryptedValue(value)) {
            return tag;
        }

        return ctx.withTokenValue(decryptRawTokenValue(value));
    }

    private void updateTagString(String oldTag, String newTag, String vmUuid) {
        SystemTagVO vo = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, vmUuid)
                .eq(SystemTagVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .eq(SystemTagVO_.tag, oldTag)
                .find();
        if (vo == null) {
            return;
        }
        vo.setTag(newTag);
        dbf.updateAndRefresh(vo);
    }

    private String encryptValueForTag(String tokenName, String plainValue, String vmUuid) {
        String encrypted = ENC_PREFIX + encryptFacade.encrypt(plainValue);
        String candidateTag = tokenName + "::" + encrypted;
        if (candidateTag.length() > MAX_TAG_LENGTH) {
            if (VmSystemTags.USERDATA_TOKEN.equals(tokenName)) {
                logger.error(String.format(
                        "userdata for vm[uuid:%s] is too large to encrypt (tag length %d) while vmEncryption is enabled; storing plaintext",
                        vmUuid, candidateTag.length()));
                fireUserdataEncryptionDegraded(vmUuid, tokenName, plainValue.length(), candidateTag.length());
                return null;
            }
            throw new IllegalStateException(String.format(
                    "encrypted tag length %d exceeds max %d for token [%s]", candidateTag.length(), MAX_TAG_LENGTH, tokenName));
        }
        return encrypted;
    }

    private void fireUserdataEncryptionDegraded(String vmUuid, String tagType, int userdataBase64Length, int attemptedTagLength) {
        VmCanonicalEvents.VmUserdataEncryptionDegradedData data = new VmCanonicalEvents.VmUserdataEncryptionDegradedData();
        data.setVmUuid(vmUuid);
        data.setTagType(tagType);
        data.setUserdataBase64Length(userdataBase64Length);
        data.setAttemptedTagLength(attemptedTagLength);
        data.setThreshold(MAX_TAG_LENGTH);
        data.setDegradedToPlaintext(true);
        evtf.fire(VmCanonicalEvents.VM_USERDATA_ENCRYPTION_DEGRADED_PATH, data);
    }

    private SensitiveTagContext parseSensitiveTag(String tag) {
        PatternedSystemTag patternedTag = findSensitivePatternedTag(tag);
        if (patternedTag == null) {
            return null;
        }

        Map<String, String> tokens = patternedTag.getTokensByTag(tag);
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }

        String tagHead = tag.substring(0, tag.indexOf("::"));
        return new SensitiveTagContext(patternedTag, tagHead, tokens);
    }

    private PatternedSystemTag findSensitivePatternedTag(String tag) {
        int idx = tag.indexOf("::");
        if (idx <= 0) {
            return null;
        }
        PatternedSystemTag patternedTag = VmSystemTags.getSensitivePatternedTagByHead(tag.substring(0, idx));
        if (patternedTag != null && patternedTag.isMatch(tag)) {
            return patternedTag;
        }
        return null;
    }

    private List<PatternedSystemTag> sensitivePatternedTags() {
        return VmSystemTags.sensitivePatternedTags();
    }
}
