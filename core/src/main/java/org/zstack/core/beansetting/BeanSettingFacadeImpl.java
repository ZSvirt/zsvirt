package org.zstack.core.beansetting;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 7:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class BeanSettingFacadeImpl implements BeanSettingFacade, BeanPostProcessor {
    private static final CLogger logger = Utils.getLogger(BeanSettingFacadeImpl.class);
    private Map<String, Map> settings = new HashMap<String, Map>();

    private BeanSettingFacadeImpl() {
        List<String> paths = PathUtil.scanFolderOnClassPath("settings");

        for (String p : paths) {
            if (!p.endsWith(".json")) {
                logger.warn(String.format("ignore %s which is not ending with .json", p));
                continue;
            }

            File cfg = new File(p);
            try {
                String content = FileUtils.readFileToString(cfg);
                Map setting = JSONObjectUtil.toObject(content, HashMap.class);
                String beanName = (String) setting.get("beanName");
                if (beanName == null) {
                    throw new IllegalArgumentException(String.format("Setting file[%s] doesn't have mandatory field 'beanName' which matches Spring bean that this setting file applies to", p));
                }
                setting.put(beanName, setting);
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            }
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
        return o;
    }

    @Override
    public Object postProcessAfterInitialization(Object o, String s) throws BeansException {
        Map setting = settings.get(s);
        if (setting == null) {
            return o;
        }

        return o;
    }
}
