## What is Moquette?

Moquette aims to be a MQTT compliant broker. The broker supports QoS 0, QoS 1 and QoS 2.

Its designed to be evented, uses Netty for the protocol encoding and decoding part, the protocol logic 
is essentially a single threaded and it's isolated from front connectors part by LMAX disruptor's ring buffer.
 
## Who use it?

[Freedomotic] (http://www.freedomotic.com/) Is an home automation framework, uses Moquette embedded to interface with MQTT world.
Part of moquette are also used into the [Vertx MQTT module] (https://github.com/giovibal/vertx-mqtt-broker-mod), into [MQTT spy](https://code.google.com/p/mqtt-spy/) 
and into [WSO2 Messge broker] (http://techexplosives-pamod.blogspot.it/2014/05/mqtt-transport-architecture-wso2-mb-3x.html).

## 1 minute set up
Start play with it, download the self distribution tar from [BinTray](http://dl.bintray.com/andsel/generic/distribution-0.6-bundle-tar.tar.gz) ,
the un untar and start the broker listening on 1883 port and enjoy! 
```
tar zxf distribution-0.6-bundle-tar.tar.gz
cd bin
./moquette.sh
```

### Running inside OSGi container

Starting from version 0.6 Moquette is OSGi compliant, to see it in action: 
```
mvn clean install;
cd budle;
mvn install pax:provision
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
      <groupId>org.dna.mqtt</groupId>
      <artifactId>moquette-broker</artifactId>
      <version>0.6</version>
</dependency>
```

## Build from sources

After a git clone of the repository, cd into the cloned sources and: `mvn clean package`. 
In distribution/target directory will be produced the selfcontained tar for the broker with all dependencies and a running script. 


## SSL configuration
Here are some simple steps to do to configure Moquette to serve over SSL 
 **Details **
Moquette uses JavaKeyStore? and certificates to handle SSL. In order to expose it over SSL you have create a keystore for 
the broker (select the password), exporting a certificate and define 4 variables into moquette.conf.
 
 **Create a keystore**
 In a directory generate the keystore using the JRE's keytool: 
 ```
 keytool -keystore serverkeystore.jks -alias testserver -genkey -keyalg RSA
 ```
 
 To make it work you have to answer at the first question, say moquette.dna.org and as password we could use passw0rdsrv 
 for both (keystore and keymanger)
  
 **Export a certificate**
  Then you need export a certificate: 
  ```
  keytool -export -alias testserver -keystore serverkeystore.jks -file testserver.crt
  ```
  
  **Imporing on the client side**
  Supposing you have already created the keystore for the client side, (name it clientkeystore for example), we could import the certificate with: 
  ```
  keytool -keystore clientkeystore.jks -import -alias testserver -file testserver.crt -trustcacerts
  ```
  
  It's done! We just need use the Paho client to connect to the server, check ServerIntegrationSSLTest.java integration test to see how. 
  