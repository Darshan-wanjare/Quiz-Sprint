package com.example.quizsprint;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Screen 3 - Result.
 * Uses: L11 RatingBar + switch on the score, L12 SQLite (reads the attempts of this email).
 */
public class ResultActivity extends AppCompatActivity {

    TextView tvResultSubject, tvScore, tvRemark, tvHistory, tvEmail, tvAttempts;
    RatingBar ratingBar;
    Button btnTryAgain, btnHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvResultSubject = findViewById(R.id.tvResultSubject);
        tvScore = findViewById(R.id.tvScore);
        tvRemark = findViewById(R.id.tvRemark);
        tvHistory = findViewById(R.id.tvHistory);
        tvEmail = findViewById(R.id.tvEmail);
        tvAttempts = findViewById(R.id.tvAttempts);
        ratingBar = findViewById(R.id.ratingBar);
        btnTryAgain = findViewById(R.id.btnTryAgain);
        btnHome = findViewById(R.id.btnHome);

        // Read the values sent by QuizActivity (getXxxExtra)
        final String subject = getIntent().getStringExtra("subject");
        final String email = getIntent().getStringExtra("email");
        int score = getIntent().getIntExtra("score", 0);
        int total = getIntent().getIntExtra("total", 10);
        if (subject == null || email == null) {
            Toast.makeText(this, "No result to show", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvResultSubject.setText("Subject: " + subject);
        tvScore.setText(score + " / " + total);

        // Remark using switch on the score (as in Lab 11)
        String remark;
        switch (score) {
            case 10:
                remark = "Perfect score! Outstanding!";
                break;
            case 9:
            case 8:
                remark = "Excellent work!";
                break;
            case 7:
            case 6:
                remark = "Good job, keep going!";
                break;
            case 5:
            case 4:
                remark = "Average - revise once more.";
                break;
            default:
                remark = "Needs more practice.";
        }
        tvRemark.setText(remark);

        // Read-only RatingBar: 10 marks = 5 stars
        ratingBar.setRating(total == 0 ? 0 : (score * 5f) / total);

        // The score was already saved by QuizActivity. Here we only read the history
        // of THIS email for THIS subject and check how many attempts are left.
        DBmanager db = new DBmanager(this);
        tvHistory.setText(db.getAttemptHistory(email, subject));

        int used = db.attemptsUsed(email, subject);
        int left = db.attemptsLeft(email, subject);
        tvEmail.setText("Email: " + DBmanager.normalizeEmail(email));
        if (left > 0) {
            tvAttempts.setText("Attempts used: " + used + " of " + DBmanager.MAX_ATTEMPTS
                    + "  (" + left + " left)");
        } else {
            tvAttempts.setText("Attempts used: " + used + " of " + DBmanager.MAX_ATTEMPTS
                    + "  - no attempts left for this email in " + subject);
            // no more tries: switch the Try Again button off
            btnTryAgain.setEnabled(false);
            btnTryAgain.setAlpha(0.4f);
        }

        btnTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnTryAgain.setEnabled(false);   // stop a double tap from using 2 attempts
                Intent intent = new Intent(ResultActivity.this, QuizActivity.class);
                intent.putExtra("subject", subject);
                intent.putExtra("email", email);
                startActivity(intent);
                finish();
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();   // MainActivity is still below in the back stack
            }
        });
    }
}
