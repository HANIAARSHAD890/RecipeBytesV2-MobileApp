<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="120"/>
</p>

**RecipeBytes** — your smart kitchen companion. Explore recipes, plan meals, and cook smarter with AI-powered features.

---

## 📲 Installation

1. Download `RecipeBytesAPK.apk`
2. Enable **Install from unknown sources** on your device
3. Open the APK and tap **Install**
4. Launch and start cooking!

---

## ✨ Features

### 🔐 Authentication & Profile
- Email/password sign up & sign in
- Profile image upload via Cloudinary
- Username & bio editing
- Session persistence with auto-login

### 📖 Recipe Management
- 5-step recipe creation wizard
- Cloudinary image upload (gallery/camera/URL)
- AI-powered nutrition calculation
- Drag-and-drop step reordering
- Edit & delete recipes with confirmation
- Public/private visibility toggle
- Draft auto-save & restore

### 🔍 Explore & Discover
- Browse all public recipes
- Search by title or description
- Filter by category, time, and recency
- List/grid layout toggle
- Like & favorite recipes
- View likers list

### 🤖 AI & Smart Input
- AI recipe generation from dish name
- OCR vision recipe extraction from photos
- Recipe extraction from YouTube/website links
- Smart suggest by available ingredients
- Ingredient comparison (have vs missing)

### 📅 Meal Planner
- Monthly calendar with meal indicator dots
- Categorize meals (breakfast/lunch/dinner/dessert)
- Chip-based meal selection
- Firestore persistence
- Scheduled meal reminder notifications

### 🛒 Shopping List
- Auto-generated from missing ingredients
- Real-time Firebase sync
- Checkbox with strikethrough
- Delete completed lists

### 🏠 Home Dashboard
- Recipe metrics (total/public/private/likes)
- Quick-action cards for all features

### 🎨 Personalization
- Light/dark mode toggle
- Follow system theme by default
- Last-screen memory on reopen

### 🔔 Notifications & Alerts
- Meal reminders via AlarmManager
- Boot-completion reminder
- Power-connected recipe prompt
- Offline connectivity alert

---

## 🛠 Tech Stack

- **Language:** Kotlin
- **Minimum SDK:** 24 (Android 7.0+)
- **Target SDK:** 36 (Android 15)
- **UI:** Material Design 3
- **Backend:** Firebase Auth, Realtime Database, Firestore
- **AI:** Groq API (LLaMA models)
- **Images:** Cloudinary CDN, Glide, Picasso
- **Persistence:** DataStore, Gson
- **Navigation:** Navigation Component
- **HTTP:** OkHttp

---

## 👨‍💻 Developed By

**RecipeBytes Team** — Hania Arshad & Maryyam Fakhar

*Making cooking simpler, one recipe at a time.*
