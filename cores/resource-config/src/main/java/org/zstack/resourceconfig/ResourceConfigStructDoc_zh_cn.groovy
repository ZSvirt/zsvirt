package org.zstack.resourceconfig

import org.zstack.resourceconfig.ResourceConfigInventory

doc {

	title "资源级配置的数据结构"

	field {
		name "value"
		desc "配置值"
		type "String"
		since "3.17.0"
	}
	ref {
		name "effectiveConfigs"
		path "org.zstack.resourceconfig.ResourceConfigStruct.effectiveConfigs"
		desc "已生效的配置清单列表"
		type "List"
		since "3.17.0"
		clz ResourceConfigInventory.class
	}
	field {
		name "name"
		desc "配置名称"
		type "String"
		since "3.17.0"
	}
}
