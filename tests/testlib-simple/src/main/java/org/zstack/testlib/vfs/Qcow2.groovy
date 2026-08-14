package org.zstack.testlib.vfs

import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path

class Qcow2 extends Volume {
    private static CLogger logger = Utils.getLogger(Qcow2.class)

    Path backingFile

    Qcow2(VFS vfs) {
        super(vfs)
    }

    Qcow2 backingQcow2() {
        if (backingFile == null) {
            return null
        }

        return vfs.getFile(backingFile.toAbsolutePath().toString(), true)
    }

    Qcow2 create(String pathStr, long actualSize, Long virtualSize=null, String backingFileStr=null) {
        logger.debug("[Qcow2 create] path: ${pathStr}, actual size ${actualSize}, virtual size ${virtualSize}, backing file: ${backingFileStr}")
        path =  vfs.getPath(pathStr)
        if (Files.exists(path)) {
            throw new FileAlreadyExistsException("${pathStr} already exists")
        }

        if (backingFileStr != null) {
            backingFile = vfs.getPath(backingFileStr)
            if (!Files.isRegularFile(backingFile)) {
                throw new FileNotFoundException("backing file[${backingFileStr}] not found")
            }
        }

        this.actualSize = actualSize
        if (virtualSize != null) {
            this.virtualSize = virtualSize
        } else {
            this.virtualSize = actualSize
        }

        vfs.createDirectories(path.getParent())
        create()

        return this
    }

    Qcow2 rebase(Path p) {
        if (p == null) {
            return rebase((String)null)
        } else {
            return rebase(p.toAbsolutePath().toString())
        }
    }

    Qcow2 rebase(String backingFilePathStr) {
        Path originBackingFile = backingFile

        if (backingFilePathStr == null) {
            backingFile = null
        } else {
            Path p = vfs.getPath(backingFilePathStr)
            if (!Files.exists(p)) {
                throw new FileNotFoundException("cannot find backing file[${backingFilePathStr}]")
            }

            backingFile = p
        }

        assert path != backingFile : "cannot rebase the qcow2 to itself[${pathString()}]"

        update()
        logger.debug("[VFS(id: ${vfs.id}) REBASE (${pathString()})]: from ${originBackingFile} to ${backingFile}")

        return this
    }

    Qcow2 snapshot(String pathStr) {
        if (!exists()) {
            throw new FileNotFoundException("source file ${pathStr} not exists")
        }

        Qcow2 snapshot = new Qcow2(vfs)
        return snapshot.create(pathStr, 0L, null, path.toAbsolutePath().toString())
    }

    @Override
    protected Map<String, Object> asMap() {
        Map m = super.asMap()
        m.putAll([
                "backingFile": backingFile?.toAbsolutePath()?.toString()
        ])

        return m
    }

    @Override
    protected VFSFile fromMap(Map<String, Object> m) {
        super.fromMap(m)
        String p = m["backingFile"]
        if (p != null) {
            backingFile = vfs.getPath(p)
        }
        return this
    }

    public List<Qcow2> getQcow2Chain(Qcow2 q) {
        List<Qcow2> target = [q]
        Path bf = q.backingFile
        while (bf != null) {
            Qcow2 backingQcow2 = vfs.getFile(bf.toAbsolutePath().toString(), true)
            target.add(backingQcow2)
            bf = backingQcow2.backingFile
        }

        return target
    }

    /**
     * this will compare the backing file chain
     * @param q
     * @return
     */
    boolean qcow2Compare(Qcow2 q)  {
        List<Qcow2> target = getQcow2Chain(q)
        List<Qcow2> us = getQcow2Chain(this)

        return target == us
    }

    void assertQcow2Compare(Qcow2 q)  {
        List<Qcow2> target = getQcow2Chain(q)
        List<Qcow2> us = getQcow2Chain(this)
        assert  target == us
    }

    Qcow2 copyWithoutBackingFile(String dst) {
        Path dstPath = vfs.getPath(dst)
        if (Files.exists(dstPath)) {
            throw new FileAlreadyExistsException("target file[${dst}] already exists")
        }

        vfs.createDirectories(dstPath.getParent())
        Qcow2 f = vfs.getFile(pathString(), true)
        f.backingFile = null
        f.path = dstPath
        return f.create() as Qcow2
    }

    Qcow2 getBaseImage(Qcow2 q) {
        Qcow2 baseImage = null
        Path bf = q.backingFile
        while (bf != null) {
            baseImage = vfs.getFile(bf.toAbsolutePath().toString())

            if (baseImage == null) {
                break
            }

            bf = baseImage.backingFile
        }

        return baseImage
    }

    List<Qcow2> getChildren() {
        List<Qcow2> children = []
        vfs.walkFileSystem { vfile ->
            if (vfile instanceof Qcow2 && vfile.backingFile != null && vfile.backingFile.toAbsolutePath().toString() == pathString()) {
                children.add(vfile)
            }
        }
        return children
    }

    static Qcow2 commit(VFS vfs, Qcow2 top, Qcow2 base) {
        top.backingFile = null
        top.update()
        List<Qcow2> childrenOfTop = top.getChildren()
        childrenOfTop.forEach { children ->
            children.backingFile = vfs.getPath(base.pathString())
            children.update()
        }
        return base
    }

    static void pull(VFS vfs, String base, Qcow2 volumePath) {
        if (base == null) {
            volumePath.rebase((String) null)
        } else {
            volumePath.rebase(base)
        }
    }
}
