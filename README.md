
## What is Moquette?

[![Build Status](https://api.travis-ci.org/andsel/moquette.svg?branch=master)](https://travis-ci.org/andsel/moquette)

* [Documentation reference guide] (http://andsel.github.io/moquette/) Guide on how to use and configure Moquette
* [Google Group] (https://groups.google.com/forum/#!forum/moquette-mqtt) Google Group to participate in development discussions.
Moquette aims to be a MQTT compliant broker. The broker supports QoS 0, QoS 1 and QoS 2.

Its designed to be evented, uses Netty for the protocol encoding and decoding part.
 
## Embeddable

[Freedomotic] (http://www.freedomotic.com/) Is an home automation framework, uses Moquette embedded to interface with MQTT world.
Moquette is also used into [Atomize Spin] (http://atomizesoftware.com/spin) a software solution for the logistic field.
Part of moquette are used into the [Vertx MQTT module] (https://github.com/giovibal/vertx-mqtt-broker-mod), into [MQTT spy](http://kamilfb.github.io/mqtt-spy/)
and into [WSO2 Messge broker] (http://techexplosives-pamod.blogspot.it/2014/05/mqtt-transport-architecture-wso2-mb-3x.html).

## 1 minute set up
Start play with it, download the self distribution tar from [BinTray](https://bintray.com/artifact/download/andsel/generic/distribution-0.8-bundle-tar.tar.gz) ,
the un untar and start the broker listening on 1883 port and enjoy! 
```
tar zxf distribution-0.8-bundle-tar.tar.gz
cd bin
./moquette.sh
```

Or if you are on Windows shell
```
 cd bin
 .\moquette.bat
 ```

## Embedding in other projects
To embed Moquette in another maven project is sufficient to include a repository and declare the dependency: 

```
<repositories>
  <repository>
    <id>bintray</id>
    <url>http://dl.bintray.com/andsel/maven/</url>
    <releases>
      <enabled>true</enabled>
    </releases>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
  </repository>
</repositories>
```

Include dependency in your project: 

```
<dependency>
      <groupId>io.moquette</groupId>
      <artifactId>moquette-broker</artifactId>
      <version>0.8</version>
</dependency>
```

## Build from sources

After a git clone of the repository, cd into the cloned sources and: `mvn clean package`. 
In distribution/target directory will be produced the selfcontained tar for the broker with all dependencies and a running script. 
  
