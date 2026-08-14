package org.zstack.ovf.datatype;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.volume.VolumeInventory;

import java.util.Objects;

import static org.zstack.core.Platform.err;
import static org.zstack.ovf.datatype.OvfErrors.INVALID_IMAGE_INFO;

/**
 * Created by Wenhao.Zhang on 22/03/08
 */
public class CreateVmFromOvfImageParam {
    private String ovfId;
    private CreateVmFromOvfParamType type;
    private String url;
    private String uuid; // imageUuid

    private String fileName;
    private String longJobUuid;
    /**
     * disk index in OVF template. index=0 is root volume
     */
    private int index;
    private VolumeInventory volume;

    public ErrorCode validate() {
        if (ovfId == null)
            return err(INVALID_IMAGE_INFO, "ovfId is null");
        if (type == null)
            return err(INVALID_IMAGE_INFO, "type is null");
        if (url == null && uuid == null && !Objects.equals(type, CreateVmFromOvfParamType.Upload))
            return err(INVALID_IMAGE_INFO, "url and uuid is null when type is Upload");
        return null;
    }

    public String getOvfId() {
        return ovfId;
    }

    public void setOvfId(String ovfId) {
        this.ovfId = ovfId;
    }

    public CreateVmFromOvfParamType getType() {
        return type;
    }

    public void setType(CreateVmFromOvfParamType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLongJobUuid() {
        return longJobUuid;
    }

    public void setLongJobUuid(String longJobUuid) {
        this.longJobUuid = longJobUuid;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isImageOfRootVolume() {
        return index == 0;
    }

    public boolean isImageOfDataVolume() {
        return !isImageOfRootVolume();
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
}
