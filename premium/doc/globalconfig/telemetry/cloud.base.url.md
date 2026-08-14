
## Name

```
cloud.base.url(Telemetry Cloud 根 URL)
```

### Description

```
Telemetry Cloud base URL (domain, not IP); must be https:// or None
```

### 含义

```
Telemetry Cloud 服务根 URL（域名，非 IP）。
管理节点上传 Daily Report 及健康检查时使用的 HTTPS 端点基址，由运维配置。
```

### Type

```
java.lang.String
```

### Category

```
telemetry
```

### 取值范围

```
[-9223372036854775808, 9223372036854775807]
```

### 取值范围补充说明

```
应为 HTTPS 域名根 URL，例如 https://telemetry.example.com；不要使用 IP 地址。
```

### DefaultValue

```
None
```

### 默认值补充说明

```
默认为空，须由运维在部署 Telemetry Cloud 后配置。
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
Telemetry V1 上传前提：telemetry.consent.granted.at 已授权且 Cloud HTTPS 健康检查成功。
```

### UI暴露

```
否
```

### CLI手册暴露

```
是
```

## 注意事项

```
未配置或 Cloud 不可达时，本地采集可继续但上传任务会保留 pending 队列并重试，不影响虚拟化业务。
```
