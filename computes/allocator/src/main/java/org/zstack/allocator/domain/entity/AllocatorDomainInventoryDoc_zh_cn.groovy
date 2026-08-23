package org.zstack.allocator.domain.entity

import java.sql.Timestamp
import org.zstack.allocator.domain.entity.AllocatorDomainRuleInventory
import org.zstack.allocator.domain.entity.AllocatorDomainSceneInventory

doc {

	title "调度域清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "state"
		desc "Enabled / Disabled。Disabled 时已关联虚拟机本次不约束"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "1.0.1, zsv 5.2.0"
	}
	ref {
		name "rules"
		path "org.zstack.allocator.domain.entity.AllocatorDomainInventory.rules"
		desc "有序规则列表，按 priority 升序"
		type "List"
		since "1.0.1, zsv 5.2.0"
		clz AllocatorDomainRuleInventory.class
	}
	ref {
		name "scenes"
		path "org.zstack.allocator.domain.entity.AllocatorDomainInventory.scenes"
		desc "规则生效场景列表"
		type "List"
		since "1.0.1, zsv 5.2.0"
		clz AllocatorDomainSceneInventory.class
	}
}
