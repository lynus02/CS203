# TARIFF Calculation
An application built with Spring Boot with Maven

## Development Prerequisites
- **Java JDK 21**
- **MySQL Community Server**

## Getting Started 
Follow these steps to get the development environment running.

1. **Clone the Repository**
```bash
git clone https://github.com/lynus02/CS203.git
```

2. **Install and Configure MySQL**
- **Download and Install:** Download MySQL Community Server from https://dev.mysql.com/downloads/
- **Set Root Password:** During installation, you will be prompted to set a password for the `root` user. **Remember this password**
- **Verify Installation:** Open a terminal and run the command below. Enter your password. If you see the `mysql>` prompt, it's working. Type `exit` to quit.
```terminal
mysql -u root -p
```

3. **Setting Up Database Environment Variables**

To ensure your MySQL credentials are kept secure and not committed to version control, please follow these steps to set up environment variables on your machine.

- In IntelliJ, go to `Run` > `Edit Configurations`
- Select your Spring Boot application configuration
- Under `Environment Variables`, add:
```text
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password
DB_URL=jdbc:mysql://localhost:3306/your_database
```
- Run the application to verify it can connect to your MySQL database.



