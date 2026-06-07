package org.zstack.test.unittest.compute;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSensitiveTagEncryptor;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.encrypt.EncryptFacade;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.vm.VmCanonicalEvents;
import org.zstack.header.vm.VmInstanceVO;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Collections;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class VmSensitiveTagEncryptorTest {
    private VmSensitiveTagEncryptor encryptor;
    private EncryptFacade encryptFacade;

    @Before
    public void setUp() throws Exception {
        encryptor = new VmSensitiveTagEncryptor();
        encryptFacade = mock(EncryptFacade.class);
        setField("encryptFacade", encryptFacade);
    }

    @Test
    public void testIsSensitiveVmTag() {
        Assert.assertTrue(encryptor.isSensitiveVmTag("consolePassword::pwd"));
        Assert.assertTrue(encryptor.isSensitiveVmTag("sshkey::key"));
        Assert.assertTrue(encryptor.isSensitiveVmTag("userdata::dGVzdA=="));
        Assert.assertFalse(encryptor.isSensitiveVmTag("hostname::vm1"));
        Assert.assertFalse(encryptor.isSensitiveVmTag(null));
    }

    @Test
    public void testIsEncryptedValue() {
        when(encryptFacade.decrypt("cipher")).thenReturn("plain");
        when(encryptFacade.decrypt("not-decryptable")).thenReturn("not-decryptable");

        Assert.assertTrue(encryptor.isEncryptedValue("ENC:cipher"));
        Assert.assertFalse(encryptor.isEncryptedValue("ENC:not-decryptable"));
        Assert.assertFalse(encryptor.isEncryptedValue("plain"));
        Assert.assertFalse(encryptor.isEncryptedValue(null));
    }

    @Test
    public void testDecryptTokenValueWhitelist() {
        when(encryptFacade.decrypt("cipher")).thenReturn("plain");

        String plain = encryptor.decryptTokenValue(
                VmInstanceVO.class.getSimpleName(),
                VmSystemTags.CONSOLE_PASSWORD_TOKEN,
                VmSystemTags.CONSOLE_PASSWORD_TOKEN,
                VmSensitiveTagEncryptor.ENC_PREFIX + "cipher");
        Assert.assertEquals("plain", plain);

        String unchanged = encryptor.decryptTokenValue(
                VmInstanceVO.class.getSimpleName(),
                VmSystemTags.HOSTNAME_TOKEN,
                VmSystemTags.HOSTNAME_TOKEN,
                VmSensitiveTagEncryptor.ENC_PREFIX + "cipher");
        Assert.assertEquals(VmSensitiveTagEncryptor.ENC_PREFIX + "cipher", unchanged);

        String wrongResource = encryptor.decryptTokenValue(
                "HostVO",
                VmSystemTags.CONSOLE_PASSWORD_TOKEN,
                VmSystemTags.CONSOLE_PASSWORD_TOKEN,
                VmSensitiveTagEncryptor.ENC_PREFIX + "cipher");
        Assert.assertEquals(VmSensitiveTagEncryptor.ENC_PREFIX + "cipher", wrongResource);
    }

    @Test
    public void testDecryptRawTokenValue() {
        when(encryptFacade.decrypt("cipher")).thenReturn("plain");
        Assert.assertEquals("plain", encryptor.decryptRawTokenValue(VmSensitiveTagEncryptor.ENC_PREFIX + "cipher"));
        Assert.assertEquals("plain-text", encryptor.decryptRawTokenValue("plain-text"));
    }

    @Test
    public void testTransformTagForCopyToEncryptedDest() {
        VmSensitiveTagEncryptor spyEncryptor = spy(encryptor);
        doReturn(true).when(spyEncryptor).isVmEncryptionEnabled("dst-vm");
        when(encryptFacade.decrypt("cipher")).thenReturn("plain");
        when(encryptFacade.encrypt("plain")).thenReturn("newCipher");

        String srcTag = VmSystemTags.CONSOLE_PASSWORD.instantiateTag(
                java.util.Collections.singletonMap(VmSystemTags.CONSOLE_PASSWORD_TOKEN,
                        VmSensitiveTagEncryptor.ENC_PREFIX + "cipher"));

        String dstTag = spyEncryptor.transformTagForCopy(
                "src-vm", VmInstanceVO.class.getSimpleName(),
                "dst-vm", VmInstanceVO.class.getSimpleName(),
                srcTag);

        Assert.assertEquals(
                VmSystemTags.CONSOLE_PASSWORD.instantiateTag(
                        java.util.Collections.singletonMap(VmSystemTags.CONSOLE_PASSWORD_TOKEN,
                                VmSensitiveTagEncryptor.ENC_PREFIX + "newCipher")),
                dstTag);
    }

    @Test
    public void testEncryptTagIfNeededSkipsAlreadyEncryptedValue() {
        VmSensitiveTagEncryptor spyEncryptor = spy(encryptor);
        doReturn(true).when(spyEncryptor).isVmEncryptionEnabled("vm-uuid");
        when(encryptFacade.decrypt("cipher")).thenReturn("plain");

        String encryptedTag = VmSystemTags.CONSOLE_PASSWORD.instantiateTag(
                Collections.singletonMap(VmSystemTags.CONSOLE_PASSWORD_TOKEN,
                        VmSensitiveTagEncryptor.ENC_PREFIX + "cipher"));

        Assert.assertEquals(encryptedTag, spyEncryptor.encryptTagIfNeeded("vm-uuid", encryptedTag));
    }

    @Test
    public void testUserdataEncryptionDegradedWhenTagTooLarge() throws Exception {
        EventFacade evtf = mock(EventFacade.class);
        setField("evtf", evtf);

        VmSensitiveTagEncryptor spyEncryptor = spy(encryptor);
        doReturn(true).when(spyEncryptor).isVmEncryptionEnabled("vm-uuid");

        char[] huge = new char[66000];
        java.util.Arrays.fill(huge, 'x');
        when(encryptFacade.encrypt(org.mockito.Mockito.anyString())).thenReturn(new String(huge));

        String plainUserdata = Base64.getEncoder().encodeToString("cloud-init-data".getBytes());
        String tag = VmSystemTags.USERDATA.instantiateTag(
                map(e(VmSystemTags.USERDATA_TOKEN, plainUserdata)));

        Assert.assertEquals(tag, spyEncryptor.encryptTagIfNeeded("vm-uuid", tag));
        verify(evtf).fire(eq(VmCanonicalEvents.VM_USERDATA_ENCRYPTION_DEGRADED_PATH), any());
    }

    @Test
    public void testTransformTagForCopyToPlainDest() {
        VmSensitiveTagEncryptor spyEncryptor = spy(encryptor);
        doReturn(false).when(spyEncryptor).isVmEncryptionEnabled("dst-vm");
        when(encryptFacade.decrypt("cipher")).thenReturn("plain");

        String encryptedTag = VmSystemTags.CONSOLE_PASSWORD.instantiateTag(
                Collections.singletonMap(VmSystemTags.CONSOLE_PASSWORD_TOKEN,
                        VmSensitiveTagEncryptor.ENC_PREFIX + "cipher"));

        String dstTag = spyEncryptor.transformTagForCopy(
                "src-vm", VmInstanceVO.class.getSimpleName(),
                "dst-vm", VmInstanceVO.class.getSimpleName(),
                encryptedTag);
        Assert.assertEquals(
                VmSystemTags.CONSOLE_PASSWORD.instantiateTag(
                        Collections.singletonMap(VmSystemTags.CONSOLE_PASSWORD_TOKEN, "plain")),
                dstTag);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = VmSensitiveTagEncryptor.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(encryptor, value);
    }
}
