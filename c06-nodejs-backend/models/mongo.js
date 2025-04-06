const {MongoClient} = require("mongodb");
const config = require("../config/db.config");

const mongoClient = new MongoClient(config.mongo.url);

async function getSnmpDataSince(minutes) {
    let snmpData = [];
    try {
        await mongoClient.connect();
        const db = mongoClient.db(config.mongo.database);
        const snmpCollection = db.collection("snmp");

        const now = new Date();
        const threshold = new Date(now.getTime() - minutes * 60 * 1000);

        console.log(`Fetching SNMP data since ${threshold.toISOString()}`);

        snmpData = await snmpCollection.find({timestamp: {$gte: threshold}}).toArray();
    } catch (error) {
        console.error("MongoDB connection error:", error);
        throw error;
    } finally {
        await mongoClient.close();
    }
    return snmpData;
}

async function insertSnmpData(snmpDocument) {
    try {
        await mongoClient.connect();
        const db = mongoClient.db(config.mongo.database);
        const snmpCollection = db.collection("snmp");

        const insertResult = await snmpCollection.insertOne(snmpDocument);
        return insertResult.insertedId;
    } catch (error) {
        console.error("MongoDB insert error:", error);
        throw error;
    } finally {
        await mongoClient.close();
    }
}

async function clearSnmpData() {
    try {
        await mongoClient.connect();
        const db = mongoClient.db(config.mongo.database);
        const snmpCollection = db.collection("snmp");

        const deleteResult = await snmpCollection.deleteMany({});
        return deleteResult.deletedCount;
    } catch (error) {
        console.error("MongoDB delete error:", error);
        throw error;
    } finally {
        await mongoClient.close();
    }
}

module.exports = {
    getSnmpDataSince,
    insertSnmpData,
    clearSnmpData
};
