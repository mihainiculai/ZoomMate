const snmp = require("net-snmp");
const mongoModel = require("../models/mongo");

const OIDS = {
    sysName: "1.3.6.1.2.1.1.5.0",
    sysDescr: "1.3.6.1.2.1.1.1.0",
    cpuLoad1: "1.3.6.1.4.1.2021.10.1.3.1",
    memTotal: "1.3.6.1.4.1.2021.4.5.0",
    memFree: "1.3.6.1.4.1.2021.4.6.0"
};

const snmpTargets = [
    {name: "c01-java-backend", host: "c01-java-backend", port: 161},
    {name: "c02-jms-broker", host: "c02-jms-broker", port: 161},
    {name: "c03-ejb-mdb", host: "c03-ejb-mdb", port: 161},
    {name: "c04-rmi-server", host: "c04-rmi-server", port: 161},
    {name: "c05-rmi-server", host: "c05-rmi-server", port: 161},
    {name: "c06-nodejs-backend", host: "c06-nodejs-backend", port: 161}
];

const sessionOptions = {
    port: 161,
    version: snmp.Version2c,
    timeout: 1000,
    transport: "udp4"
};

async function pollSnmp() {
    for (const target of snmpTargets) {
        const targetOptions = {...sessionOptions, port: target.port};
        const session = snmp.createSession(target.host, "public", targetOptions);

        const targetOids = [
            OIDS.sysName,
            OIDS.sysDescr,
            OIDS.cpuLoad1,
            OIDS.memTotal,
            OIDS.memFree
        ];

        const snmpData = await new Promise((resolve, reject) => {
            session.get(targetOids, (error, varbinds) => {
                if (error) {
                    console.error(`[SNMP] Detailed error for ${target.name}:`, error);
                    return reject(error);
                }

                const retrievedData = {};
                let foundError = false;

                varbinds.forEach((varbind, index) => {
                    if (snmp.isVarbindError(varbind)) {
                        console.error(
                            `[SNMP] Varbind error for ${target.name} at ${targetOids[index]}:`,
                            snmp.varbindError(varbind)
                        );
                        foundError = true;
                        retrievedData[targetOids[index]] = snmp.varbindError(varbind);
                    } else {
                        retrievedData[targetOids[index]] = varbind.value.toString();
                    }
                });

                if (foundError) {
                    return reject(new Error("One or more OIDs returned an error."));
                }

                return resolve(retrievedData);
            });

            session.on("error", (sessionError) => {
                console.error(`[SNMP] Session error for ${target.name}:`, sessionError);
            });
        }).catch((pollError) => {
            console.error(`[SNMP] Error at ${target.name}:`, pollError.message);
            return null;
        });

        session.close();

        if (snmpData) {
            const snmpDocument = {
                container: target.name,
                timestamp: new Date(),
                sysName: snmpData[OIDS.sysName],
                sysDescr: snmpData[OIDS.sysDescr],
                cpuLoad1: snmpData[OIDS.cpuLoad1],
                memTotal: snmpData[OIDS.memTotal],
                memFree: snmpData[OIDS.memFree]
            };

            try {
                await mongoModel.insertSnmpData(snmpDocument);
            } catch (mongoError) {
                console.error("[SNMP] Error inserting SNMP data into MongoDB:", mongoError);
            }
        }
    }
}

function startSnmpPolling() {
    pollSnmp().catch((error) => console.error("[SNMP] Initial polling error:", error));

    setInterval(() => {
        pollSnmp().catch((error) => console.error("[SNMP] Polling error:", error));
    }, 1000);
}

module.exports = {
    startSnmpPolling
};
