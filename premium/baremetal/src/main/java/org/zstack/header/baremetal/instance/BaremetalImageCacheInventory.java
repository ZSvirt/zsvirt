package org.zstack.header.baremetal.instance;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by GuoYi on 7/20/18.
 */
@Inventory(mappingVOClass = BaremetalImageCacheVO.class)
@PythonClassInventory
public class BaremetalImageCacheInventory {
    private long id;
    private String pxeServerUuid;
    private String imageUuid;
    private String url;
    private String installUrl;
    private String mediaType;
    private long size;
    private long actualSize;
    private String md5sum;
    private long utilization;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static BaremetalImageCacheInventory valueOf(BaremetalImageCacheVO vo) {
        BaremetalImageCacheInventory inv = new BaremetalImageCacheInventory();
        inv.setId(vo.getId());
        inv.setPxeServerUuid(vo.getPxeServerUuid());
        inv.setImageUuid(vo.getImageUuid());
        inv.setUrl(vo.getUrl());
        inv.setInstallUrl(vo.getInstallUrl());
        inv.setMediaType(vo.getMediaType().toString());
        inv.setSize(vo.getSize());
        inv.setActualSize(vo.getActualSize());
        inv.setMd5sum(vo.getMd5sum());
        inv.setUtilization(vo.getUtilization());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<BaremetalImageCacheInventory> valueOf(List<BaremetalImageCacheVO> vos) {
        List<BaremetalImageCacheInventory> invs = new ArrayList<>();
        for (BaremetalImageCacheVO vo : vos) {
            invs.add(BaremetalImageCacheInventory.valueOf(vo));
        }
        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPxeServerUuid() {
        return pxeServerUuid;
    }

    public void setPxeServerUuid(String pxeServerUuid) {
        this.pxeServerUuid = pxeServerUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInstallUrl() {
        return installUrl;
    }

    public void setInstallUrl(String installUrl) {
        this.installUrl = installUrl;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public long getUtilization() {
        return utilization;
    }

    public void setUtilization(long utilization) {
        this.utilization = utilization;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
