
## Name

```
collect.interval.seconds(Telemetry 采集周期)
```

### Description

```
Collection interval in seconds
```

### 含义

```
Telemetry 本地数据采集调度周期，单位秒。
用户授权开启 Telemetry 后，按该间隔执行采集任务并生成 Daily Report。
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
Telemetry V1 采集前提：telemetry.consent.granted.at 不为 None；未授权时采集器不得被调度执行。
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
仅影响调度频率，不改变单次采集的白名单字段范围；关闭授权后采集任务停止。
```
