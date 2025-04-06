const mysql = require("mysql2/promise");
const config = require("../config/db.config");

async function initDB() {
    const connection = await mysql.createConnection({
        host: config.mysql.host,
        user: config.mysql.user,
        password: config.mysql.password
    });

    await connection.query(
        `CREATE DATABASE IF NOT EXISTS \`${config.mysql.database}\`;`
    );

    await connection.query(`USE \`${config.mysql.database}\`;`);

    await connection.query(`
        CREATE TABLE IF NOT EXISTS images (
        job_id VARCHAR(255),
        data LONGBLOB,
        PRIMARY KEY (job_id)
        );
    `);

    await connection.end();
}

module.exports = {initDB};
