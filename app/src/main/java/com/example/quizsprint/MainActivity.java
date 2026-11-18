package com.example.quizsprint;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Screen 1 - Email + Subject Selection.
 * Uses: L9 Spinner, L5 AlertDialog, L3 lifecycle logs, Intent to the next screen.
 * The email is checked here, and each email gets only DBmanager.MAX_ATTEMPTS attempts
 * per subject.
 */
public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "Lifecycle";

    Spinner spinnerSubject;
    TextView tvInfo, tvAttemptsInfo;
    EditText etEmail;
    Button btnStart, btnExit;
    DBmanager db;
    String selectedSubject = QuestionBank.SUBJECTS[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Log.d(TAG, "MainActivity onCreate invoked");

        spinnerSubject = findViewById(R.id.spinnerSubject);
        tvInfo = findViewById(R.id.tvInfo);
        tvAttemptsInfo = findViewById(R.id.tvAttemptsInfo);
        etEmail = findViewById(R.id.etEmail);
        btnStart = findViewById(R.id.btnStart);
        btnExit = findViewById(R.id.btnExit);
        db = new DBmanager(this);

        // Spinner with the subjects (Lab 9)
        spinnerSubject.setOnItemSelectedListener(this);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, QuestionBank.SUBJECTS);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(ad);

        // Show how many attempts are left while the student types the email
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateAttemptsInfo();
            }
        });

        // Start -> check the email and the attempt limit, then open QuizActivity
        // and send the subject + email with putExtra()
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    etEmail.setError("Please enter your email");
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etEmail.setError("Please enter a valid email");
                    return;
                }
                if (db.attemptsLeft(email, selectedSubject) <= 0) {
                    AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
                    ab.setTitle("No attempts left");
                    ab.setMessage("This email has already used all "
                            + DBmanager.MAX_ATTEMPTS + " attempts for " + selectedSubject
                            + ". Please choose another subject.");
                    ab.setPositiveButton("OK", null);
                    ab.show();
                    return;
                }
                btnStart.setEnabled(false);   // stop a double tap from using 2 attempts
                Intent intent = new Intent(MainActivity.this, QuizActivity.class);
                intent.putExtra("subject", selectedSubject);
                intent.putExtra("email", email);
                startActivity(intent);
            }
        });

        // Database Report -> shows the question counts and the email attempts (proof)
        findViewById(R.id.btnReport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ReportActivity.class));
            }
        });

        // Exit -> AlertDialog (Lab 5)
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
                ab.setTitle("Exit Alert");
                ab.setMessage("Are you sure you want to exit?");
                ab.setCancelable(false);
                ab.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(MainActivity.this, "Selected Option : YES",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
                ab.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(MainActivity.this, "Selected Option : NO",
                                Toast.LENGTH_SHORT).show();
                        dialogInterface.dismiss();
                    }
                });
                ab.show();
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedSubject = QuestionBank.SUBJECTS[position];
        tvInfo.setText(QuestionBank.QUESTIONS_PER_QUIZ + " questions on " + selectedSubject
                + "  -  30 seconds each  -  " + DBmanager.MAX_ATTEMPTS
                + " attempts per email");
        updateAttemptsInfo();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    // Shows "x of 2 attempts left" once a valid email is typed.
    private void updateAttemptsInfo() {
        String email = etEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvAttemptsInfo.setText("");
            return;
        }
        int left = db.attemptsLeft(email, selectedSubject);
        tvAttemptsInfo.setText(left + " of " + DBmanager.MAX_ATTEMPTS
                + " attempts left for this email in " + selectedSubject);
    }

    // ---- Activity life cycle logs (Lab 3) ----
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity onStart invoked");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity onResume invoked");
        btnStart.setEnabled(true);   // it was switched off while the quiz was opening
        updateAttemptsInfo();        // attempts may have changed after a quiz
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity onPause invoked");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity onStop invoked");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "MainActivity onRestart invoked");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity onDestroy invoked");
    }
}
