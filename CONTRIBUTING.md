# Contributing to MindArc V2

Thank you for your interest in contributing to **MindArc V2**! This project is a continuation of the [original MindArc](https://github.com/ChandraSagarDev/MindArc), focused on adding comprehensive app blocking and digital wellbeing enforcement features. We welcome contributions from the community to help make this a robust solution for combating screen addiction.

---

## 📋 Table of Contents

- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Submitting Changes](#submitting-changes)
- [Reporting Bugs](#reporting-bugs)
- [Suggesting Features](#suggesting-features)
- [Code Review Process](#code-review-process)

---

## 🤝 How Can I Contribute?

There are several ways you can contribute to MindArc V2:

### 1. **Implement V2 Features**
Help build the core blocking and restriction functionality:
- App blocking mechanism using AccessibilityService
- Activity-based unlocking system
- App selection interface
- Lock warning screens
- Block enforcement logic

### 2. **Enhance Existing Features**
Improve the inherited base features:
- Improve ML Kit pose detection accuracy
- Add new exercise types (jumping jacks, planks, etc.)
- Enhance reading activity verification
- Optimize UI/UX in Jetpack Compose
- Improve gamification mechanics

### 3. **Fix Bugs**
- Check the [Issues](../../issues) page for reported bugs
- Test edge cases and report new issues
- Submit fixes with proper testing

### 4. **Documentation**
- Improve code comments and documentation
- Write tutorials or guides
- Update the README with new features
- Create architecture diagrams

### 5. **Testing**
- Write unit tests and UI tests
- Test on different devices and Android versions
- Report compatibility issues
- Performance testing and optimization

---

## 🛠️ Development Setup

### Prerequisites

- **Android Studio**: Latest stable version (Arctic Fox or newer recommended)
- **JDK**: Version 11 or higher
- **Android SDK**: API Level 26+ (Android 8.0+)
- **Physical Device or Emulator**: For testing camera-based features and accessibility services

### Setup Steps

1. **Fork the Repository**
   ```bash
   # Click the 'Fork' button on GitHub, then clone your fork
   git clone https://github.com/YOUR_USERNAME/MindArc_V2.git
   cd MindArc_V2
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository

3. **Sync Gradle**
   - Android Studio should automatically sync Gradle
   - If not, click "Sync Project with Gradle Files"

4. **Run the App**
   - Connect a physical device or start an emulator
   - Click the "Run" button or press `Shift + F10`
   - Grant necessary permissions (Camera, Accessibility, Usage Stats)

5. **Create a Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

---

## 📝 Coding Standards

### General Guidelines

- **Language**: Kotlin only (100% Kotlin codebase)
- **Architecture**: Follow Clean Architecture principles (UI → ViewModel → Repository → Data Source)
- **UI**: Use Jetpack Compose for all UI components
- **Naming**: Use descriptive, meaningful names (camelCase for variables/functions, PascalCase for classes)
- **Comments**: Write clear comments for complex logic; use KDoc for public APIs

### Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable and function names
- Avoid magic numbers; use constants

### Compose Best Practices

```kotlin
// Good: State hoisting
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    ActivityContent(
        uiState = uiState,
        onEventClick = viewModel::handleEvent
    )
}

@Composable
fun ActivityContent(
    uiState: ActivityUiState,
    onEventClick: (Event) -> Unit
) {
    // UI implementation
}
```

### ViewModel Pattern

```kotlin
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repository: ActivityRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            repository.getData()
                .catch { /* Handle error */ }
                .collect { data ->
                    _uiState.value = UiState.Success(data)
                }
        }
    }
}
```

### Dependency Injection with Hilt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideRepository(
        dataSource: DataSource
    ): Repository = RepositoryImpl(dataSource)
}
```

---

## 🚀 Submitting Changes

### Pull Request Process

1. **Ensure Your Code Works**
   - Build succeeds without errors
   - App runs on a physical device
   - All existing features still work
   - New features work as expected

2. **Write/Update Tests**
   - Add unit tests for new logic
   - Update existing tests if behavior changes
   - Aim for good test coverage

3. **Update Documentation**
   - Update README.md if adding user-facing features
   - Add/update code comments for complex logic
   - Update CHANGELOG.md (if exists)

4. **Commit Your Changes**
   ```bash
   git add .
   git commit -m "feat: Add app blocking mechanism using AccessibilityService"
   ```

   **Commit Message Format:**
   - `feat:` New feature
   - `fix:` Bug fix
   - `docs:` Documentation changes
   - `refactor:` Code refactoring
   - `test:` Adding or updating tests
   - `chore:` Maintenance tasks

5. **Push to Your Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Create a Pull Request**
   - Go to the original repository on GitHub
   - Click "New Pull Request"
   - Select your fork and branch
   - Fill in the PR template with:
     - **What**: What changes you made
     - **Why**: Why these changes are needed
     - **How**: How you implemented the changes
     - **Testing**: How you tested the changes
     - **Screenshots**: If UI changes, include before/after screenshots

---

## 🐛 Reporting Bugs

### Before Submitting a Bug Report

- Check if the bug has already been reported in [Issues](../../issues)
- Try to reproduce on the latest version
- Gather relevant information (device, Android version, logs)

### Bug Report Template

**Title**: Short, descriptive title

**Description**:
- What happened?
- What did you expect to happen?

**Steps to Reproduce**:
1. Step one
2. Step two
3. ...

**Environment**:
- Device: [e.g., Pixel 6]
- Android Version: [e.g., Android 13]
- App Version: [e.g., v2.0.0]

**Logs/Screenshots**:
- Attach relevant logcat output
- Include screenshots if applicable

**Additional Context**:
- Any other relevant information

---

## 💡 Suggesting Features

We welcome feature suggestions! Please use the following template:

**Feature Title**: Short, descriptive title

**Problem Statement**:
- What problem does this solve?
- Who will benefit from this?

**Proposed Solution**:
- How should this feature work?
- Any alternative solutions considered?

**Priority**:
- [ ] Critical (core V2 functionality)
- [ ] High (important enhancement)
- [ ] Medium (nice to have)
- [ ] Low (future consideration)

**Additional Context**:
- Mockups, diagrams, or examples
- Related issues or discussions

---

## 🔍 Code Review Process

### What We Look For

- **Functionality**: Does it work as intended?
- **Code Quality**: Is it clean, readable, and maintainable?
- **Architecture**: Does it follow Clean Architecture principles?
- **Testing**: Are there adequate tests?
- **Documentation**: Is it well-documented?
- **Performance**: Is it optimized for mobile devices?
- **Security**: Are there any security concerns (especially for blocking features)?

### Review Timeline

- Initial review within 3-5 days
- Feedback provided with constructive comments
- Approved PRs merged promptly
- Large features may require multiple review rounds

### After Your PR is Merged

- Your contribution will be acknowledged in release notes
- Update your fork to stay in sync:
  ```bash
  git checkout main
  git pull upstream main
  git push origin main
  ```

---

## 🙏 Acknowledgments

- Credit to the [original MindArc project](https://github.com/ChandraSagarDev/MindArc) for the foundational codebase
- All contributors who help improve MindArc V2
- The Android and Jetpack Compose communities for excellent resources

---

## 📞 Questions?

If you have questions about contributing:
- Open a [Discussion](../../discussions) on GitHub
- Check existing issues and pull requests
- Review the main [README.md](README.md) for project overview

**Thank you for contributing to MindArc V2! Together, we're building a better digital wellbeing solution.** 🚀

---
