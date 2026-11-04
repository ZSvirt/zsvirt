package org.zstack.configuration;

import javassist.Modifier;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.zstack.header.configuration.NoPython;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.APISessionMessage;
import org.zstack.header.message.APIListMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.search.APIGetMessage;
import org.zstack.header.search.APISearchMessage;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PythonApiActionGenerator {
    public static void generatePythonApiAction(List<String> basePkgs, String resultFolder) throws IOException {
        StringBuilder pysb = new StringBuilder();
        pysb.append(String.format("from apibinding import inventory"));
        pysb.append(String.format("\nfrom apibinding import api"));
        pysb.append(String.format("\nfrom zstacklib.utils import jsonobject"));

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(APIMessage.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(NoPython.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(Component.class));
        for (String pkg : basePkgs) {
            List<Class<?>> clazzList = new ArrayList<>();
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                try {
                    Class<?> clazz = Class.forName(bd.getBeanClassName());
                    if (Modifier.isAbstract(clazz.getModifiers())) {
                        continue;
                    }

                    if (!clazz.isAnnotationPresent(RestRequest.class)) {
                        continue;
                    }

                    if (TypeUtils.isTypeOf(clazz, APISearchMessage.class, APIGetMessage.class, APIListMessage.class)) {
                        continue;
                    }

                    clazzList.add(clazz);
                } catch (Exception e) {
                    throw new CloudRuntimeException(e);
                }
            }
            List<Class<?>> sortedClazzList = clazzList.stream().sorted(
                    (c1, c2) -> {
                        return populateActionName(c1).compareTo(populateActionName(c2));
                    }
            ).collect(Collectors.toList());
            for (Class<?> clazz : sortedClazzList) {
                if (APIQueryMessage.class.isAssignableFrom(clazz)) {
                    generateQueryMsg(pysb, clazz);
                } else {
                    generateApiMsg(pysb, clazz);
                }
            }
        }

        String pyStr = pysb.toString();
        FileUtils.write(new File(PathUtil.join(resultFolder, "api_actions.py")), pyStr);
    }

    private static void generateApiMsg(StringBuilder pysb, Class<?> clazz) {
        String actionName = populateActionName(clazz);
        pysb.append(String.format("\n\nclass %s(inventory.%s):", actionName, clazz.getSimpleName()));
        pysb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)));
        pysb.append(String.format("\n%ssuper(%s, self).__init__()", whiteSpace(8), actionName));
        pysb.append(String.format("\n%sself.sessionUuid = None", whiteSpace(8)));
        pysb.append(String.format("\n%sself.out = None", whiteSpace(8)));
        pysb.append(String.format("\n%sdef run(self):", whiteSpace(4)));
        if (!APISessionMessage.class.isAssignableFrom(clazz)) {
            pysb.append(String.format("\n%sif not self.sessionUuid:", whiteSpace(8)));
            pysb.append(String.format("\n%sraise Exception('sessionUuid of action[%s] cannot be None')", whiteSpace(12), actionName));
        }
        pysb.append(String.format("\n%sevt = api.async_call(self, self.sessionUuid)", whiteSpace(8)));
        pysb.append(String.format("\n%sself.out = evt", whiteSpace(8)));
        pysb.append(String.format("\n%sreturn self.out", whiteSpace(8)));
    }

    private static void generateSearchMsg(StringBuilder pysb, Class<?> clazz) {
        String actionName = populateActionName(clazz);
        pysb.append(String.format("\n\nclass %s(inventory.%s):", actionName, clazz.getSimpleName()));
        pysb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)));
        pysb.append(String.format("\n%ssuper(%s, self).__init__()", whiteSpace(8), actionName));
        pysb.append(String.format("\n%sself.sessionUuid = None", whiteSpace(8)));
        pysb.append(String.format("\n%sself.out = None", whiteSpace(8)));
        pysb.append(String.format("\n%sdef run(self):", whiteSpace(4)));
        pysb.append(String.format("\n%sif not self.sessionUuid:", whiteSpace(8)));
        pysb.append(String.format("\n%sraise Exception('sessionUuid of action[%s] cannot be None')", whiteSpace(12), actionName));
        pysb.append(String.format("\n%sreply = api.sync_call(self, self.sessionUuid)", whiteSpace(8)));
        pysb.append(String.format("\n%sself.out = jsonobject.loads(reply.content)", whiteSpace(8)));
        pysb.append(String.format("\n%sreturn self.out", whiteSpace(8)));
    }

    private static void generateQueryMsg(StringBuilder pysb, Class<?> clazz) {
        String actionName = populateActionName(clazz);
        pysb.append(String.format("\n\nclass %s(inventory.%s):", actionName, clazz.getSimpleName()));
        pysb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)));
        pysb.append(String.format("\n%ssuper(%s, self).__init__()", whiteSpace(8), actionName));
        pysb.append(String.format("\n%sself.sessionUuid = None", whiteSpace(8)));
        pysb.append(String.format("\n%sself.reply = None", whiteSpace(8)));
        pysb.append(String.format("\n%sself.out = None", whiteSpace(8)));
        pysb.append(String.format("\n%sdef run(self):", whiteSpace(4)));
        pysb.append(String.format("\n%sif not self.sessionUuid:", whiteSpace(8)));
        pysb.append(String.format("\n%sraise Exception('sessionUuid of action[%s] cannot be None')", whiteSpace(12), actionName));
        pysb.append(String.format("\n%sreply = api.sync_call(self, self.sessionUuid)", whiteSpace(8)));
        pysb.append(String.format("\n%sself.reply = reply", whiteSpace(8)));
        pysb.append(String.format("\n%sself.out = reply.inventories", whiteSpace(8)));
        pysb.append(String.format("\n%sreturn self.out", whiteSpace(8)));
    }

    private static void generateGetMsg(StringBuilder pysb, Class<?> clazz) {
        String actionName = populateActionName(clazz);
        pysb.append(String.format("\n\nclass %s(inventory.%s):", actionName, clazz.getSimpleName()));
        pysb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)));
        pysb.append(String.format("\n%ssuper(%s, self).__init__()", whiteSpace(8), actionName));
        pysb.append(String.format("\n%sself.sessionUuid = None", whiteSpace(8)));
        pysb.append(String.format("\n%sself.out = None", whiteSpace(8)));
        pysb.append(String.format("\n%sdef run(self):", whiteSpace(4)));
        pysb.append(String.format("\n%sif not self.sessionUuid:", whiteSpace(8)));
        pysb.append(String.format("\n%sraise Exception('sessionUuid of action[%s] cannot be None')", whiteSpace(12), actionName));
        pysb.append(String.format("\n%sreply = api.sync_call(self, self.sessionUuid)", whiteSpace(8)));
        pysb.append(String.format("\n%sself.out = jsonobject.loads(reply.inventory)", whiteSpace(8)));
        pysb.append(String.format("\n%sreturn self.out", whiteSpace(8)));
    }

    private static String populateActionName(Class<?> clazz) {
        String name = clazz.getSimpleName().replace("API", "").replace("Msg", "");
        name = String.format("%sAction", name);
        return WordUtils.capitalize(name);
    }

    private static String whiteSpace(int num) {
        return StringUtils.repeat(" ", num);
    }

    private static void generateListMsg(StringBuilder pysb, Class<?> clazz) {
        String actionName = populateActionName(clazz);
        pysb.append(String.format("\n\nclass %s(inventory.%s):", actionName, clazz.getSimpleName()));
        pysb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)));
        pysb.append(String.format("\n%ssuper(%s, self).__init__()", whiteSpace(8), actionName));
        pysb.append(String.format("\n%sself.sessionUuid = None", whiteSpace(8)));
        pysb.append(String.format("\n%sself.out = None", whiteSpace(8)));
        pysb.append(String.format("\n%sdef run(self):", whiteSpace(4)));
        pysb.append(String.format("\n%sif not self.sessionUuid:", whiteSpace(8)));
        pysb.append(String.format("\n%sraise Exception('sessionUuid of action[%s] cannot be None')", whiteSpace(12), actionName));
        pysb.append(String.format("\n%sreply = api.sync_call(self, self.sessionUuid)", whiteSpace(8)));
        pysb.append(String.format("\n%sself.out = reply.inventories", whiteSpace(8)));
        pysb.append(String.format("\n%sreturn self.out", whiteSpace(8)));
    }
}
