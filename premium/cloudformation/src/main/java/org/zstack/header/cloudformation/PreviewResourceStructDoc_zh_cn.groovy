package org.zstack.header.cloudformation

import org.zstack.cloudformation.template.struct.ActionStruct

doc {

	title "资源编排模板清单列表"

	ref {
		name "actions"
		path "org.zstack.header.cloudformation.PreviewResourceStruct.actions"
		desc "资源编排模板清单列表"
		type "List"
		since "2.5.0"
		clz ActionStruct.class
	}

	field {
		name "conditions"
		desc "条件的执行结果"
		type "Map"
		since "3.7.0"
	}
}
