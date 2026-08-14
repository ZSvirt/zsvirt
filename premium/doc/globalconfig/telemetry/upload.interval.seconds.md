
## Name

```
upload.interval.seconds(Telemetry 上传周期)
```

### Description

```
Upload interval in seconds
```

### 含义

```
Telemetry Daily Report 云端上传调度周期，单位秒。
用户授权且 Cloud 可达时，按该间隔尝试上传 pending 队列及当日最新日报。
```

### Type

```
java.lang.Long
```

### Category

```
telemetry
```

### 取值范围

```
[1, 9223372036854775807]
```

### 取值范围补充说明

```
须大于 0；V1 默认 86400 秒（24 小时）。
```

### DefaultValue

```
86400
```

### 默认值补充说明

```
一天 86400 秒。
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
Telemetry V1 上传与采集周期可同任务或拆分，均需尊重授权开关；失败时保留 pending 并在下一周期重试。
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
上传失败不影响虚拟化业务；Cloud 健康检查失败时不发起上传，本地 pending 保留待恢复后补传。
```
