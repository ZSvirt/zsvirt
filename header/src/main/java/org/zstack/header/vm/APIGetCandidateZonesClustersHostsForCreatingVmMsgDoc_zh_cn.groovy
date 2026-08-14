package org.zstack.header.vm

import org.zstack.header.vm.APIGetCandidateZonesClustersHostsForCreatingVmReply

doc {
	title "获取可创建云主机的目的地列表(GetCandidateZonesClustersHostsForCreatingVm)"

	category "vmInstance"

	desc """获取可以创建指定云主机参数的目的区域、集群、物理机。用户可以使用该API，通过指定云主机参数获得可以创建满足参数云主机的目的地。"""

	rest {
		request {
			url "GET /v1/vm-instances/candidate-destinations"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateZonesClustersHostsForCreatingVmMsg.class

			desc """"""

			params {

				column {
					name "instanceOfferingUuid"
					enclosedIn ""
					desc "计算规格UUID"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "imageUuid"
					enclosedIn ""
					desc "镜像UUID"
					location "query"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn ""
					desc "三层网络列表"
					location "query"
					type "List"
					optional false
					since "zsv 0.6"
				}
				column {
					name "rootDiskOfferingUuid"
					enclosedIn ""
					desc "根云盘规格。仅在`imageUuid`指定的镜像是ISO时需要指定"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "dataDiskOfferingUuids"
					enclosedIn ""
					desc "云盘规格列表"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "zoneUuid"
					enclosedIn ""
					desc "区域UUID"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "defaultL3NetworkUuid"
					enclosedIn ""
					desc "默认三层网络UUID"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "cpuNum"
					enclosedIn ""
					desc "CPU数目"
					location "query"
					type "Integer"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "memorySize"
					enclosedIn ""
					desc "内存大小, 单位Byte"
					location "query"
					type "Long"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "rootDiskSize"
					enclosedIn ""
					desc "根云盘规格大小。仅在`imageUuid`指定的镜像是ISO时且`rootDiskOfferingUuid`为空：需要指定"
					location "query"
					type "Long"
					optional true
					since "zsv 3.11.2"
				}
			}
		}

		response {
			clz APIGetCandidateZonesClustersHostsForCreatingVmReply.class
		}
	}
}