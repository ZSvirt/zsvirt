package org.zstack.i18ntools

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.apache.commons.lang.StringUtils
import org.zstack.header.exception.CloudRuntimeException

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors

class I18nPropertiesBuilder {
    static List<String> PLACEHOLDERS = ["%s","%d","%f","%c"]
    Gson gson = new GsonBuilder().setPrettyPrinting().create()
    String dstPath, srcPath

    I18nPropertiesBuilder() {
    }

    I18nPropertiesBuilder withDstPath(String dstPath) {
        this.dstPath = dstPath
        return this
    }

    I18nPropertiesBuilder withSrcPath(String srcPath) {
        this.srcPath = srcPath
        return this
    }

    static class Metadata implements Comparable<Metadata> {
        String raw
        String en_US
        String zh_CN
        List<String> arguments
        // line number is updated everytime, and should not be stored in the JSON file
        transient int line
        String fileName

        @Override
        int compareTo(Metadata other) {
            int fileComparison = this.fileName <=> other.fileName
            if (fileComparison != 0) {
                return fileComparison
            }
            return this.line <=> other.line
        }
    }

    class GenerateJsonContext {
        Map<String, Map<String, Metadata>> moduleMetadataMap = [:], existsModuleMetadataMap = [:]

        Map<String, Metadata> createMetadataListByModule(String moduleName) {
            def map = moduleMetadataMap.get(moduleName)
            if (map == null) {
                map = [:] as Map<String, Metadata>
                moduleMetadataMap.put(moduleName, map)
            }
            return map
        }

        void putToExistsModuleMetadataMap(String moduleName, Metadata[] metadataArray) {
            def map = existsModuleMetadataMap.get(moduleName)
            if (map == null) {
                map = [:] as Map<String, Metadata>
                existsModuleMetadataMap.put(moduleName, map)
            }
            metadataArray.each {
                map.put(it.raw, it)
            }
        }

        ScriptEngine buildEngine() {
            def eng = new ScriptEngineManager().getEngineByName("JavaScript")
            if (eng == null) {
                throw new CloudRuntimeException("JavaScript engine not available. Please use JDK 11-14 or add nashorn-core dependency.")
            }
            return eng
        }
    }

    void generateJson() {
        def srcDirFile = new File(srcPath)
        if (!srcDirFile.exists() || !srcDirFile.isDirectory()) {
            throw new CloudRuntimeException("$srcDirFile does not exist, or is not a directory")
        }

        def dstDirFile = new File(dstPath)
        if (!dstDirFile.exists() || !dstDirFile.isDirectory()) {
            throw new CloudRuntimeException("$dstPath does not exist, or is not a directory")
        }

        def context = new GenerateJsonContext()

        // read existing metadata to context.existsModuleMetadataMap
        walk(dstDirFile) { File file ->
            if (!file.name.matches("i18n_[\\w-]{1,100}+\\.json")) {
                return
            }

            def moduleName = file.name.substring(5, file.name.length() - 5)
            def json = file.text
            def array = gson.fromJson(json, Metadata[])
            context.putToExistsModuleMetadataMap(moduleName, array)
        }
        assert context.existsModuleMetadataMap.size() > 0

        walk(srcDirFile) { File file ->
            if (!file.name.endsWith(".java")) {
                return
            }
            def moduleName = findModuleNameFromFile(file)
            if (moduleName == null) {
                return
            }
            println "$file => ${moduleName}"

            def metadataMap = context.createMetadataListByModule(moduleName)
            def eng = context.buildEngine()
            new VoidVisitorAdapter<Object>() {
                String buildRaw(StringLiteralExpr arg) {
                    def text = arg.toString()
                    def raw = (text.startsWith('"') ? text[1..-1] : text)
                    return (raw.endsWith('"') ? raw[0..-2] : raw)
                }

                String buildRaw(BinaryExpr arg) {
                    def temp = eng.eval(arg.toString()) as String
                    return temp.replace("\n", "\\n")
                }

                boolean isSupportRaw(Expression expression) {
                    return expression instanceof StringLiteralExpr || expression instanceof BinaryExpr
                }

                @Override
                void visit(MethodCallExpr n, Object arg) {
                    super.visit(n, arg)

                    // operr, argerr, i18n: arguments[0] is string
                    // err, multiErr: arguments[1] is string
                    def methodName = n.nameAsString
                    List<String> args
                    String raw

                    try {
                        switch (methodName) {
                        case ["operr", "argerr", "i18n", "i18m"]:
                            if (n.arguments.isEmpty() || !(isSupportRaw(n.arguments[0])))  {
                                return
                            }

                            def errArg = n.arguments[0]
                            raw = (errArg instanceof StringLiteralExpr) ?
                                    buildRaw(errArg as StringLiteralExpr) : buildRaw(errArg as BinaryExpr)
                            args = n.arguments.subList(1, n.arguments.size()).collect { it.toString() }
                            break
                        case ["multiErr", "err"]:
                            if (n.arguments.size() < 2 || !(isSupportRaw(n.arguments[1])))  {
                                return
                            }

                            def errArg = n.arguments[1]
                            raw = (errArg instanceof StringLiteralExpr) ?
                                    buildRaw(errArg as StringLiteralExpr) : buildRaw(errArg as BinaryExpr)
                            args = n.arguments.subList(2, n.arguments.size()).collect { it.toString() }
                            break
                        default:
                            return
                        }
                    } catch (javax.script.ScriptException e) {
                        println("""ScriptException occurred:
        exception: ${e.getCause()}
        script: ${n.toString()}
""")
                        return
                    }

                    def m = new Metadata()
                    m.raw = raw
                    m.zh_CN = ""
                    m.en_US = coverToI18nFormatString(raw, PLACEHOLDERS)
                    m.arguments = args
                    m.line = (n.begin.isPresent() ? n.begin.get().line : 0)
                    m.fileName = file.absolutePath.substring(file.absolutePath.indexOf("src"))
                    metadataMap[raw] = m
                }
            }.visit(JavaParser.parse(file), null)
        }

        // moduleMetadataMap 和 existsModuleMetadataMap 合并数据, 规则如下:
        // 以 moduleMetadataMap 为准. moduleMetadataMap 没有的数据, 但 existsModuleMetadataMap 存在的, 一律丢弃
        // 丢弃内容自行到 git history 中查看
        // moduleMetadataMap 会从 existsModuleMetadataMap 获取 zh_CN, en_US 值. 如果有, 则覆盖 moduleMetadataMap
        def modules = context.moduleMetadataMap.keySet()
        modules.each { moduleName ->
            def metadataMap = context.moduleMetadataMap[moduleName]
            def existsMetadataMap = context.existsModuleMetadataMap[moduleName]

            if (existsMetadataMap == null || existsMetadataMap.isEmpty()) {
                return
            }

            metadataMap.each { raw, metadata ->
                def existsMetadata = existsMetadataMap[raw]
                if (existsMetadata != null) {
                    metadata.zh_CN = existsMetadata.zh_CN
                    metadata.en_US = existsMetadata.en_US
                }
            }
        }

        context.moduleMetadataMap.each { moduleName, metadataMap ->
            def jsonFile = new File(dstDirFile, "i18n_${moduleName}.json".toString())
            println("generating ${metadataMap.size()} entries to ${jsonFile}")
            def json = gson.toJson(metadataMap.values().stream().sorted().collect(Collectors.toList()))
            Files.write(jsonFile.toPath(), json.getBytes(StandardCharsets.UTF_8))
        }
    }

    class GeneratePropertiesContext {
        Map<String, List<Metadata>> moduleMetadataMap = [:]

        void putToModuleMetadataMap(String moduleName, Metadata[] metadataArray) {
            def list = moduleMetadataMap.get(moduleName)
            if (list == null) {
                list = [] as List<Metadata>
                moduleMetadataMap.put(moduleName, list)
            }
            metadataArray.each {
                list.add(it)
            }
        }
    }

    void generateI18nPropertiesFromJson() {
        def srcDirFile = new File(srcPath)
        if (!srcDirFile.exists() || !srcDirFile.isDirectory()) {
            throw new CloudRuntimeException("$srcDirFile does not exist, or is not a directory")
        }

        def dstDirFile = new File(dstPath)
        if (!dstDirFile.exists()) {
            if (!dstDirFile.mkdirs()) {
                throw new CloudRuntimeException("cannot create directory $dstPath")
            }
        } else if (!dstDirFile.isDirectory()) {
            throw new CloudRuntimeException("$dstPath is not a directory")
        }

        def context = new GeneratePropertiesContext()
        walk(srcDirFile) { File file ->
            if (!file.name.matches("i18n_[\\w-]{1,100}+\\.json")) {
                return
            }

            def moduleName = file.name.substring(5, file.name.length() - 5)
            def json = file.text
            def array = gson.fromJson(json, Metadata[])
            context.putToModuleMetadataMap(moduleName, array)
        }

        List<String> zhList = [], enList = []
        Set<String> rawSet = new HashSet<>()
        boolean anyMiss = false

        context.moduleMetadataMap.each { moduleName, metadataList ->
            def title = "# In Module: $moduleName"
            zhList.add(title)
            enList.add(title)

            int enMiss = 0, zhMiss = 0
            metadataList.each {
                if (it.raw in rawSet) {
                    return
                }

                rawSet.add(it.raw)
                zhList.add("${escapeKey(it.raw)} = ${escapeValue(it.zh_CN)}")
                enList.add("${escapeKey(it.raw)} = ${escapeValue(it.en_US)}")

                if (it.zh_CN == null || it.zh_CN.isEmpty()) {
                    zhMiss++
                }
                if (it.en_US == null || it.en_US.isEmpty()) {
                    enMiss++
                }
            }

            if (enMiss > 0) {
                println("[WARN] there are $enMiss entries in module $moduleName not translated (en_US), you need to fix them" +
                        " in the source JSON file")
                anyMiss = true
            }
            if (zhMiss > 0) {
                println("[WARN] there are $zhMiss entries in module $moduleName not translated (zh_CN), you need to fix them" +
                        " in the source JSON file")
                anyMiss = true
            }

            zhList.add("")
            enList.add("")
        }

        // for en
        def f = Paths.get(dstDirFile.absolutePath, "messages_en_US.properties")
        def texts = enList.join("\n")
        Files.write(f, texts.getBytes(StandardCharsets.UTF_8))
        println("generated an i18n property file at ${f.toString()}")

        // for zh
        f = Paths.get(dstDirFile.absolutePath, "messages_zh_CN.properties")
        texts = zhList.join("\n")
        Files.write(f, texts.getBytes(StandardCharsets.UTF_8))
        println("generated an i18n property file at ${f.toString()}")

        if (anyMiss) {
            throw new CloudRuntimeException("there are some entries not translated, please check the generated i18n property files")
        }
    }

    static String escapeKey(String str) {
        return str.replace("\\", "\\\\")
                .replace(" ", "\\ ")
                .replace("=", "\\=")
                .replace(":", "\\:")
                .replace("\r", "\\\r")
                .replace("\n", "\\\n")
    }

    static String escapeValue(String str) {
        str = str.replace("\\", "\\\\")
                .replace("\r", "\\\r")
                .replace("\n", "\\\n")
                .replace("'", "''")

        Pattern pattern = Pattern.compile(/\{.*?}/)
        Matcher matcher = pattern.matcher(str)

        StringBuffer result = new StringBuffer()
        while (matcher.find()) {
            String value = matcher.group()
            try {
                // 尝试解析花括号内的内容是否为整数
                Integer.parseInt(value[1..-2]) // 去掉首尾的 { 和 }
                matcher.appendReplacement(result, value) // 是整数，保留原样
            } catch (NumberFormatException ignored) {
                matcher.appendReplacement(result, "'" + value + "'") // 不是整数，用单引号包裹
            }
        }
        matcher.appendTail(result)
        return result.toString()
    }

    static String coverToI18nFormatString(String s, List<String> placeholders) {
        // convert "abc %s d %s" to "abc {0} d {1}"
        int c = 0
        def record = [:] as Map<Integer, String>
        for (p in placeholders) {
            c += StringUtils.countMatches(s, p)

            int index = 0
            int position = s.indexOf(p, index)
            while (position >= 0) {
                record.put(position, p)
                index = position + 1
                position = s.indexOf(p, index)
            }
        }

        def placeholderIndexes = (record.size() > 0) ? record.keySet() : new HashSet<Integer>()
        String out = s
        for (i in 0..<c) {
            int temp = placeholderIndexes.min()
            out = out.replaceFirst(record.get(temp), "{$i}")
            placeholderIndexes.remove(temp)
        }

        return out
    }

    static void walk(File dir, Closure<Void> handler) {
        for (f in dir.listFiles()) {
            if (f.isDirectory()) {
                walk(f, handler)
                continue
            }

            handler.call(f)
        }
    }

    /**
     * zstack\premium\accesskey\src\main\java\org\zstack\accessKey\AccessKeyApiInterceptor.java => 'accessKey'
     */
    static String findModuleNameFromFile(File dirFile) {
        int srcIndex = dirFile.absolutePath.indexOf("src")
        if (srcIndex <= 0) {
            return null
        }
        def moduleDir = dirFile.absolutePath.substring(0, srcIndex)
        def modulePath = Paths.get(moduleDir).getFileName()
        return modulePath != null ? modulePath.toString() : null
    }
}
