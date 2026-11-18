package com.example.quizsprint;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Screen 4 - Database Report (proof for the teacher).
 * Reads the numbers live from the SQLite database "quizdb":
 *   - how many questions each subject has (table "questions")
 *   - every email with its attempts (table "attempts")
 */
public class ReportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvQuestionCounts = findViewById(R.id.tvQuestionCounts);
        TextView tvAttemptsReport = findViewById(R.id.tvAttemptsReport);
        TextView tvLimit = findViewById(R.id.tvLimit);
        Button btnBack = findViewById(R.id.btnReportBack);

        DBmanager db = new DBmanager(this);
        tvQuestionCounts.setText(db.getQuestionCountReport());
        tvAttemptsReport.setText(db.getAttemptsReport());
        tvLimit.setText("Rule: each email gets only " + DBmanager.MAX_ATTEMPTS
                + " attempts per subject.");

        btnBack.setOnClickListener(v -> finish());
    }
}
