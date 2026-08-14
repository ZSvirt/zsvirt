package org.zstack.storage.backup.imagestore;

/**
 * Created by mingjian.deng on 2017/9/25.
 */
public class ImageStoreImageResponse {
    private String id;
    private String parent;
    private String blobsum;
    private String created;
    private String author;
    private String arch;
    private String desc;
    private Long size;
    private Long virtualsize;
    private String name;

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

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
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

    public String getInstallPath() {
        return String.format("%s%s/%s", ImageStoreBackupStorage.zstoreProto, name, id);
    }
}
