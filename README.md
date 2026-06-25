RouteSmart – Intelligent Navigation Assistant 🚗📍
<p align="center"> <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge"/> <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=for-the-badge"/> <img src="https://img.shields.io/badge/Maps-OpenStreetMap-blue?style=for-the-badge"/> <img src="https://img.shields.io/badge/API-OSRM%20%7C%20Overpass-orange?style=for-the-badge"/> <img src="https://img.shields.io/badge/Status-Active-success?style=for-the-badge"/> </p>
📱 Overview

RouteSmart is an intelligent Android navigation application that goes beyond traditional route guidance by providing real-time smart recommendations, live GPS tracking, nearby place suggestions, voice assistance, and an integrated travel chatbot.

Unlike conventional navigation apps that only provide directions, RouteSmart helps travelers discover useful places such as hospitals, fuel stations, ATMs, restaurants, cafes, hotels, and shops during their journey.

✨ Features
🗺️ Smart Route Navigation
Find routes between source and destination.
Display route on map using OSRM.
Show distance and estimated travel time.
Real-time route visualization.
📍 Live GPS Tracking
Track current user location.
Automatically update location while travelling.
Map follows user movement.
Dynamic location marker.
🧠 Smart Place Recommendations
Recommendations within a 15 km radius.
Nearby:
Hospitals
Fuel Stations
Restaurants
Cafes
Hotels
ATMs
Pharmacies
Police Stations
Shops
🎤 Voice Assistant
Voice-based place search.
Speech-to-text integration.
Ask for nearby places while driving.
💬 Travel Chatbot
Rule-based travel assistant.
Provides:
Nearby places
Safety suggestions
Fuel information
Food recommendations
Emergency assistance
📌 Interactive Map Markers
Recommended places shown directly on the map.
Colorful markers for important locations.
Easy navigation to nearby places.
🎨 Modern User Interface
Floating chatbot button.
Voice assistant button.
Bottom sheet recommendations.
Smooth animations.
🚀 Problem Statement

Traditional navigation applications primarily focus on routing and often ignore the traveler's immediate needs during the journey.

RouteSmart addresses this problem by:

Providing intelligent travel recommendations.
Enhancing traveler safety.
Suggesting useful stops.
Improving travel convenience.
Supporting hands-free interaction.
🏗️ System Architecture
User Input
     ↓
MapActivity
     ↓
GPS Location Services
     ↓
Overpass API / OSRM API / Nominatim API
     ↓
Data Processing
     ↓
Map + Recommendation Cards + Chatbot
🛠️ Tech Stack
Category	Technology
Language	Kotlin
UI	XML
Maps	osmdroid
Routing	OSRM API
Geocoding	Nominatim API
Nearby Places	Overpass API
Location Services	FusedLocationProviderClient
Networking	OkHttp
Architecture	MVVM-inspired
IDE	Android Studio
📂 Project Structure
RouteSmart/
│
├── activities/
│   └── MapActivity.kt
│
├── api/
│   ├── OsrmApi.kt
│   ├── OverpassApi.kt
│   └── NominatimApi.kt
│
├── adapter/
│   └── RecommendationsAdapter.kt
│
├── model/
│   ├── RouteInfo.kt
│   ├── NearbyPlace.kt
│   └── RecommendedPlace.kt
│
├── utils/
│
├── res/
│   ├── layout/
│   ├── drawable/
│   └── values/
│
└── AndroidManifest.xml
📡 APIs Used
1. OSRM API

Used for:

Route generation
Distance calculation
Travel duration estimation
2. Overpass API

Used for:

Nearby places
Points of interest
Smart recommendations
3. Nominatim API

Used for:

Geocoding
Location search
Address conversion
📍 Nearby Place Categories
🏥 Hospital
⛽ Fuel Station
☕ Cafe
🍽️ Restaurant
🏨 Hotel
🏧 ATM
💊 Pharmacy
👮 Police Station
🛒 Shop
⚡ EV Charging Station
💬 Chatbot Commands

Examples:

Hospital near me
ATM nearby
Fuel station
Coffee shop
Restaurant nearby
Hotel near me
Safety tips
🎤 Voice Commands
Find nearby hospital
Show nearest ATM
Find fuel station
Search restaurant
Locate hotel
📸 Screenshots

Add your application screenshots here.

screenshots/
│
├── home.png
├── route.png
├── recommendations.png
├── chatbot.png
└── voice_assistant.png
⚙️ Installation
Clone the repository.
git clone https://github.com/yourusername/RouteSmart.git
Open in Android Studio.
Sync Gradle.
Run the application on:
Android Emulator
Physical Device
🔐 Permissions Required
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
📈 Future Enhancements
AI-powered chatbot integration.
Real-time traffic analysis.
Offline maps support.
Weather-aware recommendations.
Personalized travel suggestions.
Emergency SOS system.
Route optimization using AI.
Travel history and analytics.
🎓 Learning Outcomes
Android Development using Kotlin.
OpenStreetMap integration.
REST API consumption.
GPS and Location Services.
Voice recognition.
RecyclerView implementation.
Map overlays and markers.
Real-time recommendation systems.
👨‍💻 Developed By

Nishant Yadav
B.Tech Computer Science Engineering (CSE)
Android Developer | Full Stack Developer | Cloud Enthusiast

⭐ If you found this project useful, please give it a star on GitHub.
📧 Contact
LinkedIn: Add your profile link
GitHub: Add your GitHub profile
Email: Add your email

🚗 RouteSmart
Navigate Smarter. Travel Safer. Explore Better.
