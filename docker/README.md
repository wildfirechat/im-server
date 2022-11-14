# 野火IM服务docker使用说明

## 编译镜像
首先需要先编译应用服务，在项目根目录下使用下面命令编译
```
mvn clean package
```
生成的软件包在```distribution/target/distribution-XX-bundle-tar.tar.gz```，把此软件包拷贝到docker目录下。

然后进入到docker目录下，编译镜像
```
sudo docker build -t im-server .
```

## 运行
配置：
运行需要手动指定配置目录，手动指定配置目录的方法如下，其中$PATH_TO_CONFIG为im配置目录，需要为绝对路径，模版为```distribution/src/main/resources```目录。另外需要创建h2数据库目录（如果用mysql就不需要了）、日志目录、内置存储服务目录（如果使用其它对象存储服务就不需要了）。
```
sudo docker run -it --name im-server -v $PATH_TO_CONFIG:/opt/im-server/config -v $PATH_TO_LOGS:/opt/im-server/logs -v $PATH_TO_H2DB:/opt/im-server/h2db -v $PATH_TO_MEDIA:/opt/im-server/media -e JVM_XMX=256M -e JVM_XMS=256M -p 80:80 -p 1883:1883 -p 8083:8083 -p 8084:8084 -p 18080:18080 im-server
```

## 导出镜像
```
sudo docker save -o im-server.tar im-server
```

## 导入镜像
```
sudo docker load -i im-server.tar
```
