package org.zstack.drs.entity

import org.zstack.drs.api.Threshold
import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "集群DRS"

	field {
		name "clusterUuid"
		desc "集群UUID"
		type "String"
		since "4.0.0"
	}
	field {
		name "state"
		desc "状态"
		type "String"
		since "4.0.0"
	}
	field {
		name "balancedState"
		desc "集群平衡状态"
		type "String"
		since "4.0.0"
	}
	field {
		name "lastAdviceGroupUuid"
		desc "上一次动态调度的组UUID"
		type "String"
		since "4.10.0"
	}
	field {
		name "automationLevel"
		desc "集群动态调度触发等级"
		type "String"
		since "4.0.0"
	}
	ref {
		name "thresholds"
		path "org.zstack.drs.entity.ClusterDRSInventory.thresholds"
		desc "动态调度阈值"
		type "List"
		since "4.0.0"
		clz Threshold.class
	}
	field {
		name "thresholdDuration"
		desc "动态调度阈值判定时长"
		type "Integer"
		since "4.0.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.0.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.0.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.0.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
}
