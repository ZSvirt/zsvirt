package org.zstack.ovf.api

import org.zstack.ovf.api.APIUpdateImagePackageEvent

doc {
	title "UpdateImagePackage"

	category "ovf"

	desc """更新镜像包信息"""

	rest {
		request {
			url "PUT /v1/image-packages/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateImagePackageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateImagePackage"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.14.6"
				}
				column {
					name "name"
					enclosedIn "updateImagePackage"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.14.6"
				}
				column {
					name "description"
					enclosedIn "updateImagePackage"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.14.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.14.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.14.6"
				}
			}
		}

		response {
			clz APIUpdateImagePackageEvent.class
		}
	}
}