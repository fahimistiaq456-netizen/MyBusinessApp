# MyBusinessApp - Full-Stack Mobile & Backend Solution

A modern, high-performance Android enterprise application paired with a robust Spring Boot REST API backend.

---

## 📱 Architecture Overview

```
mybusineesapp/
├── MybussinessApp/      # Android Client (Kotlin + Jetpack Compose)
│   ├── app/             # Application source code and assets
│   ├── gradle/          # Gradle wrapper
│   └── build.gradle.kts # Project-level Gradle build configuration
│
└── demo/                # Backend Server (Spring Boot + Java 17 + MongoDB)
    ├── src/             # Controller, Model, Service, and Repository layers
    ├── gradle/          # Gradle wrapper
    └── build.gradle     # Spring Boot dependencies and build configuration
```

---

## 🚀 Projects

### 1. Android Application (`MybussinessApp`)
Built with modern Android engineering best practices:
- **UI Toolkit**: 100% Jetpack Compose with Material 3
- **Design System**: Fluid Glassmorphism design language with ambient dynamic lighting, 3D interactive tilt cards, and glowing radial gradients
- **Architecture**: MVVM + Unidirectional Data Flow (UDF) + Repository Pattern
- **Networking**: Retrofit 2 + OkHttp 4 + Gson
- **Asynchronous**: Kotlin Coroutines & StateFlow
- **Special Features**:
  - Arc Side Panel with dynamic gesture-morphing Canvas shapes
  - Home Screen Widget (`AppWidgetProvider`) with financial quick stats & deep-links
  - Live Weather Integration using OpenWeatherMap API
  - Comprehensive Authentication flow (Login, Sign-Up, Forgot Password, Reset Password)

### 2. Backend API (`demo`)
Enterprise-ready REST API powering authentication and data management:
- **Framework**: Spring Boot 3.3.4 (Java 17)
- **Database**: MongoDB with Spring Data MongoDB
- **Security**: BCrypt password encryption (`spring-security-crypto`)
- **Key Endpoints**:
  - User registration & validation
  - Login authentication
  - Password recovery and reset mechanisms

---

## 🛠️ Setup & Running Locally

### Prerequisites
- **Android Studio**: Ladybug / Hedgehog or newer
- **Java Development Kit**: JDK 17
- **MongoDB**: Running locally on `localhost:27017` or via MongoDB Atlas
- **Android SDK**: API Level 34+

### Running the Backend (`demo`)
```bash
cd demo
./gradlew bootRun
```
*Alternatively, run `run_backend.bat` to automatically set up ADB reverse port forwarding and launch the backend.*

### Running the Android App (`MybussinessApp`)
1. Open `MybussinessApp` in Android Studio.
2. In `local.properties`, configure your OpenWeather API key:
   ```properties
   OPEN_WEATHER_API_KEY=your_api_key_here
   ```
3. Sync Gradle and run on an Android Device / Emulator.

---

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.
