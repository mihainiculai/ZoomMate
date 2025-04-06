FROM critoma/amd64_u24_noble_ism_security:latest

ENV NODE_HOME=/opt/software/node-v22.10.0-linux-x64
ENV PATH=$NODE_HOME/bin:$PATH

WORKDIR /app

RUN apt-get update && apt-get install -y snmp snmpd

COPY . /app
COPY snmpd.conf /etc/snmp/

RUN npm install

EXPOSE 3001 161/udp

CMD service snmpd start && npm start
