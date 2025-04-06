<p align="center">
  <img src="https://i.imgur.com/9ZUmJUo.png" alt="Logo" width="100">
  <h3 align="center">ZoomMate</h3>
</p>

**ZoomMate** is a distributed application designed to zoom into BMP images using a microservices architecture orchestrated via Docker. Whether you're zooming in or out, ZoomMate gives you detailed control over large BMP files, making image exploration an interactive experience. 🖼️🔍

This project was developed as part of an academic assignment focused on containerization, messaging systems, and backend/frontend integration.

---

## What It Does 🧠

ZoomMate receives a BMP image from the user via the frontend, processes it in multiple backend services (including zooming operations using RMI servers), and delivers the zoomed image back to the user for download.

### 🔁 General Flow:
1. 🖼️ Upload a BMP image from the web UI.
2. 📩 The backend Java service sends the image through a **JMS Topic** to a **JMS Broker**.
3. 📬 An EJB MDB subscribed to the topic receives the image and distributes the processing to **two RMI Servers**.
4. 🧠 After zoom processing, the modified image is stored in a **MySQL Database**.
5. 📡 The backend notifies the frontend via **WebSocket** and provides a download link.
6. 📊 Additionally, **SNMP metrics** are collected and stored in **MongoDB**.

---

## Technologies Used 🛠️

- **Frontend:** Next.js (React)
- **Backend:**
  - Java with **Javalin** & JMS Client (C01)
  - **Apache ActiveMQ** (C02)
  - **Jakarta EE EJB MDB** with RMI Client (C03)
  - **Apache TomEE RMI Servers** (C04 & C05)
  - **Node.js** with Express (C06)
- **Databases:**
  - **MySQL** for storing images
  - **MongoDB** for SNMP metrics
- **Docker & Docker Compose**
- **SNMP** for monitoring container metrics

---

## Architecture Diagram 🖼️

<p align="center">
  <img src="./diagram.png" alt="Architecture Diagram" width="800">
</p>

---

## How to Run It 🚀

### ✅ Prerequisites:
- Docker & Docker Compose installed on your system.

### ▶️ Start the App:
```bash
docker-compose -p zoom-mate up --build
```

### 🌍 Access the Application:
Open your browser and navigate to:  
```
http://localhost/
```

---

## Container Breakdown 🧩

| Container | Role |
|----------|------|
| **C01** | Java Backend with Javalin REST & JMS Publisher |
| **C02** | JMS Broker (Apache ActiveMQ) |
| **C03** | EJB MDB JMS Subscriber & RMI Client |
| **C04 & C05** | Java RMI Servers for zooming logic |
| **C06** | Node.js backend with Express, exposing image & metrics APIs |
| **C07** | MySQL for storing zoomed BMPs |
| **C08** | MongoDB for SNMP container metrics |
| **C09** | Frontend in Next.js (React) |

---

## Environment Configuration 🔧

All container interactions and environment variables are already defined in the `docker-compose.yml`. SNMP is configured on each container using port `161/udp`.

Make sure ports like `8081`, `3001`, `80`, `1099`, and `61616` are available before launching.

---

## License 📄

This project is licensed under the [MIT License](LICENSE).
