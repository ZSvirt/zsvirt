
## Name

```
auto.sync.interval.seconds(第三方用户源自动同步时间间隔)
```

### Description

```
time interval in seconds for automatic synchronization of account from remote server
```

### 含义

```
第三方用户源自动同步时间间隔，单位秒；
必须在 auto.sync.enable 配置设置为 true 时，该参数才有效。
```

### Type

```
java.lang.Long
```

### Category

```
accountImport
```

### 取值范围

```
[60, 9223372036854775807]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
86400
```

### 默认值补充说明

```
一天 86400 秒
```

### 支持的资源级配置

||
|---|
|org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO

### 资源粒度说明

```
ThirdPartyUserSourceVO 为第三方用户源
```

### 背景信息

```
无
```

### UI暴露

```
是
```

### CLI手册暴露

```
是
```

## 注意事项

```
无
```
