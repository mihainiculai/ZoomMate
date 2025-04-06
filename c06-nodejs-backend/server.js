require("dotenv").config();
const express = require("express");
const bodyParser = require("body-parser");
const cors = require("cors");
const imageRoutes = require("./routes/images");
const snmpRoutes = require("./routes/snmp");
const snmpService = require("./services/snmpService");
const {initDB} = require("./models/init_mysql");
const {clearSnmpData} = require("./models/mongo");

// Initialize MySQL tables
initDB()
    .then(() => {
        console.log("Checked and initialized MySQL tables if needed.");
    })
    .catch((error) => {
        console.error("Failed to initialize MySQL DB:", error);
        process.exit(1);
    });

// Clear SNMP data
clearSnmpData()
    .then((deletedCount) => {
        console.log(`Cleared ${deletedCount} SNMP records.`);
    })
    .catch((error) => {
        console.error("Failed to clear SNMP data:", error);
        process.exit(1);
    });

const app = express();

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: true}));
app.use(
    express.raw({
        type: "application/octet-stream",
        limit: "10000mb"
    })
);

// Routes
app.use("/api", imageRoutes);
app.use("/api", snmpRoutes);

// Start SNMP polling
snmpService.startSnmpPolling();

// Start server
const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}.`);
});
