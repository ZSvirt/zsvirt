package org.zstack.storage.backup.imagestore;

import org.zstack.core.Platform;
import org.zstack.header.errorcode.OperationFailureException;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Created by mingjian.deng on 2017/8/31.
 */
public class ImageStoreImageStruct {
    private String id;
    private String parent;
    private String blobsum;
    private Timestamp created;
    private String author;
    private String arch;
    private String desc;
    private Long size;
    private Long virtualsize;
    private String name;

    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public ImageStoreImageStruct() {
    }

    public ImageStoreImageStruct(ImageStoreImageResponse resp) {
        this.id = resp.getId();
        this.parent = resp.getParent();
        this.blobsum = resp.getBlobsum();
        this.created = fmt(resp.getCreated());
        this.author = resp.getAuthor();
        this.arch = resp.getArch();
        this.desc = resp.getDesc();
        this.size = resp.getSize();
        this.virtualsize = resp.getVirtualsize();
        this.name = resp.getName();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getBlobsum() {
        return blobsum;
    }

    public void setBlobsum(String blobsum) {
        this.blobsum = blobsum;
    }

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    public Timestamp fmt(String created) {
        try {
            SimpleDateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
            return new Timestamp(df.parse(created.substring(0, 18)).getTime());
        } catch (ParseException e) {
            throw new OperationFailureException(Platform.operr("parse create time error: %s", e.getMessage()));
        }
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getVirtualsize() {
        return virtualsize;
    }

    public void setVirtualsize(Long virtualsize) {
        this.virtualsize = virtualsize;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
