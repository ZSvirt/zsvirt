package org.zstack.test.unittest.compute;

import org.junit.Test;
import org.zstack.compute.vm.VmMetadataRegistrationInputValidator;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertThrows;

public class VmMetadataRegistrationValidationTest {
    private static final String METADATA = "{\"vm\":{\"resourceUuid\":\"11111111111111111111111111111111\"}}";

    @Test
    public void acceptsInlineWithoutAStorageMetadataPath() {
        VmMetadataRegistrationInputValidator.validate(null, METADATA, 1024);
    }

    @Test
    public void keepsPathInputIndependentOfTheInlinePayloadLimit() {
        VmMetadataRegistrationInputValidator.validate("/vm-metadata/vm.vmmeta", null, 0);
    }

    @Test
    public void requiresExactlyOneInput() {
        assertThrows(IllegalArgumentException.class,
                () -> VmMetadataRegistrationInputValidator.validate(null, null, 1024));
        assertThrows(IllegalArgumentException.class,
                () -> VmMetadataRegistrationInputValidator.validate("/vm.vmmeta", METADATA, 1024));
    }

    @Test
    public void rejectsBlankInputs() {
        for (String blank : new String[]{"", " ", "\n\t"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> VmMetadataRegistrationInputValidator.validate(null, blank, 1024));
            assertThrows(IllegalArgumentException.class,
                    () -> VmMetadataRegistrationInputValidator.validate(blank, null, 1024));
        }
    }

    @Test
    public void rejectsMalformedJsonAndMissingVmIdentityBeforeRouting() {
        for (String invalid : new String[]{"{", "[]", "null", "{}", "{\"vm\":{}}",
                "{\"vm\":{\"resourceUuid\":\" \"}}"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> VmMetadataRegistrationInputValidator.validate(null, invalid, 1024));
        }
    }

    @Test
    public void limitsUtf8BytesAndAcceptsTheExactBoundary() {
        String metadata = METADATA.replace("}}", "},\"schemaVersion\":\"版本\"}");
        int bytes = metadata.getBytes(StandardCharsets.UTF_8).length;
        VmMetadataRegistrationInputValidator.validate(null, metadata, bytes);
        assertThrows(IllegalArgumentException.class,
                () -> VmMetadataRegistrationInputValidator.validate(null, metadata, bytes - 1));
    }
}
