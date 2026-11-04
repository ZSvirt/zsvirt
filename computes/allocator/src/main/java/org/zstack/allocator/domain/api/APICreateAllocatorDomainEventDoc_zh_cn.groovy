package org.zstack.allocator.domain.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.allocator.domain.entity.AllocatorDomainInventory

doc {

	title "创建调度域的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "1.0.1, zsv_5.2.0"
	}
	ref {
		name "error"
		path "org.zstack.allocator.domain.api.APICreateAllocatorDomainEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "1.0.1, zsv_5.2.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.allocator.domain.api.APICreateAllocatorDomainEvent.inventory"
		desc "调度域清单"
		type "AllocatorDomainInventory"
		since "1.0.1, zsv_5.2.0"
		clz AllocatorDomainInventory.class
	}
}
