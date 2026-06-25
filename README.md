# Bidly

## Requirements
* **Java 21** & **Spring Boot 3.3.1**
* **Docker** & **Node.js**

## Project Overview
### Bidly is an online auction and bidding platform. It allows users to list items for sale, while other users can place live bids to compete and purchase those items through an auction system.

## Run Commands
### 1. Email Server (MailDev)
### docker run -d -p 1025:1025 -p 1080:1080 maildev/maildev
### 2. Monitoring (Prometheus & Grafana)
### docker compose up -d
## Useful Links
### Application & Services
### Frontend: http://localhost:5173/
### Eureka Dashboard: http://localhost:8761/
### MailDev: http://localhost:1080
### Observability & Metrics
### Actuator: http://localhost:8080/actuator/prometheus
### Prometheus Targets: http://localhost:9090/targets
### Grafana UI Home: http://localhost:3000

## Microservices Arhitecture
### The application uses a Microservices Architecture. The Gateway Service acts as the single entry point (port 8080) and automatically routes every incoming API call to its corresponding backend microservice.

### System Diagram
![Bidly Architecture](images/architecture.jpg)

### Gateway Service: Port 8080 (Routes all incoming requests)
### Auth Service: Port 8081 (Handles authentication & users)
### Auction Service: Port 8082 (Handles listings & bidding)
### Notification Service: Port 8083 (Handles emails & notifications)
### Discovery Service (Eureka): Port 8761 (Service registry)
### Frontend (React): Port 5173

## API Documentation
#### Auth Service
#### All authentication endpoints are prefixed with '/api/v1/auth'
#### Post | /register | Register a new user account
#### GET | /confirm?token=... | Confirms the user account via email token
#### POST | /login | Authenticates a user and returns Access & Refresh Tokens.
#### POST | /refresh-token | Generates a new Access Token using a valid Refresh Token.

