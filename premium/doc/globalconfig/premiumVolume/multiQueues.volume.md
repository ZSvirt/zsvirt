
## Name

```
multiQueues.volume(数据云盘多队列)
```

### Description

```
the multiQueues of data volume.
```

### 含义

```
为数据云盘设置多队列数量
```

### Type

```
java.lang.Integer
```

### Category

```
premiumVolume
```

### 取值范围

```
[0 ,128]
```

### 取值范围补充说明

```
0为默认值,表示未设置多队列数量;其余数字表示云盘多队列数量，超出vCPU数的部分不生效
```

### DefaultValue

```
0
```

### 默认值补充说明

```
0为默认值,表示未设置多队列数量
```

### 支持的资源级配置

||
|---|
|org.zstack.header.volume.VolumeVO
|org.zstack.header.cluster.ClusterVO

### 资源粒度说明

```
云盘
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
不支持virtio-scsi,同时设置virtio-scsi和多队列时,云盘多队列设置不生效
```
