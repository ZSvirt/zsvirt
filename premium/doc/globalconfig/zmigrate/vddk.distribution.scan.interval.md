
## Name

```
vddk.distribution.scan.interval(ZMigrate VDDK分发扫描间隔，单位为秒)
```

### Description

```
Interval for scanning ZMigrate Linux boot VMs to distribute VDDK, in seconds
```

### 含义

```
ZMigrate VDDK分发扫描间隔，单位为秒
```

### Type

```
java.lang.Long
```

### Category

```
zmigrate
```

### 取值范围

```
[1, 9223372036854775807]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
60
```

### 默认值补充说明

```
无
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
无
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
修改后会停止当前扫描任务，并按新间隔重新创建定时任务。
```
