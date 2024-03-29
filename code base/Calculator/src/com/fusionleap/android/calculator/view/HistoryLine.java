package com.fusionleap.android.calculator.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fusionleap.android.calculator.History;
import com.fusionleap.android.calculator.HistoryEntry;
import com.fusionleap.calculator.R;

public class HistoryLine extends LinearLayout {
    private static final int COPY = 0;
    private static final int COPY_BASE = 1;
    private static final int COPY_EDITED = 2;
    private static final int REMOVE = 3;
    private String[] mMenuItemsStrings;
    private HistoryEntry mHistoryEntry;
    private History mHistory;
    private BaseAdapter mAdapter;

    public HistoryLine(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu) {
        MenuHandler handler = new MenuHandler();
        if(mMenuItemsStrings == null) {
            Resources resources = getResources();
            mMenuItemsStrings = new String[4];
            mMenuItemsStrings[COPY] = String.format(resources.getString(R.string.copy), mHistoryEntry.getBase() + "=" + mHistoryEntry.getEdited());
            mMenuItemsStrings[COPY_BASE] = String.format(resources.getString(R.string.copy), mHistoryEntry.getBase());
            mMenuItemsStrings[COPY_EDITED] = String.format(resources.getString(R.string.copy), mHistoryEntry.getEdited());
            mMenuItemsStrings[REMOVE] = resources.getString(R.string.remove_from_history);
        }
        for(int i = 0; i < mMenuItemsStrings.length; i++) {
            menu.add(Menu.NONE, i, i, mMenuItemsStrings[i]).setOnMenuItemClickListener(handler);
        }
    }

    private class MenuHandler implements MenuItem.OnMenuItemClickListener {
        public boolean onMenuItemClick(MenuItem item) {
            return onTextContextMenuItem(item.getTitle());
        }
    }

    public boolean onTextContextMenuItem(CharSequence title) {
        boolean handled = false;
        if(TextUtils.equals(title, mMenuItemsStrings[COPY])) {
            copyContent(mHistoryEntry.getBase() + "=" + mHistoryEntry.getEdited());
            handled = true;
        }
        else if(TextUtils.equals(title, mMenuItemsStrings[COPY_BASE])) {
            copyContent(mHistoryEntry.getBase());
            handled = true;
        }
        else if(TextUtils.equals(title, mMenuItemsStrings[COPY_EDITED])) {
            copyContent(mHistoryEntry.getEdited());
            handled = true;
        }
        else if(TextUtils.equals(title, mMenuItemsStrings[REMOVE])) {
            removeContent();
            handled = true;
        }
        return handled;
    }

    public void copyContent(String content) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, content));
        String toastText = String.format(getResources().getString(R.string.text_copied_toast), content);
        Toast.makeText(getContext(), toastText, Toast.LENGTH_SHORT).show();
    }

    private void removeContent() {
        mHistory.remove(mHistoryEntry);
        mAdapter.notifyDataSetChanged();
    }

    public HistoryEntry getHistoryEntry() {
        return mHistoryEntry;
    }

    public void setHistoryEntry(HistoryEntry historyEntry) {
        this.mHistoryEntry = historyEntry;
    }

    public History getHistory() {
        return mHistory;
    }

    public void setHistory(History history) {
        this.mHistory = history;
    }

    public BaseAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(BaseAdapter adapter) {
        this.mAdapter = adapter;
    }

    public void showMenu() {
        showContextMenu();
    }
}
