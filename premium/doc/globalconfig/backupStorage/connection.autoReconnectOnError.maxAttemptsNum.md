
## Name

```
connection.autoReconnectOnError.maxAttemptsNum(本地备份服务器连续重连失败阈值)
```

### Description

```
the number of continuous connect failures that do not auto reconnect backup storage again. 0 means never stop.
```

### 含义

```
默认为0，表示管理节点会一直自动重连本地备份服务器。该参数用于设置管理节点尝试自动重连本地备份服务器过程中，允许连续重连失败的最大次数。超过该阈值，管理节点会停止自动重连本地备份服务器。
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
0
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
