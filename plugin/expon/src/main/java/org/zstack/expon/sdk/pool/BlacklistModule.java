package org.zstack.expon.sdk.pool;

/**
 * @example {
 * "clone_id": 0,
 * "expire_time": 0,
 * "nid": 1,
 * "path": "10.1.107.48:9001/13/VOLUME/volume-5fa313cc-8da4-4212-9605-5a6b8e3ba917",
 * "snap_id": 0,
 * "tree_id": 1,
 * "type": "VOLUME",
 * "uss_id": 13,
 * "uss_ip": "10.1.107.48",
 * "uss_port": 9001,
 * "vol_namespace": "ussns",
 * "volname": "volume-5fa313cc-8da4-4212-9605-5a6b8e3ba917"
 * }
 */
public class BlacklistModule {
    private String cloneId;
    private String expireTime;
    private String nid;
    private String path;
    private String snapId;
    private String treeId;
    private String type;
    private String ussId;
    private String ussIp;
    private String ussPort;
    private String volNamespace;
    private String volname;

    public String getCloneId() {
        return cloneId;
    }

    public void setCloneId(String cloneId) {
        this.cloneId = cloneId;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }

    public String getNid() {
        return nid;
    }

    public void setNid(String nid) {
        this.nid = nid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSnapId() {
        return snapId;
    }

    public void setSnapId(String snapId) {
        this.snapId = snapId;
    }

    public String getTreeId() {
        return treeId;
    }

    public void setTreeId(String treeId) {
        this.treeId = treeId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUssId() {
        return ussId;
    }

    public void setUssId(String ussId) {
        this.ussId = ussId;
    }

    public String getUssIp() {
        return ussIp;
    }

    public void setUssIp(String ussIp) {
        this.ussIp = ussIp;
    }

    public String getUssPort() {
        return ussPort;
    }

    public void setUssPort(String ussPort) {
        this.ussPort = ussPort;
    }

    public String getVolNamespace() {
        return volNamespace;
    }

    public void setVolNamespace(String volNamespace) {
        this.volNamespace = volNamespace;
    }

    public String getVolname() {
        return volname;
    }

    public void setVolname(String volname) {
        this.volname = volname;
    }
}
