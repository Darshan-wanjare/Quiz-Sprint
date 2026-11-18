package com.example.quizsprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Settings + helper logic for the questions (no Android code, so it is easy to test).
 *
 * The questions themselves live in text files in app/src/main/assets:
 *      questions_java.txt, questions_dbms.txt, questions_android.txt
 * DBmanager reads these files once and stores every question in the SQLite
 * table "questions".
 *
 * One line of a file = one question:
 *      question @@ CORRECT answer @@ wrong 1 @@ wrong 2 @@ wrong 3
 * The correct answer is always written FIRST in the file. The app shuffles the
 * 4 options when it loads the file, so on screen the correct one is in a random place.
 */
public class QuestionBank {

    public static final String[] SUBJECTS = {"Java", "DBMS", "Android"};

    public static final int QUESTIONS_PER_QUIZ = 10;

    private static final String SEPARATOR = " @@ ";

    // "Java" -> "questions_java.txt"
    public static String assetFileFor(String subject) {
        return "questions_" + subject.toLowerCase(Locale.ROOT) + ".txt";
    }

    /**
     * Turns one line of a question file into a Question.
     * Returns null for blank lines and for comment lines (starting with #).
     * Throws IllegalArgumentException if the line is not written correctly.
     */
    public static Question parseLine(String line, Random rnd) {
        String s = line.trim();
        if (s.isEmpty() || s.startsWith("#")) return null;

        String[] parts = s.split(SEPARATOR, -1);
        if (parts.length != 5) {
            throw new IllegalArgumentException("Expected 5 parts (question + 4 options) but found "
                    + parts.length + " in: " + s);
        }
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
            if (parts[i].isEmpty()) {
                throw new IllegalArgumentException("Empty part in: " + s);
            }
        }

        // parts[1] is the correct answer. Shuffle the 4 options and remember where it went.
        List<String> opts = new ArrayList<>();
        for (int i = 1; i <= 4; i++) opts.add(parts[i]);
        Collections.shuffle(opts, rnd);
        int answer = opts.indexOf(parts[1]);
        return new Question(0, parts[0], opts.toArray(new String[0]), answer);
    }

    /**
     * Chooses the questions for ONE attempt.
     *   allIds  = every question id of the subject
     *   usedIds = ids this email has already been given for this subject
     * Questions that were never shown to this email are chosen first, so the
     * questions do not repeat. Only if fewer than "count" new questions are left
     * (not possible with 200+ questions and 2 attempts) are old ones used to fill up.
     */
    public static List<Long> pickQuestionIds(List<Long> allIds, Set<Long> usedIds,
                                             int count, Random rnd) {
        List<Long> fresh = new ArrayList<>();
        List<Long> old = new ArrayList<>();
        for (Long id : allIds) {
            if (usedIds.contains(id)) old.add(id);
            else fresh.add(id);
        }
        Collections.shuffle(fresh, rnd);
        List<Long> picked = new ArrayList<>(fresh.subList(0, Math.min(count, fresh.size())));
        if (picked.size() < count) {
            Collections.shuffle(old, rnd);
            picked.addAll(old.subList(0, Math.min(count - picked.size(), old.size())));
        }
        Collections.shuffle(picked, rnd);
        return picked;
    }
}
