package org.zstack.header.volume

import org.zstack.header.volume.APIChangeVolumeEncryptionEvent

doc {
	title "转换硬盘加密属性(ChangeVolumeEncryption)"

	category "volume"

	desc """转换硬盘加密属性"""

	rest {
		request {
			url "PUT /v1/volumes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeVolumeEncryptionMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeVolumeEncryption"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "encrypted"
					enclosedIn "changeVolumeEncryption"
					desc ""
					location "body"
					type "boolean"
					optional false
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
			}
		}

		response {
			clz APIChangeVolumeEncryptionEvent.class
		}
	}
}