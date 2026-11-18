package com.example.quizsprint;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * SQLite helper (same pattern as Lab 12).
 * Database : quizdb
 * Tables   :
 *   questions (id, subject, question, opt_a, opt_b, opt_c, opt_d, answer)
 *       -> 200+ questions per subject, filled from the files in assets/
 *   attempts  (id, email, subject, question_ids, score, total, started_on, finished)
 *       -> one row per quiz attempt of an email. This table is also what we use to
 *          (1) count the attempts of an email and (2) remember which questions
 *          that email already got, so a new attempt gets different questions.
 *
 * IMPORTANT: if you edit the question files in assets/, increase dbversion by 1.
 * The app then rebuilds the questions table the next time it starts.
 */
public class DBmanager extends SQLiteOpenHelper {

    // One email can attempt each subject this many times (the first try counts as 1).
    public static final int MAX_ATTEMPTS = 2;

    public static final long NO_ATTEMPTS_LEFT = -1;
    public static final long FAILED = -2;

    private static final String dbname = "quizdb";
    private static final int dbversion = 2;

    private final Context context;

    public DBmanager(Context context) {
        super(context, dbname, null, dbversion);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table questions (id integer primary key autoincrement, "
                + "subject text, question text, opt_a text, opt_b text, opt_c text, "
                + "opt_d text, answer integer)");
        db.execSQL("create table attempts (id integer primary key autoincrement, "
                + "email text, subject text, question_ids text, score integer, "
                + "total integer, started_on text, finished integer default 0)");
        seedQuestions(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // version 1 only had a "scores" table (no email), so it is not needed any more
        db.execSQL("DROP TABLE IF EXISTS scores");
        db.execSQL("DROP TABLE IF EXISTS questions");
        db.execSQL("DROP TABLE IF EXISTS attempts");
        onCreate(db);
    }

    // Reads the question files from assets/ and inserts every question.
    // (onCreate/onUpgrade already run inside one transaction, so this is fast.)
    private void seedQuestions(SQLiteDatabase db) {
        Random rnd = new Random();
        for (String subject : QuestionBank.SUBJECTS) {
            String file = QuestionBank.assetFileFor(subject);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    context.getAssets().open(file), StandardCharsets.UTF_8))) {
                String line;
                int lineNo = 0;
                while ((line = br.readLine()) != null) {
                    lineNo++;
                    Question q;
                    try {
                        q = QuestionBank.parseLine(line, rnd);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException(file + " line " + lineNo + ": "
                                + e.getMessage());
                    }
                    if (q == null) continue;   // blank line or comment
                    ContentValues c = new ContentValues();
                    c.put("subject", subject);
                    c.put("question", q.text);
                    c.put("opt_a", q.options[0]);
                    c.put("opt_b", q.options[1]);
                    c.put("opt_c", q.options[2]);
                    c.put("opt_d", q.options[3]);
                    c.put("answer", q.answer);
                    db.insert("questions", null, c);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + file, e);
            }
        }
    }

    // "  Sham@Gmail.com " and "sham@gmail.com" are the same email.
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------------
    //  Attempts per email
    // ---------------------------------------------------------------------

    // How many times this email has already started this subject's quiz.
    public int attemptsUsed(String email, String subject) {
        return countAttempts(getReadableDatabase(), normalizeEmail(email), subject);
    }

    public int attemptsLeft(String email, String subject) {
        return Math.max(0, MAX_ATTEMPTS - attemptsUsed(email, subject));
    }

    private int countAttempts(SQLiteDatabase db, String email, String subject) {
        Cursor cur = db.rawQuery("SELECT COUNT(*) FROM attempts WHERE email = ? AND subject = ?",
                new String[]{email, subject});
        try {
            return cur.moveToFirst() ? cur.getInt(0) : 0;
        } finally {
            cur.close();
        }
    }

    /**
     * Starts a new attempt: checks the limit, chooses 10 questions this email has not
     * seen before and saves them. The attempt is counted from this moment, so quitting
     * in the middle of a quiz does not give a free retry.
     * Returns the attempt id, NO_ATTEMPTS_LEFT, or FAILED.
     */
    public long startAttempt(String email, String subject) {
        String mail = normalizeEmail(email);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (countAttempts(db, mail, subject) >= MAX_ATTEMPTS) return NO_ATTEMPTS_LEFT;

            // all question ids of this subject
            List<Long> allIds = new ArrayList<>();
            Cursor cur = db.rawQuery("SELECT id FROM questions WHERE subject = ?",
                    new String[]{subject});
            try {
                while (cur.moveToNext()) allIds.add(cur.getLong(0));
            } finally {
                cur.close();
            }

            // question ids this email already received for this subject
            Set<Long> used = new HashSet<>();
            cur = db.rawQuery("SELECT question_ids FROM attempts WHERE email = ? AND subject = ?",
                    new String[]{mail, subject});
            try {
                while (cur.moveToNext()) used.addAll(parseIds(cur.getString(0)));
            } finally {
                cur.close();
            }

            List<Long> picked = QuestionBank.pickQuestionIds(allIds, used,
                    QuestionBank.QUESTIONS_PER_QUIZ, new Random());
            if (picked.isEmpty()) return FAILED;

            ContentValues c = new ContentValues();
            c.put("email", mail);
            c.put("subject", subject);
            c.put("question_ids", joinIds(picked));
            c.put("total", picked.size());
            c.put("started_on", new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                    .format(new Date()));
            long id = db.insert("attempts", null, c);
            if (id == -1) return FAILED;
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    // The questions of an attempt, in the same order they were chosen.
    // (Used at the start of a quiz and again after a screen rotation.)
    public List<Question> getAttemptQuestions(long attemptId) {
        SQLiteDatabase db = getReadableDatabase();
        List<Long> ids = new ArrayList<>();
        Cursor cur = db.rawQuery("SELECT question_ids FROM attempts WHERE id = ?",
                new String[]{String.valueOf(attemptId)});
        try {
            if (cur.moveToFirst()) ids = parseIds(cur.getString(0));
        } finally {
            cur.close();
        }

        Map<Long, Question> byId = new HashMap<>();
        if (!ids.isEmpty()) {
            // ids are numbers read from our own table, so joining them is safe
            cur = db.rawQuery("SELECT id, question, opt_a, opt_b, opt_c, opt_d, answer "
                    + "FROM questions WHERE id IN (" + joinIds(ids) + ")", null);
            try {
                while (cur.moveToNext()) {
                    long id = cur.getLong(0);
                    byId.put(id, new Question(id, cur.getString(1),
                            new String[]{cur.getString(2), cur.getString(3),
                                    cur.getString(4), cur.getString(5)},
                            cur.getInt(6)));
                }
            } finally {
                cur.close();
            }
        }

        List<Question> result = new ArrayList<>();
        for (Long id : ids) {
            Question q = byId.get(id);
            if (q != null) result.add(q);
        }
        return result;
    }

    // Saves the score when the quiz is finished.
    public String finishAttempt(long attemptId, int score, int total) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues c = new ContentValues();
        c.put("score", score);
        c.put("total", total);
        c.put("finished", 1);
        int rows = db.update("attempts", c, "id = ?", new String[]{String.valueOf(attemptId)});
        return rows == 1 ? "Score saved" : "Failed to save score";
    }

    // Latest attempts of this email for this subject, newest first, as readable text.
    public String getAttemptHistory(String email, String subject) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT score, total, started_on, finished FROM attempts "
                        + "WHERE email = ? AND subject = ? ORDER BY id ASC",
                new String[]{normalizeEmail(email), subject});
        List<String> lines = new ArrayList<>();
        try {
            while (cur.moveToNext()) {
                String result = cur.getInt(3) == 1
                        ? cur.getInt(0) + " / " + cur.getInt(1)
                        : "not completed";
                lines.add("Attempt " + (lines.size() + 1) + ":  " + result
                        + "   -   " + cur.getString(2));
            }
        } finally {
            cur.close();
        }
        if (lines.isEmpty()) return "No attempts yet.";
        StringBuilder sb = new StringBuilder();
        for (int i = lines.size() - 1; i >= 0 && i >= lines.size() - 5; i--) {
            sb.append(lines.get(i)).append("\n");
        }
        return sb.toString().trim();
    }

    // ---------------------------------------------------------------------
    //  Report (ReportActivity shows this as proof for the teacher)
    // ---------------------------------------------------------------------

    // How many questions each subject has in the "questions" table.
    public String getQuestionCountReport() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT subject, COUNT(*) FROM questions "
                + "GROUP BY subject ORDER BY subject", null);
        StringBuilder sb = new StringBuilder();
        int total = 0;
        try {
            while (cur.moveToNext()) {
                int n = cur.getInt(1);
                total += n;
                sb.append(cur.getString(0)).append(":  ").append(n).append(" questions")
                        .append(n > 200 ? "   (more than 200)" : "   (NOT enough)")
                        .append("\n");
            }
        } finally {
            cur.close();
        }
        sb.append("Total:  ").append(total).append(" questions");
        return sb.toString();
    }

    // Every email, its attempts per subject, and how many questions repeated between attempts.
    public String getAttemptsReport() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT email, subject, question_ids, score, total, "
                + "started_on, finished FROM attempts ORDER BY email, subject, id", null);
        Map<String, List<String>> groups = new LinkedHashMap<>();   // "email - subject" -> lines
        Map<String, Set<Long>> seen = new HashMap<>();
        Map<String, Integer> repeats = new HashMap<>();
        try {
            while (cur.moveToNext()) {
                String key = cur.getString(0) + "  -  " + cur.getString(1);
                List<String> lines = groups.get(key);
                if (lines == null) {
                    lines = new ArrayList<>();
                    groups.put(key, lines);
                    seen.put(key, new HashSet<Long>());
                    repeats.put(key, 0);
                }
                int rep = repeats.get(key);
                for (Long id : parseIds(cur.getString(2))) {
                    if (!seen.get(key).add(id)) rep++;     // this question was given before
                }
                repeats.put(key, rep);
                String result = cur.getInt(6) == 1
                        ? cur.getInt(3) + " / " + cur.getInt(4) : "not completed";
                lines.add("Attempt " + (lines.size() + 1) + ":  " + result
                        + "  (" + cur.getString(5) + ")\n   question ids: "
                        + cur.getString(2).replace(",", ", "));
            }
        } finally {
            cur.close();
        }
        if (groups.isEmpty()) {
            return "No attempts yet. Take a quiz first, then open this report again.";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> e : groups.entrySet()) {
            int used = e.getValue().size();
            sb.append(e.getKey()).append("\n");
            sb.append("Attempts used: ").append(used).append(" of ").append(MAX_ATTEMPTS)
                    .append("  (").append(Math.max(0, MAX_ATTEMPTS - used)).append(" left)\n");
            for (String line : e.getValue()) sb.append(line).append("\n");
            sb.append("Questions repeated between attempts: ")
                    .append(repeats.get(e.getKey())).append("\n\n");
        }
        return sb.toString().trim();
    }

    // ---- helpers: "5,17,230" <-> list of ids ----
    private static String joinIds(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(ids.get(i));
        }
        return sb.toString();
    }

    private static List<Long> parseIds(String csv) {
        List<Long> ids = new ArrayList<>();
        if (csv == null || csv.isEmpty()) return ids;
        for (String p : csv.split(",")) ids.add(Long.parseLong(p.trim()));
        return ids;
    }
}
