package org.zstack.test.checker.code

import org.junit.Test
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Scan project text files and shebang scripts; fail if any use CRLF line endings.
 */
class LFFileChecker {
    private static final CLogger logger = Utils.getLogger(LFFileChecker.class)

    private static final Set<String> SKIP_DIR_NAMES = [
            ".git", ".idea", ".vscode", ".gradle", "target", "node_modules",
            "out", "build", ".settings", ".classpath", ".project"
    ] as Set

    private static final Set<String> TEXT_EXTENSIONS = [
            "java", "groovy", "xml", "properties", "yml", "yaml", "json", "md", "txt",
            "sh", "bash", "zsh", "py", "rb", "pl", "php", "js", "ts", "jsx", "tsx",
            "css", "scss", "less", "html", "htm", "sql", "cql", "conf", "cfg", "ini",
            "csv", "tsv", "gradle", "kts", "kt", "go", "rs", "c", "h", "cpp", "hpp",
            "cc", "hh", "cmake", "mk", "makefile", "dockerfile", "toml", "env",
            "service", "spec", "in", "ac", "am", "m4", "ldif", "j2", "template",
            "rst", "adoc", "svg", "proto", "feature", "gitignore", "dockerignore",
            "gitattributes", "editorconfig", "npmrc", "eslintrc", "prettierrc",
            "babelrc", "map", "list", "allow", "deny", "policy", "rules", "script",
            "inc", "def", "lds", "sbt", "pom", "xsd", "xsl", "xslt", "dtd", "wsdl",
            "wadl", "plist", "strings", "gradlew", "bat", "cmd", "ps1", "vbs"
    ] as Set

    private static final Set<String> TEXT_FILE_NAMES = [
            "Dockerfile", "Makefile", "Rakefile", "Gemfile", "Vagrantfile",
            "Jenkinsfile", "README", "LICENSE", "CHANGELOG", "NOTICE", "AUTHORS",
            "CONTRIBUTING", "gradlew", "mvnw", "configure", "install-sh"
    ] as Set

    @Test
    void testLineEndingsAreLf() {
        Path root = findProjectRoot()
        logger.info("LFFileChecker scan root: ${root}")

        List<String> crlfFiles = []
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (SKIP_DIR_NAMES.contains(dir.fileName?.toString())) {
                    return FileVisitResult.SKIP_SUBTREE
                }
                return FileVisitResult.CONTINUE
            }

            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!attrs.isRegularFile()) {
                    return FileVisitResult.CONTINUE
                }
                if (!shouldCheck(file)) {
                    return FileVisitResult.CONTINUE
                }
                if (containsCarriageReturn(file)) {
                    crlfFiles.add(root.relativize(file).toString().replace('\\', '/'))
                }
                return FileVisitResult.CONTINUE
            }
        })

        if (!crlfFiles.isEmpty()) {
            throw new CloudRuntimeException(
                    "CRLF line endings found in ${crlfFiles.size()} file(s):\n" + crlfFiles.join("\n"))
        }
        logger.info("LFFileChecker - PASS, scanned text/shebang files under ${root}")
    }

    private static Path findProjectRoot() {
        String basedir = System.getProperty("basedir", System.getProperty("user.dir"))
        Path moduleDir = Paths.get(basedir).toAbsolutePath().normalize()
        // tests/checker -> repository root
        Path root = moduleDir.resolve("../..").normalize()
        if (Files.exists(root.resolve("pom.xml"))) {
            return root
        }
        throw new CloudRuntimeException("unable to locate project root from basedir[${basedir}]")
    }

    private static boolean shouldCheck(Path file) {
        String name = file.fileName.toString()
        if (TEXT_FILE_NAMES.contains(name)) {
            return true
        }
        String ext = extensionOf(name)
        if (ext && TEXT_EXTENSIONS.contains(ext)) {
            return true
        }
        return hasShebang(file)
    }

    private static String extensionOf(String name) {
        int idx = name.lastIndexOf('.')
        if (idx <= 0 || idx == name.length() - 1) {
            return ""
        }
        return name.substring(idx + 1).toLowerCase()
    }

    private static boolean hasShebang(Path file) {
        try {
            InputStream input = Files.newInputStream(file)
            try {
                byte[] head = new byte[2]
                int n = input.read(head)
                return n == 2 && head[0] == (byte) '#' && head[1] == (byte) '!'
            } finally {
                input.close()
            }
        } catch (IOException ignored) {
            return false
        }
    }

    private static boolean containsCarriageReturn(Path file) {
        InputStream input = Files.newInputStream(file)
        try {
            byte[] buf = new byte[8192]
            int n
            while ((n = input.read(buf)) != -1) {
                for (int i = 0; i < n; i++) {
                    if (buf[i] == (byte) '\r') {
                        return true
                    }
                }
            }
            return false
        } finally {
            input.close()
        }
    }
}
