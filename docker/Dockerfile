FROM openjdk:8-jre-alpine

ADD ../distribution/target/distribution-*-bundle-tar.tar.gz /opt/im-server/

RUN sed -i 's/#JAVA_OPTS="$JAVA_OPTS -Xmx128M"/JAVA_OPTS="$JAVA_OPTS -Xmx$JVM_XMX"/g' /opt/im-server/bin/wildfirechat.sh
RUN sed -i 's/#JAVA_OPTS="$JAVA_OPTS -Xms128M"/JAVA_OPTS="$JAVA_OPTS -Xms$JVM_XMS"/g' /opt/im-server/bin/wildfirechat.sh

WORKDIR /opt/im-server

VOLUME /opt/im-server/config
VOLUME /opt/im-server/logs

EXPOSE 80/tcp 1883/tcp 8083/tcp 8084/tcp 18080/tcp

ENV JVM_XMX 256M
ENV JVM_XMS 256M


CMD ./bin/wildfirechat.sh
