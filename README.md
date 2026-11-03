# MathBuddy 🦉 — Voice AI Math Tutor for Children

**MathBuddy** is a child-friendly, voice-interactive AI math tutor designed for children aged 4–6. Children can speak to Barnaby the Owl, solve math problems aloud, listen to friendly explanations, and track their personalized learning journey.

---

## 🌟 Hackathon Mandatory Workflow
1. **Voice In 🎙️**: Child speaks into the microphone using the native Browser Web Speech API (`SpeechRecognition`).
2. **AI Understands 🧠**: Google Gemini understands natural language voice intents (e.g. starting quizzes, answering questions, asking for help/explanations, changing topics/difficulty).
3. **Real Action ⚡**: Spring Boot 3 backend deterministically validates mathematical answers, tracks progress, updates stars, and persists attempts in MySQL.
4. **Voice Out 🔊**: Barnaby the Owl speaks aloud using the browser's `SpeechSynthesis` API.

---

## ✨ Key Features
- **Child Profiles & Data Isolation**: Support for multiple learner profiles (even with identical names) with completely isolated quiz histories, stars, and recommendations.
- **Owl's Math Adventure**: Interactive, step-by-step visual explanations for incorrect answers using pedagogical teaching strategies (Addition, Subtraction, Counting, Comparison, Missing Numbers, Equal Groups, and Fair Sharing).
- **MathVisual Component**: Visual representations using concrete objects (apples 🍎, cookies 🍪, stars ⭐, balloons 🎈) with animations for addition, visual cross-outs for subtraction, numbered stepping stones, and comparison groups.
- **Participatory Learning & Retries**: Encourages children to participate with follow-up prompts and a "Try Again" workflow.
- **Rainbow Classroom UI**: Pastel sky, sunshine yellow, mint, and lavender theme with Barnaby the Owl SVG mascot, large touch targets, and accessible keypad controls.

---

## 🛠️ Technology Stack
- **Backend**: Java 21, Spring Boot 3 (Spring Web, Spring Data JPA)
- **Database**: MySQL
- **AI Integration**: Google Gemini API (`gemini-1.5-flash`)
- **Frontend**: HTML5, Modern CSS3, Vanilla JavaScript (ES6+)
- **Speech APIs**: Browser Web Speech API (`webkitSpeechRecognition` & `SpeechSynthesis`)

---

## 🚀 Getting Started

### Prerequisites
- Java 17 or 21
- Maven 3.8+
- MySQL 8.0+ running on port 3306

### Environment Variables
Configure your Gemini API key (optional for fallback mode):
```bash
export GEMINI_API_KEY="your-gemini-api-key"
```

### Database Configuration
Update `mathbuddy/src/main/resources/application.properties` if needed:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mathbuddy?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
```

### Run the Application
```bash
cd mathbuddy
mvn spring-boot:run
```
Or package and run the JAR:
```bash
mvn package -DskipTests
java -jar target/mathbuddy-1.0.0.jar
```

Open your browser at:
```
http://localhost:8080
```
