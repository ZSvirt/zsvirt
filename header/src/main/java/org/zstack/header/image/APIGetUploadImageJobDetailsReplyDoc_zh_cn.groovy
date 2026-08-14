package org.zstack.header.image

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.image.APIGetUploadImageJobDetailsReply.JobDetails
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取上传镜像任务详情结果"

	ref {
		name "error"
		path "org.zstack.header.image.APIGetUploadImageJobDetailsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.11.0"
		clz ErrorCode.class
	}
	ref {
		name "existingJobDetails"
		path "org.zstack.header.image.APIGetUploadImageJobDetailsReply.existingJobDetails"
		desc "已存在的未完成任务详情"
		type "List"
		since "zsv 3.11.0"
		clz JobDetails.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.11.0"
	}
}
