package com.mbf.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/** Always-visible touch-and-drag A-Z navigation optimized for narrow phone screens. */
public final class AlphabetRail extends View {
    public interface OnLetterSelected { void onSelected(char letter); }
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final OnLetterSelected callback;
    private int foreground;
    private int highlight;
    private int active = -1;

    public AlphabetRail(Context context, int foreground, int highlight, OnLetterSelected callback) {
        super(context);
        this.foreground = foreground;
        this.highlight = highlight;
        this.callback = callback;
        setContentDescription("Indice alfabetico delle app; trascina per scorrere");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setColors(int foreground, int highlight) {
        this.foreground = foreground;
        this.highlight = highlight;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (height <= 0) return;
        float step = height / LETTERS.length();
        float size = Math.min(14f * getResources().getDisplayMetrics().scaledDensity, step * .75f);
        paint.setTextSize(size);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < LETTERS.length(); i++) {
            paint.setColor(active == i ? highlight : foreground);
            float y = getPaddingTop() + step * (i + .5f) - (paint.ascent() + paint.descent()) / 2f;
            canvas.drawText(String.valueOf(LETTERS.charAt(i)), getWidth() / 2f, y, paint);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            getParent().requestDisallowInterceptTouchEvent(true);
            float y = event.getY() - getPaddingTop();
            float usable = getHeight() - getPaddingTop() - getPaddingBottom();
            if (usable <= 0) return true;
            int position = Math.max(0, Math.min(LETTERS.length() - 1, (int) (y / usable * LETTERS.length())));
            if (active != position) {
                active = position;
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                callback.onSelected(LETTERS.charAt(position));
                invalidate();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            active = -1;
            invalidate();
            if (action == MotionEvent.ACTION_UP) performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
