# SplitSnap 🧾⚡

**SplitSnap** is a native Android expense-splitting application designed to eliminate manual receipt calculations. Built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Clean MVVM Architecture**, the app combines camera capture with multimodal AI parsing to convert physical receipts into interactive, assignable expense breakdowns in real time.

---

## 🌟 Key Features

* **AI Receipt OCR & Structured Parsing:** Utilizes the Google Gemini API to analyze raw receipt images and extract itemized lines, prices, subtotals, tax, and tips into strongly typed data models.
* **Dynamic Bill Splitting:** Assign single items or split shared dishes among group members with custom tip and tax distribution.
* **Reactive State & Live Calculations:** Built with Jetpack Compose and `StateFlow` for instant mathematical recalculations without UI stutter or lag.
* **Offline-Ready Persistence:** Integrated **Room Database (SQLite)** with Kotlin Coroutines and Flow for caching recent receipts, breakdowns, and past history locally.
* **Secure API Key Handling:** Utilizes the **Secrets Gradle Plugin** to load credentials locally from `.env` without exposing keys to version control.

---

## 🛠️ Tech Stack & Architecture

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose, Material 3
* **Architecture:** MVVM (Model-View-ViewModel) + Unidirectional Data Flow (UDF)
* **Local Persistence:** Room Database (SQLite), Flow, Coroutines
* **AI / Multimodal OCR:** Google Gemini API
* **Asynchronous Flow:** Kotlin Coroutines, StateFlow, SharedFlow
* **Build & Security:** Gradle (KTS), Secrets Gradle Plugin

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug | 2024.2+ (or newer)
* JDK 17+
* Android SDK (API 24 to 36)
* A Google Gemini API Key

### Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/endgrainww-byte/SplitSnap.git](https://github.com/endgrainww-byte/SplitSnap.git)
   cd SplitSnap
