package org.zstack.allocator.domain.entity

import java.sql.Timestamp

doc {

	title "调度域规则清单"

	field {
		name "id"
		desc "自增主键"
		type "Long"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "domainUuid"
		desc "所属调度域UUID"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "priority"
		desc "求值顺序，从 0 开始。Create/Update 时按 API rules 列表下标赋值"
		type "Integer"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "resourceUuid"
		desc "规则指向的资源UUID"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "resourceType"
		desc "资源类型：HostVO / ClusterVO / PrimaryStorageVO / L3NetworkVO"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "resourceQualifier"
		desc "与 resourceType 搭配。PrimaryStorageVO 时为 Root / Data / 空（全部云盘）"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "resourcePolicy"
		desc "预留字段，当前为 null"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "effect"
		desc "Include / Exclude / Recommend / NotRecommend"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "policyFrom"
		desc "规则来源类型，一般 null 表示 default"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "policyUuid"
		desc "规则来源UUID，一般 null"
		type "String"
		since "1.0.1, zsv 5.2.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "1.0.1, zsv 5.2.0"
	}
}
