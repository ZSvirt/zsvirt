
## Name

```
ping.sleepPeriodAfterFailure(ping失败时发送下次ping请求的时间间隔)
```

### Description

```
The sleep period before retrying the next backup storage ping after a ping failure
```

### 含义

```
每次PING请求失败后，发送下次PING请求前的时间间隔。设置过大会导致判定镜像服务器失联周期变长，设置过小会导致网络抖动时误判镜像服务器连接状态
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
[-1, 2147483647]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
1
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
