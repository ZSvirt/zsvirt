
## Name

```
blob.upload.concurrency(分片上传并发度)
```

### Description

```
concurrency when upload blobs of single image to imagestore
```

### 含义

```
上传单个镜像到镜像仓库时，其分片的上传并发度
```

### Type

```
java.lang.Integer
```

### Category

```
imagestore
```

### 取值范围

```
[1, 16]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
4
```

### 默认值补充说明

```
以磁带库为存储介质时，不推荐并发，即并发度为1。
```

### 支持的资源级配置

||
|---|
|org.zstack.header.storage.backup.BackupStorageVO

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
是
```

### CLI手册暴露

```
是
```

## 注意事项

```
并发度应根据网络带宽，存储介质和CPU核数综合判定，并不是越高越好。过高的并发度虽然可以获得更高的网络传输速度，但在一些存储介质上反而导致更低的落盘速度。
```
