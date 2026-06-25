🚗 RouteSmart – Intelligent Travel & Navigation Assistant
<div align="center">
Navigate Smarter • Travel Safer • Explore Better

An AI-inspired Android navigation application that combines real-time routing, smart recommendations, live GPS tracking, voice assistance, and an intelligent travel chatbot to enhance the overall travel experience.

<p align="center"> <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"> <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"> <img src="https://img.shields.io/badge/Maps-OpenStreetMap-7EBC6F?style=for-the-badge"> <img src="https://img.shields.io/badge/API-OSRM%20%7C%20Overpass-orange?style=for-the-badge"> <img src="https://img.shields.io/badge/Status-Active-success?style=for-the-badge"> </p> </div>
🌟 About RouteSmart

RouteSmart is a next-generation Android navigation application designed to go beyond traditional route guidance.

Unlike conventional navigation systems that only provide directions, RouteSmart intelligently assists travelers by offering:

✅ Smart nearby recommendations
✅ Live GPS-based suggestions
✅ Voice-assisted interactions
✅ Travel safety guidance
✅ Interactive map experiences
✅ Intelligent travel chatbot support

The application continuously monitors the user's location and delivers contextual recommendations such as hospitals, fuel stations, restaurants, ATMs, cafes, hotels, and safety-related information.

📱 Key Features
🗺️ Intelligent Route Navigation
Route calculation between source and destination.
Distance and travel duration estimation.
Dynamic route visualization.
Real-time route rendering.
📍 Live GPS Tracking
Continuous location updates.
Automatic map centering.
Live location marker movement.
GPS-based recommendation updates.
🧠 Smart Recommendations

RouteSmart recommends places within a 15 km radius based on the user's live location.

Categories Supported
🏥 Hospitals
⛽ Fuel Stations
☕ Cafes
🍽 Restaurants
🏨 Hotels
🏧 ATMs
💊 Pharmacies
👮 Police Stations
🛒 Shops
⚡ EV Charging Stations
🎤 Voice Assistant

Hands-free travel assistance.

Users can simply speak:

"Find nearby hospital"
"Show fuel station"
"Locate ATM"
"Find a restaurant"

The application converts speech to text and responds intelligently.

💬 Smart Travel Chatbot

The integrated travel assistant helps users with:

Nearby places
Safety suggestions
Emergency support
Fuel recommendations
Travel assistance
📌 Interactive Map Markers
Nearby places shown directly on the map.
Marker-based navigation.
Quick access to recommendations.
Visual representation of important locations.
🚀 Problem Statement

Most navigation applications primarily focus on route guidance and ignore the traveler's real-time needs during the journey.

RouteSmart addresses this challenge by providing:

Context-aware travel assistance.
Intelligent recommendations.
Safety-aware navigation.
Voice-enabled interactions.
Real-time travel support.
🏗️ System Architecture
          User Input
               │
               ▼
         MapActivity
               │
 ┌─────────────┼─────────────┐
 ▼             ▼             ▼
GPS       OSRM API     Nominatim API
Location     │               │
             ▼
       Overpass API
             │
             ▼
    Data Processing Layer
             │
             ▼
Map + Recommendations + Chatbot
🛠️ Tech Stack
Category	Technology
Language	Kotlin
UI Design	XML
Maps	osmdroid
Routing	OSRM API
Nearby Places	Overpass API
Geocoding	Nominatim API
Location Services	FusedLocationProviderClient
Networking	OkHttp
Architecture	Modular Architecture
IDE	Android Studio
📂 Project Structure
RouteSmart
│
├── activities
│   └── MapActivity.kt
│
├── api
│   ├── OsrmApi.kt
│   ├── OverpassApi.kt
│   └── NominatimApi.kt
│
├── adapter
│   └── RecommendationsAdapter.kt
│
├── model
│   ├── RouteInfo.kt
│   ├── NearbyPlace.kt
│   └── RecommendedPlace.kt
│
├── res
│   ├── drawable
│   ├── layout
│   └── values
│
└── AndroidManifest.xml
📡 APIs Used
OSRM API
Route generation
Distance calculation
Travel duration estimation
Overpass API
Nearby place recommendations
Points of interest
Smart suggestions
Nominatim API
Address search
Geocoding
Location conversion
💬 Sample Chatbot Queries
Hospital near me
ATM nearby
Fuel station
Restaurant nearby
Coffee shop
Safety tips
Hotel near me
🎤 Sample Voice Commands
Find nearby hospital
Show nearest ATM
Find fuel station
Locate a cafe
Search restaurant
Find hotel
📸 Application Screenshots

Add your screenshots inside the screenshots/ folder.

screenshots/
│
├── home_screen.png
├── navigation_screen.png
├── recommendations.png
├── chatbot.png
├── voice_assistant.png
└── route_map.png
⚙️ Installation
Clone the Repository
git clone https://github.com/nishanty23/RouteSmart.git
Open in Android Studio
File → Open → RouteSmart
Sync Gradle
Run on:
Android Emulator
Physical Android Device
🔐 Permissions Required
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
📈 Future Enhancements
🤖 AI-powered chatbot integration.
🚦 Real-time traffic analysis.
🌦 Weather-aware recommendations.
🗺 Offline map support.
🚨 Emergency SOS system.
🧠 Personalized travel recommendations.
📊 Travel analytics dashboard.
🎯 AI-based route optimization.
🎓 Learning Outcomes
Android application development using Kotlin.
OpenStreetMap integration.
REST API consumption.
GPS and location services.
Speech recognition.
Map overlays and markers.
Real-time recommendation systems.
User-centric mobile application design.
👨‍💻 Developer
Nishant Yadav

B.Tech Computer Science Engineering (CSE)
Android Developer • Full Stack Developer • Cloud Enthusiast

GitHub: https://github.com/nishanty23
LinkedIn: (Add your LinkedIn URL)
Email: (Add your email)
⭐ Support

If you found this project useful:

🌟 Star the repository
🍴 Fork the project
📢 Share your feedback

<div align="center">
🚗 RouteSmart
Navigate Smarter. Travel Safer. Explore Better.

Made with ❤️ using Kotlin and OpenStreetMap

</div>
🔥 Extra Suggestion
