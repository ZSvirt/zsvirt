package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmAttachableDataVolumeReply

doc {
	title "获取云主机可加载云盘列表(GetVmAttachableDataVolume)"

	category "vmInstance"

	desc """获取一个云主机可以加载的云盘列表"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/data-volume-candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmAttachableDataVolumeMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
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
			}
		}

		response {
			clz APIGetVmAttachableDataVolumeReply.class
		}
	}
}