module.exports = {
    mysql: {
        host: process.env.MYSQL_HOST || "0.0.0.0",
        user: process.env.MYSQL_USER || "stud",
        password: process.env.MYSQL_PASSWORD || "stud",
        database: process.env.MYSQL_DATABASE || "database"
    },
    mongo: {
        url: process.env.MONGO_URL || "mongodb://stud:stud@0.0.0.0:27017/",
        database: process.env.MONGO_DB_NAME || "database"
    }
};
  