
## Name

```
memory.auto.balloon.scan.interval(内存回收扫描间隔)
```

### Description

```
memory auto balloon scan interval
```

### 含义

```
设置间隔后，内存回收扫描将在该间隔内进行一次，单位为秒
```

### Type

```
java.lang.Integer
```

### Category

```
vmMemory
```

### 取值范围

```
[60 ,600]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
300
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
需要云主机启用内存回收功能，并且支持通过virtio balloon机制进行内存回收
```

### UI暴露

```
否
```

### CLI手册暴露

```
否
```

## 注意事项

```
无
```
