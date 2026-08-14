package org.zstack.testlib.vfs

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.vfs.tree.PathNode
import org.zstack.testlib.vfs.tree.Qcow2Tree
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.nio.file.CopyOption
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors

class VFS {
    private static final CLogger logger = Utils.getLogger(VFS.class)

    private FileSystem fileSystem
    String id = "id-not-specified"

    VFS(String id) {
        this()
        this.id = id
    }

    VFS() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix().toBuilder()
                .setAttributeViews("basic", "owner", "posix", "unix")
                .build())
    }

    static void vfsHook(String path, EnvSpec env, Closure func) {
        env.afterSimulator(path) { rsp, entity, spec ->
            assert rsp.hasProperty("success") : "the response[${rsp.getClass()}] doesn't have a field named success"

            if (!rsp["success"]) {
                return rsp
            }

            def ret = func(rsp, entity, spec)

            return ret == null ? rsp : ret
        }
    }

    PathNode asSinglePathNode() {
        PathNode root = new PathNode(path: "/", children: asPathNodes())
        root.children.each { it.parent = root }
        return root
    }

    List<PathNode> asPathNodes() {
        Map<String, PathNode> nodes = [:]

        walkFileSystem { f ->
            PathNode node = nodes.computeIfAbsent(f.pathString(), {new PathNode(path: f.pathString())})
            if (f instanceof Qcow2 && f.backingFile != null) {
                String bpath = f.backingFile.toAbsolutePath().toString()
                assert exists(f.backingFile) : "corrupt file[${f.pathString()}], it's backing file[${bpath}] not found on the VFS[id: ${id}]"
                PathNode p = nodes.computeIfAbsent(bpath, {new PathNode(path: bpath)})
                p.children.add(node)
                node.parent = p
            }
        }

        return nodes.values().findAll { it.parent == null }
    }

    String dumpAsString() {
        PathNode root = new PathNode(path: "/", children: asPathNodes())
        return "======================= Dump VFS[id: ${id}] ===================\n" +
                "${root.dumpAsString(false)}\n" +
                "==============================================================="
    }

    CephRaw createCephRaw(String path, long size, String parent=null) {
        return new CephRaw(this).create(path, size, parent)
    }

    Raw createRaw(String path, long size) {
        return new Raw(this).create(path, size) as Raw
    }

    Qcow2 createQcow2(String path, long actualSize, Long virtualSize=0, String backingFile=null) {
        return new Qcow2(this).create(path, actualSize, virtualSize, backingFile)
    }

    Path getPath(String pathStr) {
        return fileSystem.getPath(pathStr)
    }

    boolean move(Path from, Path to, CopyOption... options) {
        logger.debug("[VFS(id: ${id}) MOVE FILE]: FROM ${from.toAbsolutePath().toString()} TO ${to.toAbsolutePath().toString()}")
        return Files.move(from, to, options)
    }

    List<Path> link(String linkPath, String existingPath) {
        return link(getPath(linkPath), getPath(existingPath))
    }

    List<Path> link(Path link, Path existing) {
        if (!isDir(existing)) {
            logger.debug("[VFS(id: ${id}) LINK FILE]: EXISTING ${existing.toAbsolutePath().toString()} LINK ${link.toAbsolutePath().toString()}")
            return [Files.createLink(link, existing)]
        }

        List<Path> links = []
        Files.walk(existing).forEach {existingPath ->
            String newPath = existingPath.toString().replace(existing.toString(), link.toString())
            if (isDir(existingPath)) {
                Files.createDirectories(getPath(newPath))
                return
            }

            if (Files.isSameFile(getPath(newPath), existingPath)) {
                return
            }

            logger.debug("[VFS(id: ${id}) LINK FILE]: EXISTING ${existing.toAbsolutePath().toString()} LINK ${link.toAbsolutePath().toString()}")
            links.add(Files.createLink(getPath(newPath), existingPath))
        }
        return links
    }

    void unlink(String linkStr, boolean onlyLinkedPath) {
        def f = getPath(linkStr)
        if (!Files.isDirectory(f)) {
            if (!onlyLinkedPath || Files.getAttribute(f, "unix:nlink") > 1) {
                logger.debug("[VFS(id: ${id}) DELETE]: ${f.toAbsolutePath().toString()}")
                Files.delete(f)
            }
            return
        }
        Files.walk(f).forEach { path ->
            if (Files.isDirectory(path)) {
                return
            }

            if (!onlyLinkedPath || Files.getAttribute(f, "unix:nlink") > 1) {
                logger.debug("[VFS(id: ${id}) DELETE]: ${path.toAbsolutePath().toString()}")
                Files.delete(path)
            }
        }
    }

    boolean exists(String path) {
        return exists(getPath(path))
    }

    boolean exists(Path path) {
        return Files.exists(path)
    }

    boolean isFile(String path) {
        return isFile(getPath(path))
    }

    boolean isFile(Path path) {
        assert exists(path) : "cannot find file[${path.toAbsolutePath().toString()}]"
        return Files.isRegularFile(path)
    }

    void delete(String pathStr) {
        delete(getPath(pathStr))
    }

    void Assert(boolean expr, String msg) {
        assert expr : "${msg}. VFS[id: ${id}] Dump:\n${dumpAsString()}"
    }

    void delete(Path path) {
        Assert(exists(path), "cannot find the file or dir[${path}]")

        if (isFile(path)) {
            logger.debug("[VFS(id: ${id}) DELETE]: ${path}")
            Files.delete(path)
        } else {
            assert isDir(path) : "cannot find dir[${path}] or it's not a directory"

            logger.debug("[VFS(id: ${id}) DELETE DIR]: ${path}")
            Files.walk(path).sorted(Comparator.reverseOrder()).forEach {
                logger.debug("[VFS(id: ${id}) DELETE]: ${it}")
                Files.delete(it)
            }
        }
    }

    boolean isDir(String path) {
        return isDir(getPath(path))
    }

    boolean isDir(Path path) {
        assert exists(path) : "cannot find directory[${path.toAbsolutePath().toString()}]"
        return Files.isDirectory(path)
    }

    Path createDirectories(Path p) {
        return createDirectories(p.toAbsolutePath().toString())
    }

    void write(Path p, String str) {
        if (exists(p)) {
            logger.debug("[VFS(id: ${id}) UPDATE FILE]: ${p}, ${str}")
        } else {
            logger.debug("[VFS(id: ${id}) CREATE FILE]: ${p}, ${str}")
        }
        Files.write(p, str.getBytes())
    }

    Path createDirectories(String pathStr) {
        Path p = fileSystem.getPath(pathStr)
        assert !Files.isRegularFile(p) : "${pathStr} is a file"
        logger.debug("[VFS(id: ${id}) CREATE DIRs]: ${pathStr}")
        Files.createDirectories(p)
        return p
    }

    static <T> T getFile(String pathStr, VFS vfs) {
        return getFile(vfs.getPath(pathStr), vfs)
    }

    static <T> T getFile(Path path, VFS vfs) {
        if (!Files.exists(path)) {
            return null
        }

        if (!Files.isRegularFile(path)) {
            throw new FileNotFoundException("${path.toString()} is not a file")
        }

        String json = Files.readAllLines(path).join("\n")
        return VFSFile.fromJSON(json, vfs)
    }

    def <T> T getFile(String pathStr, boolean errorOnMissing=false) {
        T f = getFile(pathStr, this)
        if (f == null && errorOnMissing) {
            throw new FileNotFoundException("file[${pathStr}] not found on VFS[id: ${id}]")
        }

        return f
    }

    def <T> T getFile(Path path, boolean errorOnMissing=false) {
        T f = getFile(path, this)
        if (f == null && errorOnMissing) {
            throw new FileNotFoundException("file[${path.toString()}] not found on VFS[id: ${id}]")
        }

        return f
    }

    private static VFSFile doFindFile(VFS vfs, Path path, Function<VFSFile, Boolean> c) {
        // check exists to prevent NoSuchFileException from requiredExist() in Files.list
        if (!Files.exists(path)) {
            return null
        }

        if (Files.isRegularFile(path)) {
            VFSFile f = vfs.getFile(path.toAbsolutePath().toString(), true)
            if (c.apply(f)) {
                return f
            } else {
                return null
            }
        }

        if (!Files.list(path).iterator().hasNext()) {
            return null
        }

        List<Path> itemsInFolder = Files.list(path).collect(Collectors.toList())
        for (Path p : itemsInFolder) {
            VFSFile f = doFindFile(vfs, p, c)
            if (f != null) {
                return f
            }
        }

        return null
    }

    def <T> T findFile(Function<VFSFile, Boolean> c) {
        return findFile(this, c)
    }

    void walkFileSystem(Consumer<VFSFile> c) {
        walkFileSystem(this, c)
    }

    static void walkFileSystem(VFS vfs, Consumer<VFSFile> c) {
        findFile(vfs) {
            f -> c.accept(f)
                return false
        }
    }

    static <T> T findFile(VFS vfs, Function<VFSFile, Boolean> c) {
        Iterator<Path> iterator = vfs.fileSystem.getRootDirectories().iterator()
        while (iterator.hasNext()) {
            VFSFile f = doFindFile(vfs, iterator.next(), c)
            if (f != null) {
                return f as T
            }
        }

        return null
    }

    VFSFile createFileFrom(Path path, VFSFile f) {
        Path p = fileSystem.getPath(path.toAbsolutePath().toString())
        if (Files.exists(p)) {
            throw new FileAlreadyExistsException("file[${f.pathString()}] already exists")
        }

        createDirectories(p.getParent())
        def originPath = f.path
        f.path = p
        write(p, f.asJSONString())
        VFSFile ret = getFile(f.pathString(), true)
        f.path = originPath // restore original path
        return ret
    }

    VFSFile createFileFrom(VFSFile f) {
        Path p = fileSystem.getPath(f.pathString())
        if (Files.exists(p)) {
            throw new FileAlreadyExistsException("file[${f.pathString()}] already exists")
        }

        createDirectories(p.getParent())
        write(p, f.asJSONString())
        return getFile(f.pathString(), true)
    }

    void destroy() {
        fileSystem.close()
    }

    Qcow2Tree vfsQcow2Tree() {
        return new Qcow2Tree(this)
    }

    FileSystem getFileSystem() {
        return fileSystem
    }
}
