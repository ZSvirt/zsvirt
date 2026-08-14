package org.zstack.header.volume

import org.zstack.header.volume.APIGetDataVolumeAttachableVmReply

doc {
	title "GetDataVolumeAttachableVm"

	category "volume"

	desc """获取云盘是否能被加载"""

	rest {
		request {
			url "GET /v1/volumes/{volumeUuid}/candidate-vm-instances"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetDataVolumeAttachableVmMsg.class

			desc """"""

			params {

				column {
					name "volumeUuid"
					enclosedIn ""
					desc "云盘UUID"
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
			clz APIGetDataVolumeAttachableVmReply.class
		}
	}
}