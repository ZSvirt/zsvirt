package org.zstack.header.volume

import org.zstack.header.volume.APIGetVolumeQosReply

doc {
	title "GetVolumeQos"

	category "mevoco"

	desc """获取云盘限速"""

	rest {
		request {
			url "GET /v1/volumes/{uuid}/qos"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVolumeQosMsg.class

			desc """获取云盘限速"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云盘的UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "forceSync"
					enclosedIn ""
					desc "是否到物理机上去同步数据"
					location "query"
					type "Boolean"
					optional true
					since "3.3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetVolumeQosReply.class
		}
	}
}