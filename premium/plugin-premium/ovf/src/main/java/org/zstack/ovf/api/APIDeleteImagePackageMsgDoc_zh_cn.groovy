package org.zstack.ovf.api

import org.zstack.ovf.api.APIDeleteImagePackageEvent

doc {
	title "DeleteImagePackage"

	category "ovf"

	desc """删除镜像包"""

	rest {
		request {
			url "DELETE /v1/image-packages/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteImagePackageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.14.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "3.14.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.14.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.14.6"
				}
			}
		}

		response {
			clz APIDeleteImagePackageEvent.class
		}
	}
}