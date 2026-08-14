package org.zstack.testlib.tool

import org.apache.logging.log4j.util.Strings
import org.zstack.core.Platform
import org.zstack.header.vo.EO
import org.zstack.header.vo.ResourceVO
import org.zstack.utils.path.PathUtil

import javax.persistence.Entity
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class TermsGenerator {
    static String termFileFormat = "terms_%s.yaml"

    static String translationFileFormat = "translation_from_%s_to_%s.yaml"

    static List<TermTranslatePropertyLine> generateTermsOfZStackVersion(String version) {
        String outputDir = System.getProperty("user.dir")

        Set<Class> resourceVOs = Platform.reflections.getSubTypesOf(ResourceVO.class)
        resourceVOs = resourceVOs.findAll { return it.isAnnotationPresent(Entity.class) && !it.isAnnotationPresent(EO.class) }

        List<TermTranslatePropertyLine> linesFromProperties = loadFileFromResource(PathUtil.join(outputDir, '../../conf', String.format(termFileFormat, version)))

        List<String> termKeysFromResourceConfig = resourceVOs.collect {
            // VmInstanceVO -> VmInstance
            return it.simpleName.replaceAll("VO\$", "").replaceAll("EO\$", "")
        }.sort()

        List<String> missingTerms = termKeysFromResourceConfig.findAll { termKey ->
            return !linesFromProperties.any { it.term == termKey }
        }

        List<TermTranslatePropertyLine> linesToWrite = linesFromProperties + missingTerms.collect {
            return new TermTranslatePropertyLine(it, "")
        }

        String content = linesToWrite.collect { it.toString() }.join("\n")

        new File(PathUtil.join(outputDir, "../../conf", String.format(termFileFormat, version))).write(content, Charset.forName("UTF-8").toString())
        return linesToWrite
    }

    static generateTermsFromJpaEntities(String replaceTo) {
        List<TermTranslatePropertyLine> cloudTerms = generateTermsOfZStackVersion("cloud")
        List<TermTranslatePropertyLine> zsvTerms = generateTermsOfZStackVersion("zsv")

        if (replaceTo == null) {
            replaceTo = "cloud"
        }

        Map<String, TermTranslatePropertyLine> replaceMap = [:]
        if (replaceTo == "cloud") {
            cloudTerms.each {
                replaceMap.put(it.term, it)
            }

            zsvTerms.each {
                if (replaceMap.containsKey(it.term)) {
                    replaceMap.get(it.term).replaceTo = it.translation
                }
            }
        } else if (replaceTo == "zsv") {
            zsvTerms.each {
                replaceMap.put(it.term, it)
            }

            cloudTerms.each {
                if (replaceMap.containsKey(it.term)) {
                    replaceMap.get(it.term).replaceTo = it.translation
                }
            }
        }

        String outputDir = System.getProperty("user.dir")
        String content = replaceMap
                .values()
                .findAll { Strings.isNotBlank(it.translation) && Strings.isNotBlank(it.replaceTo) }
                .collect { it.toTranslationString() }
                .join("\n")
        new File(PathUtil.join(outputDir, '../../conf', String.format(translationFileFormat, replaceTo, replaceTo == "cloud" ? "zsv" : "cloud"))).write(content, Charset.forName("UTF-8").toString())

        List<TermTranslatePropertyLine> replaceList = replaceMap
                .values()
                .findAll { Strings.isNotBlank(it.translation) && Strings.isNotBlank(it.replaceTo) }

        replaceList.each {
            println "replace ${it.translation} to ${it.replaceTo}"
        }

        String propertyFileName = "messages_zh_CN.properties"
        String outputFileName = "messages_${replaceTo == "cloud" ? "zsv" : "cloud"}_zh_CN.properties"
        replaceI18nProperties(outputDir, '../../conf/i18n', propertyFileName, "../conf/i18n", outputFileName, replaceList)
    }

    static void replaceI18nProperties(String workDir, String sourceDir, String fileName, String destDir, String destFileName, List<TermTranslatePropertyLine> replaceList) {
        // replace terms in i18n messages_zh_CN.properties of every lines
        String i18nConf = PathUtil.join(workDir, sourceDir, fileName)
        List<String> outputLines = []
        if (PathUtil.exists(i18nConf)) {
            for (String line : new File(i18nConf).readLines(Charset.forName("UTF-8").toString())) {
                replaceList.each {
                    line = line.replaceAll(it.translation, it.replaceTo)
                }

                outputLines.add(line)
                outputLines.add("\n")
            }
        }

        File f = new File(PathUtil.join(workDir, destDir, destFileName));
        f.withOutputStream({ os ->
            os.withWriter(StandardCharsets.UTF_8.toString()) { writer ->
                outputLines.each { line ->
                    writer.write(line)
                }
            }
        })
    }

    static List<TermTranslatePropertyLine> loadFileFromResource(String fileName) {
        if (!PathUtil.exists(fileName)) {
            return new ArrayList<TermTranslatePropertyLine>()
        }

        List<TermTranslatePropertyLine> results = []
        for (String line : new File(fileName).readLines(Charset.forName("UTF-8").toString())) {
            println "not processed line: ${line}"
            String[] tokens = line.split("=")

            String token1
            if (tokens.size() > 1)
                token1 = tokens[1].trim()
            else
                token1 = ""

            TermTranslatePropertyLine propertyLine = new TermTranslatePropertyLine(tokens[0].trim(), token1)
            println "line: ${propertyLine}"
            results.add(propertyLine)
        }

        return results
    }

    static class TermTranslatePropertyLine {
        TermTranslatePropertyLine(String term, String translation) {
            this.term = new String(term.getBytes(), Charset.forName("UTF-8").toString())
            this.translation = new String(translation.getBytes(), Charset.forName("UTF-8").toString())
        }

        String term
        String translation
        String replaceTo

        String toString() {
            return String.format("%s=%s", term, translation)
        }

        String toTranslationString() {
            return String.format("%s=%s", translation, replaceTo)
        }
    }
}

