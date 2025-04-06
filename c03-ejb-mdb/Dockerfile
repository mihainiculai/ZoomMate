FROM critoma/amd64_u24_noble_ism_security:latest

ENV JAVA_HOME=/opt/software/java/jdks/jdk-21.0.2
ENV PATH=$JAVA_HOME/bin:$PATH

WORKDIR /app

RUN apt-get update && apt-get install -y maven snmp snmpd

COPY . /app
COPY snmpd.conf /etc/snmp/

RUN mvn clean install

COPY src/main/resources/META-INF/tomee.xml /opt/software/apache-tomee-plume-10.0.0-M3/conf/tomee.xml

RUN mkdir -p /opt/software/apache-tomee-plume-10.0.0-M3/apps/
RUN cp target/c03-ejb-mdb-1.0-SNAPSHOT.jar /opt/software/apache-tomee-plume-10.0.0-M3/apps/

EXPOSE 161/udp

CMD service snmpd start && /opt/software/apache-tomee-plume-10.0.0-M3/bin/catalina.sh run
