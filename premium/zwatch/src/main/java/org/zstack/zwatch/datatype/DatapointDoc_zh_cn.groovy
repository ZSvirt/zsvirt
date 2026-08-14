package org.zstack.zwatch.datatype



doc {

	title "监控数据结构"

	field {
		name "value"
		desc "监控值"
		type "double"
		since "0.6"
	}
	field {
		name "time"
		desc "记录生成时间"
		type "long"
		since "0.6"
	}
	field {
		name "labels"
		desc "标签"
		type "Map"
		since "0.6"
	}
}
