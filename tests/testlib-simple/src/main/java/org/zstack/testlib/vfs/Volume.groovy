package org.zstack.testlib.vfs

class Volume extends VFSFile {
    long virtualSize
    long actualSize

    Volume(VFS vfs) {
        super(vfs)
    }

    @Override
    protected Map<String, Object> asMap() {
        Map m = super.asMap()
        m.putAll([
                "virtualSize": virtualSize,
                "actualSize": actualSize
        ])
        return m
    }

    @Override
    protected VFSFile fromMap(Map<String, Object> m) {
        super.fromMap(m)
        virtualSize = m["virtualSize"] as long
        actualSize = m["actualSize"] as long
        return this
    }

    protected Volume create(String pathStr, long size) {
        actualSize = size
        virtualSize = size
        path = vfs.getPath(pathStr)
        vfs.Assert(!vfs.exists(path),"file[${pathStr}] already exists")
        create()
        return this
    }
}
