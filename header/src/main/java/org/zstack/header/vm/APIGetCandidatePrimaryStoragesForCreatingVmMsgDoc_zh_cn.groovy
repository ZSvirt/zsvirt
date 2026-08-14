package org.zstack.header.vm

import org.zstack.header.vm.APIGetCandidatePrimaryStoragesForCreatingVmReply

doc {
	title "获取创建云主机时可选择的主存储(GetCandidatePrimaryStoragesForCreatingVm)"

	category "vmInstance"

	desc """用户可通过指定云主机参数，来获取当前参数下可选择的主存储"""

	rest {
		request {
			url "GET /v1/vm-instances/candidate-storages"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidatePrimaryStoragesForCreatingVmMsg.class

			desc """"""

			params {

				column {
					name "imageUuid"
					enclosedIn ""
					desc "镜像UUID"
					location "query"
					type "String"
					optional false
					since "zsv 2.1"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn ""
					desc "三层网络UUID"
					location "query"
					type "List"
					optional false
					since "zsv 2.1"
				}
				column {
					name "rootDiskOfferingUuid"
					enclosedIn ""
					desc "根云盘使用的云盘规格UUID，镜像类型为ISO时可选且必选"
					location "query"
					type "String"
					optional true
					since "zsv 2.1"
				}
				column {
					name "rootDiskSize"
					enclosedIn ""
					desc "自定义根云盘大小。仅在`imageUuid`指定的镜像是ISO时且`rootDiskOfferingUuid`为空需要指定"
					location "query"
					type "Long"
					optional true
					since "zsv 3.11.2"
				}
				column {
					name "dataDiskOfferingUuids"
					enclosedIn ""
					desc "数据云盘使用的云盘规格UUID"
					location "query"
					type "List"
					optional true
					since "zsv 2.1"
				}
				column {
					name "dataDiskSizes"
					enclosedIn ""
					desc "自定义数据云盘大小"
					location "query"
					type "List"
					optional true
					since "zsv 3.13.9"
				}
				column {
					name "zoneUuid"
					enclosedIn ""
					desc "区域UUID"
					location "query"
					type "String"
					optional true
					since "zsv 2.1"
				}
				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "query"
					type "String"
					optional true
					since "zsv 2.1"
				}
				column {
					name "defaultL3NetworkUuid"
					enclosedIn ""
					desc "默认三层网络UUID"
					location "query"
					type "String"
					optional true
					since "zsv 2.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 2.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 2.1"
				}
			}
		}

		response {
			clz APIGetCandidatePrimaryStoragesForCreatingVmReply.class
		}
	}
}