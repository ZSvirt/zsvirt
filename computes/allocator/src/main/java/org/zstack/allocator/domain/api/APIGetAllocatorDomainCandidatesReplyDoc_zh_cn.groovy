package org.zstack.allocator.domain.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostInventory
import org.zstack.header.cluster.ClusterInventory
import org.zstack.header.storage.primary.PrimaryStorageInventory
import org.zstack.header.network.l3.L3NetworkInventory

doc {

	title "获取调度域候选资源的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "1.0.1, zsv_5.2.0"
	}
	ref {
		name "error"
		path "org.zstack.allocator.domain.api.APIGetAllocatorDomainCandidatesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "1.0.1, zsv_5.2.0"
		clz ErrorCode.class
	}
	ref {
		name "hosts"
		path "org.zstack.allocator.domain.api.APIGetAllocatorDomainCandidatesReply.hosts"
		desc "可加入域的主机列表"
		type "List"
		since "1.0.1, zsv_5.2.0"
		clz HostInventory.class
	}
	ref {
		name "clusters"
		path "org.zstack.allocator.domain.api.APIGetAllocatorDomainCandidatesReply.clusters"
		desc "可加入域的集群列表"
		type "List"
		since "1.0.1, zsv_5.2.0"
		clz ClusterInventory.class
	}
	ref {
		name "primaryStorages"
		path "org.zstack.allocator.domain.api.APIGetAllocatorDomainCandidatesReply.primaryStorages"
		desc "可加入域的主存储列表"
		type "List"
		since "1.0.1, zsv_5.2.0"
		clz PrimaryStorageInventory.class
	}
	ref {
		name "l3Networks"
		path "org.zstack.allocator.domain.api.APIGetAllocatorDomainCandidatesReply.l3Networks"
		desc "可加入域的三层网络列表，含 PortGroup"
		type "List"
		since "1.0.1, zsv_5.2.0"
		clz L3NetworkInventory.class
	}
}
