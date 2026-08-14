package org.zstack.header.vm

import org.zstack.header.cluster.ClusterInventory
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostInventory

doc {

	title "云主机可启动目的地结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmStartingCandidateClustersHostsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "hosts"
		path "org.zstack.header.vm.APIGetVmStartingCandidateClustersHostsReply.hostInventories"
		desc "云主机可启动的物理机清单"
		type "List"
		since "zsv 0.6"
		clz HostInventory.class
	}
	ref {
		name "clusters"
		path "org.zstack.header.vm.APIGetVmStartingCandidateClustersHostsReply.clusterInventories"
		desc "云主机可启动的集群清单"
		type "List"
		since "zsv 0.6"
		clz ClusterInventory.class
	}
}
