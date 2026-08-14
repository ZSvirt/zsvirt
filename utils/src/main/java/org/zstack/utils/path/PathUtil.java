package org.zstack.utils.path;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class PathUtil {
    private static final CLogger logger = Utils.getLogger(PathUtil.class);
    public static String HOME_DIR_PROPERTY_NAME = "user.home";
    public static String join(String... paths) {
        assert paths != null && paths.length > 0;

        File parent = new File(paths[0]);
        for (int i = 1; i < paths.length; i++) {
            parent = new File(parent, paths[i]);
        }
        return parent.getPath();
    }

    public static String absPath(String path) {
        if (path.startsWith("~")) {
            path = path.replaceAll("~", System.getProperty(HOME_DIR_PROPERTY_NAME));
        }

        return new File(path).getAbsolutePath();
    }

    public static String getZStackHomeFolder() {
        String homeDir = System.getProperty(HOME_DIR_PROPERTY_NAME);
        File f = new File(homeDir);
        if (!f.exists()) {
            f.mkdirs();
        }
        return f.getAbsolutePath();
    }

    public static String getFolderUnderZStackHomeFolder(String folder) {
        String path = join(getZStackHomeFolder(), folder);
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
        return f.getAbsolutePath();
    }

    public static String getFilePathUnderZStackHomeFolder(String path) {
        String folder = getFolderUnderZStackHomeFolder(parentFolder(path));
        return join(folder, new File(path).getName());
    }

    public static String parentFolder(String fullPath) {
        if (!fullPath.contains(File.separator)) {
            return fullPath;
        }

        return fullPath.substring(0, fullPath.lastIndexOf(File.separator));
    }

    public static String fileName(String fullPath) {
        return new File(fullPath).getName();
    }

    public static File findFileOnClassPath(String fileName, boolean exceptionOnNotFound) {
        File f = findFileOnClassPath(fileName);
        if (f == null && exceptionOnNotFound) {
            throw new RuntimeException(String.format("unable to find file[%s] on classpath", fileName));
        }

        return f;
    }

    public static boolean exists(String path) {
        File f = new File(path);
        return f.exists();
    }

    public static File findFolderOnClassPath(String folderName, boolean exceptionOnNotFound) {
        File folder = findFolderOnClassPath(folderName);
        if (folder == null && exceptionOnNotFound) {
            throw new RuntimeException(String.format("The folder %s is not found on classpath or there is another resource has the same name.", folderName));
        }

        return folder;
    }

    public static File findFolderOnClassPath(String folderName) {
        URL folderUrl = PathUtil.class.getClassLoader().getResource(folderName);
        if (folderUrl == null || !folderUrl.getProtocol().equals("file")) {
            logger.warn(String.format("The folder %s is not found on classpath or there is another resource has the same name.", folderName));
            return null;
        }

        try {
            File folder = new File(folderUrl.toURI());
            if (!folder.isDirectory()) {
                return null;
            }

            return folder;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static File findFileOnClassPath(String fileName) {
        URL fileUrl = PathUtil.class.getClassLoader().getResource(fileName);

        if (fileUrl != null && fileUrl.getProtocol().equals("jar")) {
            InputStream is = PathUtil.class.getClassLoader().getResourceAsStream(fileName);
            File file = new File(fileName);

            File parentFile = file.getParentFile();
            try {
                if (parentFile != null && !parentFile.exists()) {
                    parentFile.mkdirs();
                }

                if (!file.exists() && !file.createNewFile()) {
                    logger.error(String.format("fail to create new File[%s]", file));
                }
            } catch (IOException e) {
                logger.error(String.format("create file error: %s", e.getMessage()));
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is));
                 BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
                String s = "";
                while ((s = br.readLine()) != null) {
                    out.write(s);
                }
                return file;
            } catch (IOException e) {
                logger.error(String.format("read jar resource error: %s", e.getMessage()));

                if (!file.delete()) {
                    logger.warn(String.format("failed to delete file[%s]", file));
                }
                return null;
            }
        }

        if (fileUrl == null || !fileUrl.getProtocol().equals("file")) {
            logger.warn(String.format("The file %s is not found in classpath or there is another resource has the same name.", fileName));
            return null;
        }

        try {
            return new File(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean compareFileByMd5(File src, File dst) {
        try (FileInputStream srcIn = new FileInputStream(src);
             FileInputStream dstIn = new FileInputStream(dst)) {
            String srcMd5 = DigestUtils.md5Hex(srcIn);
            String dstMd5 = DigestUtils.md5Hex(dstIn);
            return srcMd5.equals(dstMd5);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param src not null, no directory
     * @param expectMd5 not null, example: db4d30c06fb3506e6669c9709efe43c5
     */
    public static boolean checkFileByMd5(File src, String expectMd5) {
        if (expectMd5 == null || !expectMd5.matches("^[0-9a-z]{32}$")) {
            throw new RuntimeException(String.format("not valid md5sum %s", expectMd5));
        }

        if (!src.exists()) {
            throw new RuntimeException(String.format("file[%s] is not exist", src));
        }
        if (src.isDirectory()) {
            throw new RuntimeException(String.format("file[%s] is a directory", src));
        }
        if (!src.canRead()) {
            throw new RuntimeException(String.format("file[%s] is not readable", src));
        }
        
        try (FileInputStream srcIn = new FileInputStream(src)) {
            return expectMd5.equalsIgnoreCase(DigestUtils.md5Hex(srcIn));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> scanFolderOnClassPath(String folderName) {
        URL folderUrl = PathUtil.class.getClassLoader().getResource(folderName);
        if (folderUrl == null || !folderUrl.getProtocol().equals("file")) {
            String info = String.format("The folder %s is not found in classpath or there is another resource has the same name.", folderName);
            logger.warn(info);
            return new ArrayList<String>();
        }

        try {
            File folder = new File(folderUrl.toURI());
            List<String> ret = new ArrayList<>();
            scanFolder(ret, folder.getAbsolutePath());
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to locate service portal configure files: %s", e.getMessage()), e);
        }
    }

    public static void scanFolder(List<String> ret, String folderName) {
        try {
            File folder = new File(folderName);
            if (!folder.isDirectory()) {
                return;
            }

            File[] fileArray = folder.listFiles();
            if (fileArray == null) {
                return;
            }

            for (File f : fileArray) {
                if (f.isDirectory()) {
                    scanFolder(ret, f.getAbsolutePath());
                } else {
                    ret.add(PathUtil.join(folder.getAbsolutePath(), f.getName()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to locate service portal configure files: %s", e.getMessage()), e);
        }
    }

    public static void forceRemoveFile(String path) {
        try {
            File f = new File(path);
            boolean success = f.delete();
            logger.warn(String.format("Delete %s status: %s", path, success));
        } catch (Exception e) {
            logger.warn(String.format("Failed in deleting file: %s", path));
        }
    }

    public static void forceRemoveDirectory(String path) {
        try {
            FileUtils.deleteDirectory(new File(path));
            logger.warn(String.format("Deleted directory: %s", path));
        } catch (IOException ex) {
            logger.warn(String.format("Failed in deleting directory: %s: %s", path, ex.getMessage()));
        }
    }

    public static void forceCreateDirectory(String path) {
        try {
            FileUtils.forceMkdir(new File(path));
        } catch (IOException ex) {
            logger.warn(String.format("Failed in creating directory: %s: %s", path, ex.getMessage()));
        }
    }

    public static void copyFolderToFolder(String srcFolder, String dstFolder) {
        try {
            FileUtils.copyDirectory(new File(srcFolder), new File(dstFolder));
        } catch (IOException ex) {
            logger.warn(String.format("Copy directory %s to %s: %s", srcFolder, dstFolder, ex.getMessage()));
        }
    }

    /**
     * @see #setFilePosixPermissions(File, String)
     */
    public static void setFilePosixPermissions(String path, String perms) {
        setFilePosixPermissions(new File(path), perms);
    }

    /**
     * @param perms permission like: "rwxr-xr-x" "rw-------"
     */
    public static void setFilePosixPermissions(File file, String perms) {
        try {
            Set<PosixFilePermission> s = PosixFilePermissions.fromString(perms);
            Files.setPosixFilePermissions(file.toPath(), s);
        } catch (IOException ex) {
            logger.warn(String.format("set %s permission to %s: %s", file, perms, ex.getMessage()));
        }
    }

    public static String createTempDirectory() {
        try {
            return Files.createTempDirectory("tmp").toAbsolutePath().normalize().toString();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String createTempFile(String prefix, String suffix) {
        try {
            return Files.createTempFile(prefix, suffix).toAbsolutePath().normalize().toString();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void writeFile(String fpath, String content) throws IOException {
        writeFile(new File(fpath), content.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeFile(String fpath, byte[] data) throws IOException {
        writeFile(new File(fpath), data);
    }

    public static void writeFile(File file, String content) throws IOException {
        writeFile(file, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeFile(File file, byte[] data) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(data);
            outputStream.flush();
        }
    }

    public static String createTempFileWithContent(String content) {
        String tmpFile = null;
        try {
            tmpFile = Files.createTempFile("zs-", ".tmp").toAbsolutePath().normalize().toString();
            writeFile(tmpFile, content);
            return tmpFile;
        } catch (IOException e) {
            Optional.ofNullable(tmpFile).ifPresent(PathUtil::forceRemoveFile);
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void moveFile(String source, String target) {
        try {
            FileUtils.moveFile(new File(source), new File(target));
        } catch (IOException e){
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String readFileToString(String path, Charset charset) {
        try (UnicodeReader reader = new UnicodeReader(new FileInputStream(new File(path)), charset.toString())) {
            return IOUtils.toString(reader);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static boolean isValidPath(String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException | NullPointerException ex) {
            return false;
        }
        return true;
    }
    
    public static boolean chown(File file, String userOwner, String groupOwner) {
        boolean success = true;
        FileSystem fs = FileSystems.getDefault();
        UserPrincipalLookupService lookup = fs.getUserPrincipalLookupService();
        
        if (userOwner != null) {
            try {
                UserPrincipal newOwner = lookup.lookupPrincipalByName(userOwner);
                Files.setOwner(file.toPath(), newOwner);
            } catch (IOException e) {
                success = false;
            }
        }
        
        if (groupOwner != null) {
            try {
                GroupPrincipal group = lookup.lookupPrincipalByGroupName(groupOwner);
                Files.getFileAttributeView(file.toPath(), PosixFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS).setGroup(group);
            } catch (IOException e) {
                success = false;
            }
        }
        
        return success;
    }

    /**
     * If 'parentPath' is parent path of 'childPath', return true.
     * 'parentPath' and 'childPath' must be absolute path.
     * @param parentPath  must be absolute path
     * @param childPath  must be absolute path
     */
    public static boolean isParentPath(String childPath, String parentPath) {
        Path child = Paths.get(childPath).normalize();
        final Path parent = Paths.get(parentPath).normalize();

        try {
            while (true) {
                if (Files.isSameFile(child, parent)) {
                    return true;
                }
                Path nextFile = child.getParent();
                if (nextFile == null || Files.isSameFile(nextFile, child)) {
                    return false;
                }
                child = nextFile;
            }
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isDir(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file.isDirectory();
        } else {
            return false;
        }
    }

    public static Long getFileInode(String filePath) {
        Path path = Paths.get(filePath);
        try {
            return (Long) Files.getAttribute(path, "unix:ino");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static FileTime getFileLastModifiedTime(String filePath) {
        Path path = Paths.get(filePath);
        try {
            return Files.getLastModifiedTime(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String normalizePathWithoutQuery(String path) {
        try {
            String normalizedPath = path.replaceFirst(
                    "^([a-zA-Z]+:)(?!/{2})",
                    "$1//");
            final String query = new URI(normalizedPath).getQuery();
            if (query != null) {
                return StringUtils.removeEnd(path, '?' + query);
            }
        } catch (URISyntaxException ignored) {
            logger.warn(String.format("unexpected path: %s", path));
        }

        return path;
    }
}