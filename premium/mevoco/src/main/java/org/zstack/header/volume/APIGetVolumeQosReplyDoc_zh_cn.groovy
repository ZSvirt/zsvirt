package org.zstack.header.volume

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取硬盘限速结果"

	field {
		name "volumeUuid"
		desc "硬盘 UUID"
		type "String"
		since "3.1.0"
	}
	field {
		name "volumeBandwidth"
		desc "硬盘总带宽，-1表示未限制"
		type "long"
		since "3.1.0"
	}
	field {
		name "volumeBandwidthRead"
		desc "硬盘读取带宽，-1表示未限制"
		type "long"
		since "3.2.0"
	}
	field {
		name "volumeBandwidthWrite"
		desc "硬盘写入带宽，-1表示未限制"
		type "long"
		since "3.2.0"
	}
	field {
		name "iopsTotal"
		desc "硬盘总IOPS限制，-1表示未限制"
		type "long"
		since "3.14.0"
	}
	field {
		name "iopsRead"
		desc "硬盘读取IOPS限制，-1表示未限制"
		type "long"
		since "3.14.0"
	}
	field {
		name "iopsWrite"
		desc "硬盘写入IOPS限制，-1表示未限制"
		type "long"
		since "3.14.0"
	}
	field {
		name "volumeBandwidthUpthreshold"
		desc "云盘总带宽上限，-1表示无限制"
		type "long"
		since "3.2.0"
	}
	field {
		name "volumeBandwidthReadUpthreshold"
		desc "硬盘读取带宽上限，-1表示无上限限制"
		type "long"
		since "3.2.0"
	}
	field {
		name "volumeBandwidthWriteUpthreshold"
		desc "硬盘写入带宽上限，-1表示无上限限制"
		type "long"
		since "3.2.0"
	}
	field {
		name "iopsTotalUpthreshold"
		desc "硬盘总IOPS上限，-1表示无上限限制"
		type "long"
		since "3.14.0"
	}
	field {
		name "iopsReadUpthreshold"
		desc "硬盘读取IOPS上限，-1表示无上限限制"
		type "long"
		since "3.14.0"
	}
	field {
		name "iopsWriteUpthreshold"
		desc "硬盘写入IOPS上限，-1表示无上限限制"
		type "long"
		since "3.14.0"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.APIGetVolumeQosReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
}
