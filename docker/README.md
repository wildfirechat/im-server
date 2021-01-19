# 野火IM服务docker使用说明

## 编译镜像
首先需要先编译应用服务，在项目根目录下使用下面命令编译
```
mvn clean package
```

然后进入到docker目录下，编译镜像
```
sudo docker build -t wildfire_im .
```

## 运行
配置：
运行需要手动指定配置目录，这样不用重新打包镜像。手动指定配置目录的方法如下，其中$PATH_TO_CONFIG为im配置目录，模版为```distribution/src/main/resources```目录:
```
sudo docker run -it -v $PATH_TO_CONFIG:/opt/im-server/config -v $PATH_TO_LOGS:/opt/im-server/logs -e JVM_XMX=256M -e JVM_XMS=256M -p 80:80 -p 1883:1883 -p 8083:8083 -p 8084:8084 -p 18080:18080 wildfire_im
```
