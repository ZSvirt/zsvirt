
## Name

```
ping.maxFailure(镜像服务器失联前ping请求失败次数)
```

### Description

```
The maximum count of ping failure before reconnecting backup storage
```

### 含义

```
判定镜像服务器失联前ping请求失败次数。设置过大会导致判定镜像服务器失联周期变长，设置过小会导致网络抖动时误判镜像服务器连接状态
```

### Type

```
java.lang.Integer
```

### Category

```
backupStorage
```

### 取值范围

```
[0, 2147483647]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
3
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
