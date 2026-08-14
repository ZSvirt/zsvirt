package org.zstack.testlib.vfs

class CephRaw extends Raw {
    // an image cloned from a snapshot depends on the snapshot
    CephRaw parent

    CephRaw(VFS vfs) {
        super(vfs)
    }

    @Override
    protected Map<String, Object> asMap() {
        Map<String, Object> m = super.asMap()
        if (parent != null) {
            m.put("parent", parent.pathString())
        }

        return m
    }

    @Override
    protected VFSFile fromMap(Map<String, Object> m) {
        super.fromMap(m)
        String p = m["parent"]
        if (p != null) {
            parent = vfs.getFile(p, true)
        }

        return this
    }

    protected CephRaw create(String pathStr, long size, String parentPath=null) {
        if (parentPath != null) {
            parent = vfs.getFile(parentPath, true)
        }

        super.create(pathStr, size)
        return this
    }

    CephRaw flatten() {
        if (parent != null) {
            vfs.Assert(vfs.exists(parent.pathString()), "cannot find the parent[${parent.pathString()}]")
        }

        parent = null
        update()
        return this
    }
}
