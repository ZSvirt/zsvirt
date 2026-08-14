package org.zstack.testlib

import com.google.common.base.Charsets
import com.google.common.io.Resources
import org.apache.commons.lang.StringUtils
import org.zstack.core.Platform
import org.zstack.header.identity.SuppressCredentialCheck
import org.zstack.header.message.*
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.rest.APINoSee
import org.zstack.header.rest.RestRequest
import org.zstack.utils.CollectionUtils
import org.zstack.utils.FieldUtils

import java.lang.reflect.Field
import java.util.stream.Collectors

/**
 * Created by xing5 on 2017/3/31.
 */
class PythonSdkGenerator {
    private String toPythonBoolean(boolean bool) {
        return StringUtils.capitalize(String.valueOf(bool))
    }

    private generateFields(Class clz, List fieldList, List annotationList) {
        def fields = FieldUtils.getAllFields(clz)

        OverriddenApiParams oap = clz.getAnnotation(OverriddenApiParams.class)
        Map<String, APIParam> overriden = [:]
        if (oap != null) {
            for (OverriddenApiParam op : oap.value()) {
                overriden.put(op.field(), op.param())
            }
        }

        for (Field f : fields) {
            if (f.isAnnotationPresent(APINoSee.class)) {
                continue
            }

            APIParam apiParam = overriden.containsKey(f.name) ? overriden[f.name] : f.getAnnotation(APIParam.class)
            fieldList.add("self.${f.name} = None")

            def ap = []
            if (apiParam != null) {
                ap.add("required=${toPythonBoolean(apiParam.required())}")
                if (apiParam.validValues().length > 0) {
                    ap.add("valid_values=[${apiParam.validValues().collect { "'" + it + "'" }.join(",")}]")
                } else if (apiParam.validEnums().length > 0) {
                    def validValues = CollectionUtils.valuesForEnums(apiParam.validEnums()).collect(Collectors.toList())
                    ap.add("valid_values=[${validValues.collect { "'" + it + "'" }.join(",")}]")
                }
                if (!apiParam.validRegexValues().isEmpty()) {
                    String regex = apiParam.validRegexValues().replace("'", "\\\'")
                    ap.add("valid_regex_values=r'${regex}'")
                }
                if (apiParam.maxLength() != Integer.MIN_VALUE) {
                    ap.add("max_length=${apiParam.maxLength()}")
                }
                if (apiParam.minLength() > 0) {
                    ap.add("min_length=${apiParam.minLength()}")
                }
                if (apiParam.numberRange().length == 2) {
                    ap.add("number_range=[${apiParam.numberRange()[0]}, ${apiParam.numberRange()[1]}]")
                }
                ap.add("non_empty=${toPythonBoolean(apiParam.nonempty())}")
                ap.add("null_elements=${toPythonBoolean(apiParam.nullElements())}")
                ap.add("empty_string=${toPythonBoolean(apiParam.emptyString())}")
                ap.add("no_trim=${toPythonBoolean(apiParam.noTrim())}")
            }

            annotationList.add("'${f.name}': ParamAnnotation(${ap.join(",")})")
        }

        if (!clz.isAnnotationPresent(SuppressCredentialCheck.class)) {
            fieldList.add("self.sessionId = None")
            annotationList.add("'sessionId': ParamAnnotation(required=False)")
            fieldList.add("self.accessKeyId = None")
            annotationList.add("'accessKeyId': ParamAnnotation(required=False)")
            fieldList.add("self.accessKeySecret = None")
            annotationList.add("'accessKeySecret': ParamAnnotation(required=False)")
        }

        fieldList.add("self.requestIp = None")
        annotationList.add("'requestIp': ParamAnnotation(required=False)")
    }

    void generate(String outputPath) {
        Set<Class> apiMessageClasses = Platform.reflections.getSubTypesOf(APIMessage.class)

        apiMessageClasses.sort { c1, c2 -> (c1.name <=> c2.name) }

        List actions = []
        apiMessageClasses.each { clz ->
            RestRequest at = clz.getAnnotation(RestRequest.class)
            if (at == null) {
                return
            }

            String name = StringUtils.removeStart(clz.getSimpleName(), "API")
            name = StringUtils.removeEnd(name, "Msg")
            String actionName = StringUtils.capitalize(name) + 'Action'

            String superClass = APIQueryMessage.class.isAssignableFrom(clz) ? "QueryAction" : "AbstractAction"
            String paramName = at.isAction() ? StringUtils.uncapitalize(name) : at.parameterName()

            def paramList = []
            def annotationList = []
            generateFields(clz, paramList, annotationList)

            paramList = paramList.collect { " " * 8 + it }
            annotationList = annotationList.collect { " " * 8 + it }

            actions.add("""\

class $actionName($superClass):
    HTTP_METHOD = '${at.method().toString()}'
    PATH = '${at.path()}'
    NEED_SESSION = ${toPythonBoolean(!clz.isAnnotationPresent(SuppressCredentialCheck.class))}
    NEED_POLL = ${toPythonBoolean(!APISyncCallMessage.class.isAssignableFrom(clz))}
    PARAM_NAME = '$paramName'

    PARAMS = {
${annotationList.join(",\n")}
    }

    def __init__(self):
        super($actionName, self).__init__()
${paramList.join("\n")}
""")
        }

        String base = Resources.toString(Resources.getResource("zssdk.py"), Charsets.UTF_8)
        base = base + "\n\n" + actions.join("\n")

        File dir = new File(outputPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        File output = new File([dir.absolutePath, 'zssdk.py'].join("/"))
        output.write(base)

        println("Python SDK is generated at ${output.absolutePath}")
    }
}
