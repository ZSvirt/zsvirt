
## Name

```
tmp.image.cache.folder(块存储下载镜像的临时目录路径)
```

### Description

```
tmp image cache folder for downloading
```

### 含义

```
Block 主存储在下载有链的镜像时，不能直接把镜像下载到块设备里面，需要先创建一个临时块设备，
并挂载到指定目录，将所有相关镜像下载到这个目录，然后convert 到块设备中。这个配置就是用来设
置指定目录的。
```

### Type

```
java.lang.String
```

### Category

```
blockPrimaryStorage
```

### 取值范围

```

```

### 取值范围补充说明

```
无
```

### DefaultValue

```
/tmp/.imagecache/tmp/
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
否
```

## 注意事项

```
无
```
