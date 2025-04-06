FROM critoma/amd64_u24_noble_ism_security:latest

ENV JAVA_HOME=/opt/software/java/jdks/jdk-21.0.2
ENV PATH=$JAVA_HOME/bin:$PATH

WORKDIR /app

RUN apt-get update && apt-get install -y maven snmp snmpd

COPY . /app
COPY snmpd.conf /etc/snmp/

RUN mvn clean install

EXPOSE 161/udp

CMD service snmpd start && java -jar target/c02-jms-broker-1.0-SNAPSHOT.jar
