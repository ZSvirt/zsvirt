package org.zstack.header.cloudformation

import org.zstack.cloudformation.template.struct.ActionStruct

doc {

	title "资源编排模板清单列表"

	ref {
		name "actions"
		path "org.zstack.header.cloudformation.PreviewResourceStruct.actions"
		desc "资源编排模板清单列表"
		type "List"
		since "zsv_2.5.0"
		clz ActionStruct.class
	}

	field {
		name "conditions"
		desc "条件的执行结果"
		type "Map"
		since "zsv_3.7.0"
	}
}
