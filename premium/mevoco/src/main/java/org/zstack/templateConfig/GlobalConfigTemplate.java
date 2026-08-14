package org.zstack.templateConfig;


import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GlobalConfigTemplate {
    private static final CLogger logger = Utils.getLogger(GlobalConfigTemplate.class);
    private String uuid;
    private String name;
    private String type;
    private String description;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;

    public EventFacade getEvtf() {
        return evtf;
    }

    @Override
    public String toString() {
        return JSONObjectUtil.toJsonString(map(
                e("uuid", uuid),
                e("name", name),
                e("type", type),
                e("description", description)
        ));
    }


    public GlobalConfigTemplate(String  uuid, String type, String name, String description) {
        this.uuid = uuid;
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public GlobalConfigTemplate() {
    }


    public GlobalConfigTemplateVO reload() {
        SimpleQuery<GlobalConfigTemplateVO> q = dbf.createQuery(GlobalConfigTemplateVO.class);
        q.add(GlobalConfigTemplateVO_.type, Op.EQ, type);
        q.add(GlobalConfigTemplateVO_.name, Op.EQ, name);
        return q.find();
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    public String getDescription() { return description; }

    void setDescription(String description) { this.description = description; }

    public static GlobalConfigTemplate valueOf(GlobalConfigTemplateVO vo) {
        GlobalConfigTemplate t = new GlobalConfigTemplate();
        t.setName(vo.getName());
        t.setType(vo.getType());
        t.setUuid(vo.getUuid());
        return t;
    }

    public static GlobalConfigTemplate valueOf(GlobalConfigTemplate old) {
        GlobalConfigTemplate t = new GlobalConfigTemplate();
        t.setName(old.getName());
        t.setType(old.getType());
        return t;
    }

    public GlobalConfigTemplateVO toVO() {
        GlobalConfigTemplateVO vo = new GlobalConfigTemplateVO();
        vo.setName(name);
        vo.setType(type);
        vo.setUuid(uuid);
        vo.setDescription(description);
        return vo;
    }

    public static GlobalConfigTemplate valueOf(org.zstack.templateConfig.schema.GlobalConfigTemplate.Config c) {
        GlobalConfigTemplate t = new GlobalConfigTemplate();
        t.setName(c.getName());
        t.setType(c.getType());
        t.setUuid(c.getUuid());
        t.setDescription(c.getDescription());
        return t;
    }

    public String getIdentity() {
        return produceIdentity(type, name);
    }

    public static String produceIdentity(String type, String name) {
        return String.format("%s.%s", type, name);
    }

    public boolean isMe(GlobalConfigTemplate other) {
        return type.equals(other.getType()) && name.equals(other.getName());
    }


}
