package org.zstack.ovf;

import org.dom4j.DocumentHelper;
import org.junit.Test;
import org.zstack.ovf.datatype.OvfDiskInfo;
import org.zstack.ovf.datatype.OvfInfo;

import static org.junit.Assert.assertEquals;

public class OvfHelperFilenameTest {
    @Test
    public void decodesDiskFilenameWithoutChangingReferences() throws Exception {
        OvfInfo info = OvfHelper.parseOvf(DocumentHelper.parseText(
                "<Envelope xmlns=\"http://schemas.dmtf.org/ovf/envelope/1\" " +
                        "xmlns:ovf=\"http://schemas.dmtf.org/ovf/envelope/1\">" +
                        "<References><File ovf:id=\"file1\" ovf:href=\"Rocky%209-1.vmdk\"/></References>" +
                        "<DiskSection><Disk ovf:diskId=\"disk1\" ovf:fileRef=\"file1\" " +
                        "ovf:capacity=\"1024\"/></DiskSection>" +
                        "<VirtualSystem ovf:id=\"vm\"><Name>Rocky 9</Name>" +
                        "<OperatingSystemSection/><VirtualHardwareSection><System/>" +
                        "</VirtualHardwareSection></VirtualSystem></Envelope>"));

        assertEquals(1, info.getDisks().size());
        OvfDiskInfo disk = info.getDisks().get(0);
        assertEquals("Rocky 9-1.vmdk", disk.getFileName());
        assertEquals("file1", disk.getFileRef());
        assertEquals("disk1", disk.getDiskId());
    }
}
