package org.zstack.zwatch.api

import org.zstack.zwatch.api.Histogram.Tag

doc {

	title "在这里输入结构的名称"

	field {
		name "time"
		desc ""
		type "long"
		since "0.6"
	}
	field {
		name "count"
		desc ""
		type "long"
		since "0.6"
	}
	ref {
		name "tags"
		path "org.zstack.zwatch.api.Histogram.tags"
		desc "null"
		type "List"
		since "0.6"
		clz Tag.class
	}
}
