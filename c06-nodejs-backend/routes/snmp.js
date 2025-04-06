const express = require("express");
const router = express.Router();
const mongoModel = require("../models/mongo");

// GET /api/snmp
router.get("/snmp", async (req, res) => {
    try {
        const minutes = parseInt(req.query.minutes, 10) || 1;

        if (minutes <= 0) {
            return res.status(400).json({message: "Invalid 'minutes' parameter."});
        }

        const snmpData = await mongoModel.getSnmpDataSince(minutes);
        return res.status(200).json(snmpData);
    } catch (error) {
        console.error("Error fetching SNMP data:", error);
        return res.status(500).json({message: "Error fetching SNMP data."});
    }
});

module.exports = router;
