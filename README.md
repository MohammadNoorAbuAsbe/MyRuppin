# MyRuppin

MyRuppin is an Android application designed to help students at Ruppin Academic Center manage their academic schedules, grades, and upcoming events. The app provides a user-friendly interface for viewing course schedules, tracking grades, and staying updated with important events.

## Features

- **Login and Authentication**: Secure login using student credentials.
- **Grade Management**: View and filter course grades, including cumulative and annual averages.
- **Schedule Management**: View course schedules in both list and calendar views.
- **Event Tracking**: Stay updated with current and upcoming events.
- **Notifications**: Receive notifications for grade updates.
- **Dark Mode**: Supports both light and dark themes.

## Installation

1. **Clone the repository**:
    ```sh
    git clone https://github.com/yourusername/MyRuppin.git
    cd MyRuppin
    ```

2. **Open the project in Android Studio**:
    - Open Android Studio.
    - Select `Open an existing project`.
    - Navigate to the cloned repository and select the `MyRuppin` folder.

3. **Build the project**:
    - Ensure you have the necessary SDKs installed.
    - Click on `Build` > `Rebuild Project`.

4. **Run the application**:
    - Connect an Android device or start an emulator.
    - Click on `Run` > `Run 'app'`.

## Usage

1. **Login**:
    - Enter your student ID and password to log in.
    - If credentials are saved, the app will attempt to auto-login.

2. **Home Screen**:
    - View current and upcoming events.
    - Navigate to grades and schedule screens using the menu.

3. **Grades Screen**:
    - View and filter course grades.
    - See cumulative and annual averages.

4. **Schedule Screen**:
    - Toggle between list and calendar views.
    - View detailed schedules for selected days.

## Project Structure

- **MainActivity.kt**: Entry point of the application.
- **data**: Contains data models, repositories, and the TokenManager for managing authentication tokens.
- **screens**: Contains the UI screens for login, home, grades, and schedule.
- **ui/components**: Contains reusable UI components.
- **ui/theme**: Contains theme definitions for the application.
- **utils**: Contains utility classes for date formatting and UI scaling.
- **viewmodels**: Contains ViewModel classes for managing UI-related data.

## Dependencies

- **Kotlin Coroutines**: For asynchronous programming.
- **OkHttp**: For network requests.
- **Jetpack Compose**: For building the UI.
- **DataStore**: For storing user preferences and authentication tokens.
- **WorkManager**: For scheduling background tasks.

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature-branch`).
3. Make your changes.
4. Commit your changes (`git commit -m 'Add some feature'`).
5. Push to the branch (`git push origin feature-branch`).
6. Open a pull request.

## License

This project is licensed under the AGPL-3.0 License. See the [LICENSE](LICENSE) file for details.

## Contact

For any questions or feedback, please contact Mohammad Noor Abu Asbe at [your-email@example.com].

---

Thank you for using MyRuppin! I hope it helps you manage your academic life more efficiently.
