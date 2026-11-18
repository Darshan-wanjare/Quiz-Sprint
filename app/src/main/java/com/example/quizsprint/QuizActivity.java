package com.example.quizsprint;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

/**
 * Screen 2 - Quiz.
 * Uses: L8 RadioGroup, L5 AlertDialog, Toast, CountDownTimer (stretch), shuffle (stretch).
 *
 * New concept - keeping STATE across repeated screen updates:
 * the same screen is reused for all 10 questions, so we keep the question number
 * (currentQ), the score and the attempt id in fields. They are also saved in
 * onSaveInstanceState() so the quiz survives a screen rotation.
 */
public class QuizActivity extends AppCompatActivity {

    private static final long QUESTION_TIME = 30000;   // 30 seconds per question

    TextView tvSubject, tvProgress, tvTimer, tvQuestion;
    ProgressBar progressBar;
    RadioGroup rgOptions;
    RadioButton[] rb = new RadioButton[4];
    Button btnNext, btnQuit;

    String subject;
    String email;
    DBmanager db;

    // ---- state kept between questions ----
    long attemptId = -1;    // row of this attempt in the "attempts" table
    List<Question> quiz;    // the 10 questions chosen for this attempt
    int currentQ = 0;       // which question we are on (0 - 9)
    int score = 0;          // correct answers so far
    long timeLeft = QUESTION_TIME;
    CountDownTimer timer;
    boolean quitDialogOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Read the subject sent by MainActivity (getStringExtra)
        subject = getIntent().getStringExtra("subject");
        email = getIntent().getStringExtra("email");
        if (subject == null || email == null) {
            Toast.makeText(this, "No subject or email selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvSubject = findViewById(R.id.tvSubject);
        tvProgress = findViewById(R.id.tvProgress);
        tvTimer = findViewById(R.id.tvTimer);
        tvQuestion = findViewById(R.id.tvQuestion);
        progressBar = findViewById(R.id.progressBar);
        rgOptions = findViewById(R.id.rgOptions);
        rb[0] = findViewById(R.id.rbA);
        rb[1] = findViewById(R.id.rbB);
        rb[2] = findViewById(R.id.rbC);
        rb[3] = findViewById(R.id.rbD);
        btnNext = findViewById(R.id.btnNext);
        btnQuit = findViewById(R.id.btnQuit);

        tvSubject.setText(subject + " Quiz");
        db = new DBmanager(this);

        int selected = -1;
        if (savedInstanceState != null) {
            // screen was rotated: the same attempt continues, load its SAME questions again
            attemptId = savedInstanceState.getLong("attemptId", -1);
            currentQ = savedInstanceState.getInt("currentQ");
            score = savedInstanceState.getInt("score");
            timeLeft = savedInstanceState.getLong("timeLeft");
            selected = savedInstanceState.getInt("selected", -1);
            if (attemptId != -1) quiz = db.getAttemptQuestions(attemptId);
        }
        if (quiz == null || quiz.isEmpty()) {
            // fresh start: this uses up one attempt of this email and picks 10 questions
            // that this email has not seen before
            attemptId = db.startAttempt(email, subject);
            if (attemptId == DBmanager.NO_ATTEMPTS_LEFT) {
                Toast.makeText(this, "No attempts left for this email in " + subject,
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (attemptId < 0) {
                Toast.makeText(this, "Could not load the questions", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            quiz = db.getAttemptQuestions(attemptId);
            currentQ = 0;
            score = 0;
            timeLeft = QUESTION_TIME;
            selected = -1;
        }

        showQuestion();
        if (selected >= 0 && selected < 4) rb[selected].setChecked(true);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitAnswer(false);
            }
        });

        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showQuitDialog();
            }
        });

        // Back button also asks for confirmation
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showQuitDialog();
            }
        });
    }

    // Puts the current question and its 4 options on the screen.
    private void showQuestion() {
        Question q = quiz.get(currentQ);
        tvQuestion.setText(q.text);
        for (int i = 0; i < 4; i++) {
            rb[i].setText(q.options[i]);
        }
        rgOptions.clearCheck();
        tvProgress.setText("Question " + (currentQ + 1) + " of " + quiz.size()
                + "     |     Score: " + score);
        progressBar.setMax(quiz.size());
        progressBar.setProgress(currentQ + 1);
        if (currentQ == quiz.size() - 1) {
            btnNext.setText("Finish");
        } else {
            btnNext.setText("Next");
        }
    }

    // Called by the Next button, or by the timer when time is up.
    private void submitAnswer(boolean timedOut) {
        int checkedId = rgOptions.getCheckedRadioButtonId();

        // Toast if Next is pressed without an answer
        if (checkedId == -1 && !timedOut) {
            Toast.makeText(this, "Please select an answer first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (timer != null) timer.cancel();

        if (checkedId != -1) {
            int chosen = rgOptions.indexOfChild(findViewById(checkedId));
            if (chosen == quiz.get(currentQ).answer) {
                score++;
            }
        }

        currentQ++;
        if (currentQ < quiz.size()) {
            timeLeft = QUESTION_TIME;
            showQuestion();
            startTimer(timeLeft);
        } else {
            finishQuiz();
        }
    }

    // 30-second timer for each question (stretch feature)
    private void startTimer(long millis) {
        if (timer != null) timer.cancel();
        if (millis <= 0) millis = QUESTION_TIME;
        timer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                tvTimer.setText(((millisUntilFinished + 999) / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvTimer.setText("0s");
                Toast.makeText(QuizActivity.this, "Time's up!", Toast.LENGTH_SHORT).show();
                submitAnswer(true);
            }
        }.start();
    }

    // AlertDialog to confirm quitting in the middle of the quiz (Lab 5)
    private void showQuitDialog() {
        if (timer != null) timer.cancel();   // pause the timer while the dialog is open
        quitDialogOpen = true;
        AlertDialog.Builder ab = new AlertDialog.Builder(QuizActivity.this);
        ab.setTitle("Quit Quiz");
        ab.setMessage("Your progress will be lost and this attempt will still be counted "
                + "(" + DBmanager.MAX_ATTEMPTS + " attempts per email). Do you really want to quit?");
        ab.setCancelable(false);
        ab.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        ab.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                quitDialogOpen = false;
                startTimer(timeLeft);          // carry on from where the timer stopped
            }
        });
        ab.show();
    }

    // Go to the Result screen, sending the score with putExtra()
    private void finishQuiz() {
        // save the score of this attempt in SQLite
        String res = db.finishAttempt(attemptId, score, quiz.size());
        Toast.makeText(this, res, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
        intent.putExtra("subject", subject);
        intent.putExtra("email", email);
        intent.putExtra("score", score);
        intent.putExtra("total", quiz.size());
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // do not restart the timer while the quit dialog is still open
        if (quiz != null && currentQ < quiz.size() && !quitDialogOpen) startTimer(timeLeft);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) timer.cancel();     // do not count time while app is in background
    }

    // Keep the quiz state when the screen rotates
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (quiz == null || rgOptions == null) return;   // nothing to save
        outState.putLong("attemptId", attemptId);
        outState.putInt("currentQ", currentQ);
        outState.putInt("score", score);
        outState.putLong("timeLeft", timeLeft);
        int checkedId = rgOptions.getCheckedRadioButtonId();
        outState.putInt("selected",
                checkedId == -1 ? -1 : rgOptions.indexOfChild(findViewById(checkedId)));
    }
}
