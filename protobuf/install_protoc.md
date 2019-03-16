## Run follow command by sequence:
To build protobuf from source, the following tools are needed:


On Ubuntu, you can install them with:
$ sudo apt-get install autoconf automake libtool curl make g++ unzip

On Mac, you can install them with:
$ brew install autoconf automake libtool


Then run the cmd.
$ ./autogen.sh
$ ./configure
$ make
$ make check
$ sudo make install
$ sudo ldconfig # refresh shared library cache.



## Javascript 生成方法
1. 安装nodejs
2. 安装protobufjs
```
sudo npm install protobufjs@5 -g
```
3. 生成JS
```
pbjs -t static-module -w commonjs -o protocol.js wfcmessage.proto
```

> [参考1](http://dcode.io/protobuf.js/#pbjs-for-javascript)

> [参考2](https://blog.csdn.net/yyf1990cs/article/details/78942157)
