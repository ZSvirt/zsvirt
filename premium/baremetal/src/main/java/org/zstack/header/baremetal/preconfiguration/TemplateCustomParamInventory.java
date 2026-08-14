package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.search.Inventory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2018-12-28.
 */
@Inventory(mappingVOClass = TemplateCustomParamVO.class)
public class TemplateCustomParamInventory {
    private String templateUuid;
    private String param;

    public static TemplateCustomParamInventory valueOf(TemplateCustomParamVO vo) {
        TemplateCustomParamInventory inv = new TemplateCustomParamInventory();
        inv.setTemplateUuid(vo.getTemplateUuid());
        inv.setParam(vo.getParam());
        return inv;
    }

    public static List<TemplateCustomParamInventory> valueOf(Collection<TemplateCustomParamVO> vos) {
        return vos.stream().map(TemplateCustomParamInventory::valueOf).collect(Collectors.toList());
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }
}
