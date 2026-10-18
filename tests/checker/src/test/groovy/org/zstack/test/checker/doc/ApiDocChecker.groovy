package org.zstack.test.checker.doc

import org.junit.Test
import org.zstack.header.exception.CloudRuntimeException

import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.regex.Matcher
import java.util.regex.Pattern

class ApiDocChecker {
    private static final Set<String> SKIP_DIR_NAMES = [
            ".git", ".idea", ".vscode", ".gradle", "target", "node_modules",
            "out", "build", ".settings"
    ] as Set

    private static final Pattern SINCE_PATTERN = Pattern.compile(/since\s+"([^"]*)"/)

    @Test
    void checkSinceField() {
        Set<String> supported = loadSupportedSince()
        Path root = findProjectRoot()
        Set<String> errors = new TreeSet<>()

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
                String name = file.fileName?.toString()
                if (name == null || !name.endsWith("Doc_zh_cn.groovy")) {
                    return FileVisitResult.CONTINUE
                }

                String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8)
                Matcher matcher = SINCE_PATTERN.matcher(text)
                String relativePath = root.relativize(file).toString().replace('\\', '/')
                while (matcher.find()) {
                    String since = matcher.group(1)
                    if (!supported.contains(since)) {
                        errors.add("${relativePath}: found unsupported since \"${since}\"")
                    }
                }
                return FileVisitResult.CONTINUE
            }
        })

        errors.each { println it }
        assert errors.isEmpty()
    }

    private static Set<String> loadSupportedSince() {
        InputStream input = ApiDocChecker.class.getResourceAsStream("supports-since.txt")
        if (input == null) {
            throw new CloudRuntimeException("cannot find supports-since.txt")
        }
        Set<String> supported = new HashSet<>()
        try {
            input.withReader(StandardCharsets.UTF_8.name()) { reader ->
                reader.eachLine { line ->
                    String value = line?.trim()
                    if (value) {
                        supported.add(value)
                    }
                }
            }
        } finally {
            input.close()
        }
        return supported
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
}
