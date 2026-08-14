
## Name

```
upload.max.idle.duration.in.seconds(上传软件包到主机最大等待时长)
```

### Description

```
the max duration can be tolerated when uploading software package, in seconds
```

### 含义

```
上传软件包到主机时最大的等待时间，单位：秒
当上传软件包任务开启后，如果太长时间没有上传数据，该任务会暂停或失败
```

### Type

```
java.lang.Long
```

### Category

```
softwarePackage
```

### 取值范围

```
[0, 9223372036854775807]
```

### 取值范围补充说明

```
正整数
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
无
```
