package com.example.quizsprint;

/**
 * One quiz question (plain data holder, no Android code in here).
 *  id      -> row id in the "questions" table
 *  text    -> the question
 *  options -> the 4 answer options (A, B, C, D)
 *  answer  -> index (0-3) of the correct option
 */
public class Question {
    public final long id;
    public final String text;
    public final String[] options;
    public final int answer;

    public Question(long id, String text, String[] options, int answer) {
        this.id = id;
        this.text = text;
        this.options = options;
        this.answer = answer;
    }
}
