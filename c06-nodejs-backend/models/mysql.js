const mysql = require("mysql2/promise");
const config = require("../config/db.config");

const pool = mysql.createPool({
    host: config.mysql.host,
    user: config.mysql.user,
    password: config.mysql.password,
    database: config.mysql.database
});

async function saveImage(jobId, imageData) {
    const connection = await pool.getConnection();
    try {
        await connection.query(
            "INSERT INTO images (job_id, data) VALUES (?, ?)",
            [jobId, imageData]
        );
    } finally {
        connection.release();
    }
}

async function getImageByJobId(jobId) {
    const connection = await pool.getConnection();
    try {
        const [rows] = await connection.query(
            "SELECT data FROM images WHERE job_id = ?",
            [jobId]
        );
        return rows[0];
    } finally {
        connection.release();
    }
}

module.exports = {
    saveImage,
    getImageByJobId
};
