package org.zstack.zwatch.metricpusher

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "receiverUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "template"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "namespace"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "metricName"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "labelsJsonStr"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
}
