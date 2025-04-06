const express = require("express");
const router = express.Router();
const mysqlModel = require("../models/mysql");

// POST /api/upload
router.post("/upload", async (req, res) => {
    try {
        const jobId = req.headers["jobid"];
        const imageData = req.body;

        if (!jobId) {
            return res.status(400).json({message: "Missing job ID."});
        }

        if (!(imageData instanceof Buffer)) {
            return res.status(400).json({message: "Invalid image data."});
        }

        console.log(`Start processing image for jobId: ${jobId}.`);

        await mysqlModel.saveImage(jobId, imageData);

        return res.status(200).json({
            message: "Image saved successfully.",
            downloadUrl: `/api/image/${jobId}`
        });
    } catch (error) {
        console.error("Error saving image:", error);
        return res.status(500).json({message: "Error saving image."});
    }
});

// GET /api/image/:jobId
router.get("/image/:jobId", async (req, res) => {
    try {
        const {jobId} = req.params;
        const imageRecord = await mysqlModel.getImageByJobId(jobId);

        if (!jobId) {
            return res.status(400).json({message: "Missing job ID."});
        }

        if (!imageRecord) {
            return res.status(404).json({message: "Image not found."});
        }

        res.set("Content-Type", "image/bmp");
        return res.send(imageRecord.data);
    } catch (error) {
        console.error("Error fetching image:", error);
        return res.status(500).json({message: "Error fetching image."});
    }
});

module.exports = router;
