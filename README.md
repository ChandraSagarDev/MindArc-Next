# MindArc V2

**Earn your screen time through meaningful effort.**

---

## 📌 About This Project

This repository is a **continuation and further development** of the original [MindArc project](https://github.com/ChandraSagarDev/MindArc). MindArc is a digital wellbeing application for Android designed to help students and anyone struggling with screen addiction to build healthier digital habits.

**Base Version Focus**: The original MindArc project established a robust framework for activity tracking, gamification, and progress monitoring, encouraging users to engage in physical and cognitive activities.

**V2 Enhancement**: This continuation version introduces the **core app blocking and restriction functionality**, transforming MindArc from an activity tracker into a complete digital wellbeing solution where access to distracting apps must be earned through meaningful effort. V2 leverages the existing activity infrastructure to enforce discipline by linking screen access to productive behavior.

---

## ✅ Features Already Implemented (Base Version)

The original MindArc project successfully implemented the following activity tracking and gamification features, which serve as the foundation for V2:

### Physical Activities
- **Push-up Counter**: Real-time push-up detection and counting using device camera and ML Kit Pose Detection.
- **Squat Counter**: Real-time squat detection and counting using pose detection technology.

### Cognitive Activities
- **App-Provided Reading**: Pre-loaded articles and stories with comprehension quizzes to verify engagement.
- **User-Provided Reading**: Flexible reading mode where users can read their own materials (books, PDFs, documents) with reflection prompts to ensure genuine engagement.

### Gamification & Progress
- **Points & Rewards System**: Earn points for completing activities; points unlock apps for limited durations.
- **Unlock Sessions**: Time-limited access to restricted apps based on effort invested.
- **Daily Streaks**: Track consecutive days of activity completion.
- **Progress Analytics**: Comprehensive dashboard showing:
  - Total activities completed
  - Total points earned
  - Current streak and longest streak
  - Daily and weekly progress charts
  - Unlock session history
- **Achievement Badges**: Unlock achievements for reaching milestones.

### User Interface & Experience
- **Modern UI**: 100% Jetpack Compose for a declarative, responsive interface.
- **Intuitive Navigation**: Seamless navigation between screens using Navigation Compose.
- **Permission Management**: Guided permission setup for camera, accessibility, and usage stats.
- **Real-time Monitoring**: Live screen time tracking and app usage statistics.

---

## 🚧 V2 Enhancements (In Development)

This continuation version introduces new features and improvements:

### 🔒 Core V2 Features (Primary Focus)

These are the main features being implemented in V2 to transform MindArc into a complete digital wellbeing solution:

- **App Blocking & Control**: Select specific apps (e.g., social media, games) to restrict using AccessibilityService. Blocked apps cannot be accessed until a productive activity is completed.
- **Activity-Based Unlocking**: Leverage the existing activity infrastructure to unlock restricted apps - completing physical or cognitive activities grants time-limited access to blocked apps.
- **App Selection Interface**: User-friendly screen to browse installed apps and choose which ones to restrict.
- **Block Enforcement**: Real-time monitoring and interception of blocked apps using AccessibilityService.
- **Lock Warning Screen**: Informative screen shown when users attempt to access blocked apps, guiding them to choose an activity.

### 🎯 Additional Planned Features

Further enhancements planned for future releases:

**New Activity Types:**
- Additional exercise options:
  - Jumping jacks detection
  - Plank timer with form verification
  - Yoga pose detection
  - Custom workout challenges
- Mental activities:
  - Math problem solving
  - Memory games
  - Focus and breathing exercises

**Enhanced Features:**
- **Adaptive Difficulty System**: Dynamic adjustment of activity requirements based on user performance and progress.
- **Customizable Unlock Rules**: Flexible configuration of points-to-time ratios and activity requirements.
- **App-Specific Time Limits**: Set different daily limits for different restricted apps.
- **Smart Scheduling**: Time-based rules (e.g., auto-block during study hours, relax restrictions on weekends).

**Social & Competitive Features:**
- **Social Challenges**: Create and participate in challenges with friends.
- **Leaderboards**: Compare progress with other users (with privacy options).
- **Achievement Sharing**: Share milestones on social media or with friends.
- **Team Goals**: Collaborative challenges for groups or study circles.

**Integration & Expansion:**
- **Smartwatch Integration**: Track activities directly from wearable devices (Wear OS support).
- **Cloud Backup & Sync**: Synchronize progress across multiple devices.
- **Export Analytics**: Generate detailed reports and export data.
- **Parental/Study Group Mode**: Monitor and encourage progress for groups.

**User Experience Improvements:**
- **Dark Mode Enhancement**: Refined dark theme with better contrast and accessibility.
- **Widget Support**: Home screen widgets for quick stats and motivation.
- **Improved Onboarding**: Interactive tutorial and guided setup.
- **Notification Improvements**: Smarter reminders and motivational notifications.
- **Customizable Themes**: Personalized color schemes and UI customization.

**Technical Improvements:**
- **Performance Optimization**: Enhanced battery efficiency and reduced resource usage.
- **Offline Mode**: Full functionality without internet connection.
- **Enhanced Security**: Improved app blocking mechanisms to prevent bypasses.
- **Better ML Models**: More accurate pose detection with additional pose support.

---

## 🛠️ Tech Stack & Key Libraries

- **UI**: 100% Kotlin with [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern, declarative UI design.
- **Architecture**: Clean Architecture following recommended Android patterns (UI Layer → ViewModel → Repository → Data Source).
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/) for efficient dependency management.
- **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and Flows for reactive, non-blocking operations.
- **Database**: [Room](https://developer.android.com/jetpack/androidx/releases/room) for robust local data persistence.
- **Navigation**: [Jetpack Navigation for Compose](https://developer.android.com/jetpack/compose/navigation) for type-safe navigation.
- **Camera & Machine Learning**:
  - [CameraX](https://developer.android.com/training/camerax) for reliable camera operations.
  - [Google ML Kit Pose Detection](https://developers.google.com/ml-kit/vision/pose-detection) for real-time body pose analysis.
- **App Blocking**: [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) for monitoring and restricting app access.
- **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for reliable background processing.

---

## 🚀 Setup & Installation

### Prerequisites
- Android Studio (Latest stable version recommended)
- JDK 11 or higher
- Android device or emulator running Android 7.0 (API 24) or higher

### Installation Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/MindArc_V2.git
   cd MindArc_V2
   ```

2. **Open in Android Studio:**
   - Launch Android Studio
   - Select `File > Open`
   - Navigate to the cloned repository and select it

3. **Sync and Build:**
   - Allow Android Studio to sync Gradle files
   - Build the project: `Build > Make Project`
   - Wait for the build to complete successfully

4. **Run the Application:**
   - Connect an Android device or start an emulator
   - Click the Run button or press `Shift + F10`

5. **Configure Required Permissions:**
   
   After installation, you must grant necessary permissions:
   
   **Accessibility Service** (Required for app blocking):
   - Go to `Settings > Accessibility > Installed apps > MindArc`
   - Enable the MindArc service
   
   **Camera Permission** (Required for exercise detection):
   - Grant when prompted or via `Settings > Apps > MindArc > Permissions`
   
   **Usage Access** (Required for screen time tracking):
   - Go to `Settings > Apps > Special app access > Usage access`
   - Enable MindArc

---

## 📖 How to Use

### Current Base Version Workflow:
1. **Initial Setup**: Complete the onboarding process and grant required permissions.
2. **Choose an Activity**: Navigate to the activity selection screen and select between physical (push-ups, squats) or cognitive (reading) activities.
3. **Complete the Activity**: Follow the on-screen instructions to complete your chosen activity.
4. **Earn Points & Track Progress**: Accumulate points, build streaks, and unlock achievements visible on the Progress screen.

### V2 Enhanced Workflow (In Development):
1. **Initial Setup**: Complete the onboarding process and grant required permissions (including Accessibility Service).
2. **Select Apps to Block**: Choose which apps you want to restrict from the app selection screen.
3. **Attempt to Open a Blocked App**: When you try to open a restricted app, MindArc will intercept it with a lock warning screen.
4. **Choose an Activity**: Select between physical (push-ups, squats) or cognitive (reading) activities to unlock access.
5. **Complete the Activity**: Follow the on-screen instructions to complete your chosen activity.
6. **Earn Your Unlock**: Successfully completing the activity unlocks your blocked apps for a time-limited session.
7. **Track Your Progress**: View your achievements, streaks, unlock sessions, and statistics on the Progress screen.

---

## 🤝 Contributing

We welcome contributions from the community! Whether you're fixing bugs, adding new features, or improving documentation, your help is appreciated.

Please refer to our [CONTRIBUTING.md](CONTRIBUTING.md) file for detailed guidelines on how to contribute to this project.

---

## 📄 License

⚠️ This project is licensed for educational and research use only.  
Commercial use is not permitted without explicit permission from the authors.

Please refer to the LICENSE file for details.

---

## 🙏 Acknowledgments

- Original MindArc project and contributors: [github.com/ChandraSagarDev/MindArc](https://github.com/ChandraSagarDev/MindArc)
- Google ML Kit team for pose detection capabilities
- Android Jetpack team for modern development tools
- All contributors and supporters of this project

---

## 📧 Contact & Support

For questions, suggestions, or issues:
- Open an issue on GitHub
- Check existing issues and discussions
- Refer to the original MindArc repository for additional context

---

**Note**: This is an active development project. 

- **Base Version Features** (activity tracking, gamification, progress analytics) are fully functional and inherited from the original MindArc.
- **V2 Core Features** (app blocking and restriction enforcement) are currently under development.
- **Additional Planned Features** are on the roadmap for future releases.

The current codebase mirrors the base version as the first commit of V2 development is pending.
