package org.zstack.pciDevice.virtual.vfio_mdev

import org.zstack.pciDevice.virtual.vfio_mdev.APIGetMdevDeviceCandidatesReply

doc {
	title "GetMdevDeviceCandidates"

	category "pciDevice"

	desc """获取可用的MDEV设备"""

	rest {
		request {
			url "GET /v1/mdev-devices/candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetMdevDeviceCandidatesMsg.class

			desc """获取可用的MDEV设备"""

			params {

				column {
					name "clusterUuids"
					enclosedIn ""
					desc "集群UUID"
					location "query"
					type "List"
					optional true
					since "3.5.0"
				}
				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "query"
					type "String"
					optional true
					since "3.5.0"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "query"
					type "String"
					optional true
					since "3.5.0"
				}
				column {
					name "types"
					enclosedIn ""
					desc "设备类型"
					location "query"
					type "List"
					optional true
					since "3.5.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.5.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.5.0"
				}
			}
		}

		response {
			clz APIGetMdevDeviceCandidatesReply.class
		}
	}
}