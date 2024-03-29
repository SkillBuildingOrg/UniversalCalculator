

package com.fusionleap.android.calculator.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.fusionleap.android.calculator.BaseModule;
import com.fusionleap.android.calculator.CalculatorSettings;
import com.fusionleap.android.calculator.EquationFormatter;
import com.fusionleap.calculator.R;

public class CalculatorEditText extends EditText {
    private static final int BLINK = 500;

    private EquationFormatter mEquationFormatter;
    private AdvancedDisplay mDisplay;
    private long mShowCursor = SystemClock.uptimeMillis();
    Paint mHighlightPaint = new Paint();
    Handler mHandler = new Handler();
    Runnable mRefresher = new Runnable() {
        @Override
        public void run() {
            CalculatorEditText.this.invalidate();
        }
    };
    private String input = "";
    private int selectionHandle = 0;

    public CalculatorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUp();
    }

    public CalculatorEditText(final AdvancedDisplay display) {
        super(display.getContext());
        setUp();
        mDisplay = display;
        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) display.mActiveEditText = CalculatorEditText.this;
            }
        });
    }

    private void setUp() {
        setCustomSelectionActionModeCallback(new NoTextSelectionMode());
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);

        mEquationFormatter = new EquationFormatter();

        addTextChangedListener(new TextWatcher() {
            boolean updating = false;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(updating) return;

                input = s.toString().replace(EquationFormatter.PLACEHOLDER, EquationFormatter.POWER).replaceAll(",", "").replaceAll(" ", "");
                updating = true;

                // Get the selection handle, since we're setting text and
                // that'll overwrite it
                selectionHandle = getSelectionStart();
                // Adjust the handle by removing any comas or spacing to the
                // left
                String cs = s.subSequence(0, selectionHandle).toString();
                selectionHandle -= countOccurrences(cs, ',');
                selectionHandle -= countOccurrences(cs, ' ');

                setText(formatText(input));
                setSelection(Math.min(selectionHandle, getText().length()));
                updating = false;
            }
        });
    }

    class NoTextSelectionMode implements ActionMode.Callback {
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Prevents the selection action mode on double tap.
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {}

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    }

    @Override
    public String toString() {
        return input;
    }

    @Override
    public View focusSearch(int direction) {
        switch(direction) {
        case View.FOCUS_FORWARD:
            View v = mDisplay.nextView(this);
            while(!v.isFocusable())
                v = mDisplay.nextView(v);
            return v;
        }
        return super.focusSearch(direction);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // TextViews don't draw the cursor if textLength is 0. Because we're an
        // array of TextViews, we'd prefer that it did.
        if(getText().length() == 0 && isEnabled() && (isFocused() || isPressed())) {
            if((SystemClock.uptimeMillis() - mShowCursor) % (2 * BLINK) < BLINK) {
                mHighlightPaint.setColor(getCurrentTextColor());
                mHighlightPaint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight(), mHighlightPaint);
                mHandler.postAtTime(mRefresher, SystemClock.uptimeMillis() + BLINK);
            }
        }
    }

    private Spanned formatText(String input) {
        BaseModule bm = mDisplay.mLogic.mBaseModule;
        if(CalculatorSettings.digitGrouping(getContext())) {
            // Add grouping, and then split on the selection handle
            // which is saved as a unique char
            String grouped = bm.groupSentence(input, selectionHandle);
            if(grouped.contains(String.valueOf(BaseModule.SELECTION_HANDLE))) {
                String[] temp = grouped.split(String.valueOf(BaseModule.SELECTION_HANDLE));
                selectionHandle = temp[0].length();
                input = "";
                for(String s : temp) {
                    input += s;
                }
            }
            else {
                input = grouped;
                selectionHandle = input.length();
            }
        }
        return Html.fromHtml(mEquationFormatter.insertSupscripts(input));
    }

    private int countOccurrences(String haystack, char needle) {
        int count = 0;
        for(int i = 0; i < haystack.length(); i++) {
            if(haystack.charAt(i) == needle) {
                count++;
            }
        }
        return count;
    }

    public static String load(final AdvancedDisplay parent) {
        return CalculatorEditText.load("", parent);
    }

    public static String load(String text, final AdvancedDisplay parent) {
        return CalculatorEditText.load(text, parent, parent.getChildCount());
    }

    public static String load(String text, final AdvancedDisplay parent, final int pos) {
        final CalculatorEditText et = new CalculatorEditText(parent);
        et.setText(text);
        et.setSelection(0);
        if(parent.mKeyListener != null) et.setKeyListener(parent.mKeyListener);
        if(parent.mFactory != null) et.setEditableFactory(parent.mFactory);
        et.setBackgroundResource(android.R.color.transparent);
        et.setTextAppearance(parent.getContext(), R.style.display_style);
        et.setPadding(5, 0, 5, 0);
        et.setEnabled(parent.isEnabled());
        AdvancedDisplay.LayoutParams params = new AdvancedDisplay.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        et.setLayoutParams(params);
        parent.addView(et, pos);
        return "";
    }
}
