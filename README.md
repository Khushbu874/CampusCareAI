# CampusCareAI – Digital Complaint Box
## Introduction
**CampusCareAI (Digital Complaint Box)** is a secure, anonymous, and AI-powered platform that enables students to raise concerns without fear of exposure.  
Unlike traditional complaint boxes or basic digital forms, our system ensures **privacy, inclusivity, urgency detection, and AI-driven categorization**.  

Students can voice their issues in **natural language (text or speech)**.  
Admins benefit from **structured complaint categorization, emergency escalation, and transparent status tracking**.



## Features

### Multi-mode Complaint Submission
- Text input (natural language)  
- Voice input → Speech-to-Text conversion  
- File upload (PDF, DOC) for evidence or detailed reports  

### AI Intelligence
- Automatic complaint categorization  
- Urgency detection and escalation for emergencies  
- AI Chatbot for resolving minor/common issues instantly  
- Sentiment analysis for contextual understanding  

### Security & Transparency 
- End-to-End Encryption of complaints  
- Admin updates visible for real-time tracking  
- Evidence-based submission support  

### Emergency Mode
- Live location + recording submission  
- Immediate escalation to admin  

---

## System Architecture
The workflow of **CampusCareAI** ensures secure processing, categorization, and resolution of complaints:

1. **Submission** – Student submits complaint via text, voice, or file.  
2. **Encryption & Storage** – Complaint is encrypted and securely stored in Firebase Realtime Database.  
3. **AI Processing** – AI categorizes, translates, and detects urgency.  
4. **Tracking** – Students track complaint status anonymously.  
5. **Admin Resolution** – Admin dashboard provides role-based filtering and action.  
6. **Status Update** – Students receive anonymous updates.  


---

## Working Flow

1. **User Login/Access**  
   - Students: Secure login access.
   - Admins: Role-based authentication via Fireebase.  

2. **Complaint Submission**  
   - Text (typed)  
   - Voice (Speech-to-Text API)  
   - File upload (PDF/DOC for evidence)  

3. **Data Security**  
   - All complaints encrypted  
   - Stored in Firebase Realtime Database  

4. **AI Processing & Categorization**  
   - Categorizes complaints (minor, serious & emergency situation)  
   - Detects urgency & sentiment  

5. **Emergency Handling**  
   - Emergency Mode: live location + recording for admins  

6. **Admin Dashboard**  
   - View & filter complaints by category/urgency  
   - Update resolution status  

7. **Resolution & Tracking**  
   - Students track progress (submitted → in-progress → resolved)  

8. **AI Chatbot Assistance**  
   - Minor issues handled instantly by AI chatbot  

---

## Tech Stack

### Frontend
- **Android Studio** (XML for UI, Java for logic)

### Backend & Database
- **Firebase Realtime Database**  
- **Firebase Authentication**  
- **Firebase Hosting**

---

## Uniqueness
CampusCareAI is unique because it provides:  
- **AI-powered complaint categorization** & urgency detection  
- Support for **multiple input formats** (text, voice, file uploads)  
- **Emergency mode** with live location & recording support  
- Transparent **status tracking** for students  
- Inclusive design for students with communication challenges  

---

## Future Scope
- Blockchain integration for tamper-proof complaint records  
- AI-based predictive trend analysis of complaint data  
- Sign language support using AI interpretation  
- Offline submission via RFID/QR codes  
- Government adoption for **public grievance redressal**  
- Expansion to corporate offices, housing societies, NGOs  

---

## Installation

```bash
# Clone the repository
git clone https://github.com/Khushbu874/CampusCareAI.git

# Open the project in Android Studio

# Configure Firebase (Authentication & Realtime Database)
# - Add google-services.json to app/ folder
# - Enable Firebase Auth and Database in Firebase Console

# Run the project on emulator or Android device
