package org.zstack.test.unittest.storage.volume;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.header.volume.APICreateDataVolumeFromVolumeTemplateMsg;
import org.zstack.header.volume.APICreateDataVolumeMsg;
import org.zstack.header.volume.CreateDataVolumeFromVolumeTemplateMsg;
import org.zstack.header.volume.CreateDataVolumeMsg;
import org.zstack.header.volume.VolumeCreateMessage;
import org.zstack.sdk.CreateDataVolumeAction;
import org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction;

import java.lang.reflect.Field;

public class EncryptedDataVolumeCreateApiTest {
    @Test
    public void testSdkCreateDataVolumeActionExposesEncryptedParameter() throws Exception {
        Field encrypted = CreateDataVolumeAction.class.getField("encrypted");
        Assert.assertEquals(Boolean.class, encrypted.getType());
    }

    @Test
    public void testSdkCreateDataVolumeFromVolumeTemplateActionExposesEncryptedParameter() throws Exception {
        Field encrypted = CreateDataVolumeFromVolumeTemplateAction.class.getField("encrypted");
        Assert.assertEquals(Boolean.class, encrypted.getType());
    }

    @Test
    public void testApiCreateDataVolumeMessageCarriesEncryptedFlag() {
        VolumeCreateMessage msg = new APICreateDataVolumeMsg();
        msg.setEncrypted(Boolean.TRUE);
        Assert.assertEquals(Boolean.TRUE, msg.getEncrypted());
    }

    @Test
    public void testCreateDataVolumeMessageCarriesEncryptedFlag() {
        CreateDataVolumeMsg msg = new CreateDataVolumeMsg();
        msg.setEncrypted(Boolean.TRUE);
        Assert.assertEquals(Boolean.TRUE, msg.getEncrypted());
    }

    @Test
    public void testApiCreateDataVolumeFromTemplateMessageCarriesEncryptedFlag() {
        VolumeCreateMessage msg = new APICreateDataVolumeFromVolumeTemplateMsg();
        msg.setEncrypted(Boolean.TRUE);
        Assert.assertEquals(Boolean.TRUE, msg.getEncrypted());
    }

    @Test
    public void testCreateDataVolumeFromTemplateMessageCarriesEncryptedFlag() {
        CreateDataVolumeFromVolumeTemplateMsg msg = new CreateDataVolumeFromVolumeTemplateMsg();
        msg.setEncrypted(Boolean.TRUE);
        Assert.assertEquals(Boolean.TRUE, msg.getEncrypted());
    }
}
