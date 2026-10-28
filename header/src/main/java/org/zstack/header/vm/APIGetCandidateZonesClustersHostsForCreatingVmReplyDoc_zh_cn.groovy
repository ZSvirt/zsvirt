package org.zstack.header.vm

import org.zstack.header.cluster.ClusterInventory
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostInventory
import org.zstack.header.zone.ZoneInventory

doc {

	title "可创建虚拟机目的地结果"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv_0.6"
		clz ErrorCode.class
	}
	ref {
		name "zones"
		path "org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmReply.zones"
		desc "可创建该虚拟机的区域清单"
		type "List"
		since "zsv_0.6"
		clz ZoneInventory.class
	}
	ref {
		name "clusters"
		path "org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmReply.clusters"
		desc "可创建虚拟机的集群清单"
		type "List"
		since "zsv_0.6"
		clz ClusterInventory.class
	}
	ref {
		name "hosts"
		path "org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmReply.hosts"
		desc "创建虚拟机物理机清单"
		type "List"
		since "zsv_0.6"
		clz HostInventory.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "zsv_0.6"
	}
}
