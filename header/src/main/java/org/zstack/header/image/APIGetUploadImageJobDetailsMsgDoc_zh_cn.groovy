package org.zstack.header.image

import org.zstack.header.image.APIGetUploadImageJobDetailsReply

doc {
	title "GetUploadImageJobDetails"

	category "image"

	desc """获取上传镜像任务详情"""

	rest {
		request {
			url "GET /v1/images/upload-job/details/{imageId}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetUploadImageJobDetailsMsg.class

			desc """"""

			params {

				column {
					name "imageId"
					enclosedIn ""
					desc "上传镜像的唯一标识，由用户自定义，推荐使用 md5"
					location "url"
					type "String"
					optional false
					since "zsv 3.11.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
			}
		}

		response {
			clz APIGetUploadImageJobDetailsReply.class
		}
	}
}