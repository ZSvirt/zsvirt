package org.zstack.zwatch.api



doc {

	title "事件元数据结构"

	field {
		name "namespace"
		desc "名字空间"
		type "String"
		since "2.3"
	}
	field {
		name "description"
		desc "描述"
		type "String"
		since "3.9.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.3"
	}
	field {
		name "labelNames"
		desc "标签名"
		type "List"
		since "2.3"
	}
}
