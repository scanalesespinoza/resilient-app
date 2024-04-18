
# Resilient App

## About
This document provides essential information about "Resilient App" which is designed to provide solutions for rate limiting and response time monitoring in database operations. This project uses innovative technologies to ensure efficient handling of high-load scenarios and improve system responsiveness.

## Setup
To set up this project, follow these steps:
1. Clone the repository:
   ```
   git clone [https://github.com/your-username/your-project-name.git](https://github.com/scanalesespinoza/resilient-app.git)
   ```
2. Navigate to the project directory:
   ```
   cd resilient-app
   ```
3. Install dependencies:
   ```
   mvn install
   ```

## Usage
To use this project, start the application with:
```
mvn quarkus:dev
```
or
```
java -jar target/your-project-name.jar
```
Navigate to `http://localhost:8080/users` to access the application.

```
# HealthCheck Endpoints
http://localhost:8080/q/health/
http://localhost:8080/q/health/live
http://localhost:8080/q/health/ready
# Service Endpoints
http://localhost:8080/users/check
http://localhost:8080/users/all
```

## Features
- **Dynamic Rate Limiting**: Adjust the rate limiting based on the database response time.
- **Automatic Rate Reset**: Automatically resets the rate limit if the thresholds are not exceeded within a specific time frame.
- **Thread-Safe Operations**: Ensures that adjustments to rate limiting are thread-safe under high-load conditions.

## Contributing
Contributions are welcome, and can be made via issues and merge requests. If you have improvements or encounter bugs, please open an issue on the GitHub repository.

## License
This project is licensed under the Apache License 2.0. See the LICENSE file for more details.
