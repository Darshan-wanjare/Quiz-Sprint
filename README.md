# Quiz Sprint

Quiz Sprint is an Android quiz app for students. A student enters an email, picks a subject, and answers 10 timed multiple-choice questions. Each email can attempt each subject only **2 times**, and every new attempt uses questions that email has not seen before.

This is a group college project, written in **Java** with a **SQLite** database.

## Download

Install the APK directly on an Android phone: go to the **Releases** section of this repository (right side of the page) and download `QuizSprint.apk`.

On the phone you may need to allow **Install unknown apps** for the app you open the file from.

## Features

- **3 subjects:** Java, DBMS and Android
- **200+ questions per subject**, stored in a SQLite database
- **10 random questions** in every quiz, with the answer options shuffled
- **30-second timer** for each question; when time runs out the app moves to the next question
- **Email check:** the email must be valid, and only **2 attempts per email per subject** are allowed. Capital letters and extra spaces are ignored, so `Sham@Gmail.com` and `sham@gmail.com` count as the same email
- **No repeated questions:** a new attempt picks questions that the same email has not received before
- **Fair attempts:** quitting a quiz in the middle still counts as an attempt
- **Progress is kept** when the phone screen is rotated
- **Result screen** with the final score
- **Database report screen** that shows how many questions each subject has, and every email with its attempts and scores

## Screens

1. **Start screen:** enter the email, choose the subject, and see how many attempts are left
2. **Quiz screen:** question, 4 options, question number, score and timer
3. **Result screen:** the score for the attempt
4. **Report screen:** question counts per subject and the attempts of every email

## Tech used

| Part | Technology |
| --- | --- |
| Language | Java |
| Platform | Android (minimum SDK 24, target SDK 37) |
| Database | SQLite (`quizdb`, tables `questions` and `attempts`) |
| Build tool | Gradle (Kotlin DSL) |
| UI | Android Views with Material theme |

## How to run the project

1. Install [Android Studio](https://developer.android.com/studio).
2. Clone this repository, or download it as a ZIP and unzip it.
3. In Android Studio, click **File > Open** and choose the project folder.
4. Wait for **Gradle sync** to finish.
5. Click **Run** to start the app on an emulator or a connected phone.

To build an APK, click **Build > Build Bundle(s) / APK(s) > Build APK(s)**. The file is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```
app/src/main/
├── java/com/example/quizsprint/
│   ├── MainActivity.java     Start screen: email, subject, attempt limit
│   ├── QuizActivity.java     Quiz screen: questions, timer, score
│   ├── ResultActivity.java   Result screen
│   ├── ReportActivity.java   Database report screen
│   ├── DBmanager.java        SQLite database and attempt rules
│   ├── QuestionBank.java     Reads questions and picks new ones for each attempt
│   └── Question.java         One question with its options
├── assets/
│   ├── questions_java.txt
│   ├── questions_dbms.txt
│   └── questions_android.txt
└── res/                      Layouts, colors, themes and icons
```

## How to add or change questions

The questions are in the text files inside `app/src/main/assets/`. One line is one question, written like this:

```
question @@ CORRECT answer @@ wrong answer 1 @@ wrong answer 2 @@ wrong answer 3
```

- Always write the **correct answer first**. The app shuffles the options by itself.
- Lines that start with `#` are comments and are ignored.
- After you edit a question file, increase `dbversion` by 1 in `DBmanager.java`. The app then rebuilds the questions table the next time it starts.

## Author

T.M. Sanjay Kumar and team, BCA, KLE Society PC Jabin Science College.
