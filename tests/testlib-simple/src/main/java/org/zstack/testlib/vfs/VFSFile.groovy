package org.zstack.testlib.vfs

import org.zstack.utils.gson.JSONObjectUtil

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path

class VFSFile {
    String type
    Path path

    protected transient VFS vfs

    VFSFile(VFS vfs) {
        this.vfs = vfs
    }

    String pathString() {
        return path.toAbsolutePath().toString()
    }

    protected Map<String, Object> asMap() {
        return ["type": type, "path": path.toAbsolutePath().toString()]
    }

    protected VFSFile fromMap(Map<String, Object> m) {
        type = m["type"]
        String p = m["path"]
        if (p != null) {
            path = vfs.getPath(p)
        }

        return this
    }

    String asJSONString() {
        type = getClass().getName()
        return JSONObjectUtil.toJsonString(asMap())
    }

    static <T> T fromJSON(String str, VFS vfs) {
        Map m = JSONObjectUtil.toObject(str, LinkedHashMap.class)
        String type = m["type"]
        assert type
        Class clz = Class.forName(type)
        VFSFile f = clz.getConstructor(VFS.class).newInstance(vfs)
        return f.fromMap(m) as T
    }

    VFSFile update() {
        return create()
    }

    VFSFile create() {
        vfs.write(path, asJSONString())
        return this
    }

    VFSFile copy(String dst) {
        Path dstPath = vfs.getPath(dst)
        if (Files.exists(dstPath)) {
            throw new FileAlreadyExistsException("target file[${dst}] already exists")
        }

        VFSFile f = vfs.getFile(pathString(), true)
        f.path = dstPath
        return f.create()
    }

    VFSFile move(String dst) {
        VFSFile target = copy(dst)
        delete()
        return target
    }

    void delete() {
        vfs.delete(path)
    }

    boolean exists() {
        return Files.isRegularFile(path)
    }

    @Override
    int hashCode() {
        return asMap().hashCode()
    }

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof VFSFile)) {
            return false
        }

        return Objects.equals(asMap(), obj.asMap())
    }

    @Override
    String toString() {
        return asJSONString()
    }

    boolean compare(VFSFile f) {
        Map dst = f.asMap()
        Map src = asMap()
        dst.remove("path")
        src.remove("path")
        return Objects.equals(src, dst)
    }
}
