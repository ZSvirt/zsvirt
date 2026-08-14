package org.zstack.header.image

import org.zstack.header.image.APICalculateImageHashEvent

doc {
	title "CalculateImageHash"

	category "image"

	desc """计算镜像的MD5值"""

	rest {
		request {
			url "PUT /v1/images/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICalculateImageHashMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "calculateImageHash"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.1.0"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "calculateImageHash"
					desc "镜像存储UUID"
					location "body"
					type "String"
					optional false
					since "zsv 4.1.0"
				}
				column {
					name "algorithm"
					enclosedIn "calculateImageHash"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 4.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.1.0"
				}
			}
		}

		response {
			clz APICalculateImageHashEvent.class
		}
	}
}