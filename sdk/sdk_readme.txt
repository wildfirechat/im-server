    SDK使用说明：
        1. 把common.jar和sdk.jar 放到lib目录下（如没有需要手动建)。
        2. 添加下属依赖，注意本地jar包的路径。
        3. 修改build plugin，把common和sdk打包进去。
        4. 使用方法请查看sdk.jar/cn.wildfirechat.sdk/Main.class(没有混淆，可以看到源码)

    <!--# 需要添加如下依赖-->
    <dependencies>
		<dependency>
			<groupId>com.google.code.gson</groupId>
			<artifactId>gson</artifactId>
			<version>2.8.9</version>
		</dependency>

		<dependency>
			<groupId>commons-httpclient</groupId>
			<artifactId>commons-httpclient</artifactId>
			<version>3.1</version>
		</dependency>

		<dependency>
			<groupId>org.apache.httpcomponents</groupId>
			<artifactId>httpclient</artifactId>
			<version>4.5.13</version>
		</dependency>

		<dependency>
			<groupId>commons-codec</groupId>
			<artifactId>commons-codec</artifactId>
			<version>1.11</version>
			<scope>compile</scope>
		</dependency>

		<dependency>
			<groupId>com.google.protobuf</groupId>
			<artifactId>protobuf-java</artifactId>
			<version>2.5.0</version>
		</dependency>

		<dependency>
			<groupId>cn.wildfirechat</groupId>
			<artifactId>sdk</artifactId>
			<version>0.20</version>
			<scope>system</scope>
			<systemPath>${project.basedir}/src/lib/sdk-0.20.jar</systemPath>
		</dependency>

		<dependency>
			<groupId>cn.wildfirechat</groupId>
			<artifactId>common</artifactId>
			<version>0.20</version>
			<scope>system</scope>
			<systemPath>${project.basedir}/src/lib/common-0.20.jar</systemPath>
		</dependency>
	</dependencies>

    <!--# 由于添加了本地jar包，需要打包时把sdk和common打进去，下面是springboot项目添加includeSystemScope部分，其它类型项目请百度。 -->
	<build>
    	<plugins>
    		<plugin>
    			<groupId>org.springframework.boot</groupId>
    			<artifactId>spring-boot-maven-plugin</artifactId>
    			<configuration>
    				<includeSystemScope>true</includeSystemScope>
    			</configuration>
    		</plugin>
    	</plugins>
    </build>
