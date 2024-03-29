
package com.fusionleap.android.calculator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Point;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.fusionleap.android.calculator.view.CalculatorDisplay;
import com.fusionleap.android.calculator.view.CalculatorViewPager;
import com.fusionleap.android.calculator.view.Cling;
import com.fusionleap.android.calculator.view.HistoryLine;
import com.fusionleap.android.calculator.view.LegalPolicyWebViewclient;
import com.fusionleap.calculator.R;
import com.xlythe.slider.Slider;
import com.xlythe.slider.Slider.Direction;

public class Calculator extends Activity implements Logic.Listener, OnClickListener, OnMenuItemClickListener, CalculatorViewPager.OnPageChangeListener {
    public EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private ListView mHistoryView;
    private BaseAdapter mHistoryAdapter;
    private Logic mLogic;
    private CalculatorViewPager mPager;
    private CalculatorViewPager mSmallPager;
    private CalculatorViewPager mLargePager;
    private View mClearButton;
    private View mBackspaceButton;
    private View mOverflowMenuButton;
    private Slider mPulldown;
    private Graph mGraph;

    private boolean clingActive = false;

    public enum Panel {
        GRAPH, FUNCTION, HEX, BASIC, ADVANCED, MATRIX;

        int order;

        public void setOrder(final int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    public enum SmallPanel {
        HEX, ADVANCED, FUNCTION;

        int order;

        public void setOrder(final int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    public enum LargePanel {
        GRAPH, BASIC, MATRIX;

        int order;

        public void setOrder(final int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    private static final String STATE_CURRENT_VIEW = "state-current-view";
    private static final String STATE_CURRENT_VIEW_SMALL = "state-current-view-small";
    private static final String STATE_CURRENT_VIEW_LARGE = "state-current-view-large";
    private static final String PREFS_KEY_VERSION = "POLICY_VERSION";
    private final int CURRENT_POLICY_VERSION = 2;

    @Override
    public void onCreate(final Bundle state) {
        super.onCreate(state);

        // Disable IME for this application
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        setContentView(R.layout.main);
        mPager = (CalculatorViewPager)findViewById(R.id.panelswitch);
        mSmallPager = (CalculatorViewPager)findViewById(R.id.smallPanelswitch);
        mLargePager = (CalculatorViewPager)findViewById(R.id.largePanelswitch);

        if (mClearButton == null) {
            mClearButton = findViewById(R.id.clear);
            mClearButton.setOnClickListener(mListener);
            mClearButton.setOnLongClickListener(mListener);
        }
        if (mBackspaceButton == null) {
            mBackspaceButton = findViewById(R.id.del);
            mBackspaceButton.setOnClickListener(mListener);
            mBackspaceButton.setOnLongClickListener(mListener);
        }

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.history;

        mDisplay = (CalculatorDisplay)findViewById(R.id.display);

        mLogic = new Logic(this, mHistory, mDisplay);
        mLogic.setListener(this);
        if (mPersist.getMode() != null) {
            mLogic.mBaseModule.setMode(mPersist.getMode());
        }

        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mDisplay.getMaxDigits());

        mHistoryAdapter = new HistoryAdapter(this, mHistory);
        mHistory.setObserver(mHistoryAdapter);

        mPulldown = (Slider)findViewById(R.id.pulldown);
        mPulldown.setBarHeight(getResources().getDimensionPixelSize(R.dimen.history_bar_height));
        mPulldown.setSlideDirection(Direction.DOWN);
        if (CalculatorSettings.clickToOpenHistory(this)) {
            mPulldown.enableClick(true);
            mPulldown.enableTouch(false);
        }
        mPulldown.setBackgroundResource(R.color.background);
        mHistoryView = (ListView)mPulldown.findViewById(R.id.history);
        setUpHistory();

        mGraph = new Graph(mLogic);

        if (mPager != null) {
            mPager.setAdapter(new PageAdapter(mPager, mListener, mGraph, mLogic));
            mPager.setCurrentItem(state == null ? Panel.BASIC.getOrder() : state.getInt(STATE_CURRENT_VIEW, Panel.BASIC.getOrder()));
            mPager.setOnPageChangeListener(this);
            runCling(false);
            mListener.setHandler(this, mLogic, mPager);
        } else if (mSmallPager != null && mLargePager != null) {
            // Expanded UI
            mSmallPager.setAdapter(new SmallPageAdapter(mSmallPager, mLogic));
            mLargePager.setAdapter(new LargePageAdapter(mLargePager, mGraph, mLogic));
            mSmallPager.setCurrentItem(state == null ? SmallPanel.ADVANCED.getOrder() : state.getInt(STATE_CURRENT_VIEW_SMALL, SmallPanel.ADVANCED.getOrder()));
            mLargePager.setCurrentItem(state == null ? LargePanel.BASIC.getOrder() : state.getInt(STATE_CURRENT_VIEW_LARGE, LargePanel.BASIC.getOrder()));
            mSmallPager.setOnPageChangeListener(this);
            mLargePager.setOnPageChangeListener(this);
            runCling(false);
            mListener.setHandler(this, mLogic, mSmallPager, mLargePager);
        }

        mDisplay.setOnKeyListener(mListener);

        if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
            createFakeMenu();
        }

        mLogic.resumeWithHistory();
        updateDeleteMode();

        mPulldown.bringToFront();

        if (!isUpdatedPolicyInUse()) {
            showPolicyUpdateDialog();
        }
    }

    private void updateDeleteMode() {
        if (mLogic.getDeleteMode() == Logic.DELETE_MODE_BACKSPACE) {
            mClearButton.setVisibility(View.GONE);
            mBackspaceButton.setVisibility(View.VISIBLE);
        } else {
            mClearButton.setVisibility(View.VISIBLE);
            mBackspaceButton.setVisibility(View.GONE);
        }
    }

    private boolean isUpdatedPolicyInUse() {
        SharedPreferences prefs = getSharedPreferences("PREFS", Context.MODE_PRIVATE);
        int version = prefs.getInt(PREFS_KEY_VERSION, 1);
        return CURRENT_POLICY_VERSION == version;
    }

    /**
     * Show Policy update dialog
     */
    private void showPolicyUpdateDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
        alert.setCancelable(false);
        alert.setTitle(R.string.cling_simple_title);

        String fileName = "file:///android_asset/legal/privacy_policy.html";

        WebView webView = new WebView(this);
        webView.setWebViewClient(new LegalPolicyWebViewclient(this));
        WebSettings webSettings = webView.getSettings();
        webSettings.setDefaultFontSize(13);
        webSettings.setJavaScriptEnabled(false);
        webView.loadUrl(fileName);

        webView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                // Disabling copy-paste for long click
                return true;
            }
        });
        webView.setLongClickable(false);
        alert.setView(webView);
        alert.setIcon(R.mipmap.ic_launcher_calculator);
        alert.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                SharedPreferences prefs = getSharedPreferences("PREFS", Context.MODE_PRIVATE);
                Editor editor = prefs.edit();
                editor.putInt(PREFS_KEY_VERSION, CURRENT_POLICY_VERSION);
                editor.commit();
                dialog.dismiss();
            }
        });
        alert.setNegativeButton("Decline", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                onBackPressed();
            }
        });
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem mClearHistory = menu.findItem(R.id.clear_history);
        mClearHistory.setVisible(mPulldown.isSliderOpen());

        MenuItem mShowHistory = menu.findItem(R.id.show_history);
        mShowHistory.setVisible(!mPulldown.isSliderOpen());

        MenuItem mHideHistory = menu.findItem(R.id.hide_history);
        mHideHistory.setVisible(mPulldown.isSliderOpen());

        MenuItem mMatrixPanel = menu.findItem(R.id.matrix);
        if (mMatrixPanel != null) {
            mMatrixPanel.setVisible(!getMatrixVisibility() && CalculatorSettings.matrixPanel(getContext()) && !mPulldown.isSliderOpen());
        }

        MenuItem mGraphPanel = menu.findItem(R.id.graph);
        if (mGraphPanel != null) {
            mGraphPanel.setVisible(!getGraphVisibility() && CalculatorSettings.graphPanel(getContext()) && !mPulldown.isSliderOpen());
        }

        MenuItem mFunctionPanel = menu.findItem(R.id.function);
        if (mFunctionPanel != null) {
            mFunctionPanel.setVisible(!getFunctionVisibility() && CalculatorSettings.functionPanel(getContext()) && !mPulldown.isSliderOpen());
        }

        MenuItem mBasicPanel = menu.findItem(R.id.basic);
        if (mBasicPanel != null) {
            mBasicPanel.setVisible(!getBasicVisibility() && CalculatorSettings.basicPanel(getContext()) && !mPulldown.isSliderOpen());
        }

        MenuItem mAdvancedPanel = menu.findItem(R.id.advanced);
        if (mAdvancedPanel != null) {
            mAdvancedPanel.setVisible(!getAdvancedVisibility() && CalculatorSettings.advancedPanel(getContext()) && !mPulldown.isSliderOpen());
        }

        MenuItem mHexPanel = menu.findItem(R.id.hex);
        if (mHexPanel != null) {
            mHexPanel.setVisible(!getHexVisibility() && CalculatorSettings.hexPanel(getContext()) && !mPulldown.isSliderOpen());
        }

        MenuItem mLock = menu.findItem(R.id.lock);
        if (mLock != null) {
            mLock.setVisible(getGraphVisibility() && getPagingEnabled());
        }

        MenuItem mUnlock = menu.findItem(R.id.unlock);
        if (mUnlock != null) {
            mUnlock.setVisible(getGraphVisibility() && !getPagingEnabled());
        }

        return true;
    }

    private void createFakeMenu() {
        mOverflowMenuButton = findViewById(R.id.overflow_menu);
        if (mOverflowMenuButton != null) {
            mOverflowMenuButton.setVisibility(View.VISIBLE);
            mOverflowMenuButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.overflow_menu:
                PopupMenu menu = constructPopupMenu();
                if (menu != null) {
                    menu.show();
                }
                break;
        }
    }

    private PopupMenu constructPopupMenu() {
        final PopupMenu popupMenu = new PopupMenu(this, mOverflowMenuButton);
        final Menu menu = popupMenu.getMenu();
        popupMenu.inflate(R.menu.menu);
        popupMenu.setOnMenuItemClickListener(this);
        onPrepareOptionsMenu(menu);
        return popupMenu;
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        return onOptionsItemSelected(item);
    }

    private boolean getGraphVisibility() {
        if (mPager != null) {
            return mPager.getCurrentItem() == Panel.GRAPH.getOrder() && CalculatorSettings.graphPanel(getContext());
        } else if (mLargePager != null) {
            return mLargePager.getCurrentItem() == LargePanel.GRAPH.getOrder() && CalculatorSettings.graphPanel(getContext());
        }
        return false;
    }

    private boolean getFunctionVisibility() {
        // if(mPager != null) {
        // return mPager.getCurrentItem() == Panel.FUNCTION.getOrder() &&
        // CalculatorSettings.functionPanel(getContext());
        // }
        // else if(mSmallPager != null) {
        // return mSmallPager.getCurrentItem() == SmallPanel.FUNCTION.getOrder()
        // && CalculatorSettings.functionPanel(getContext());
        // }
        return false;
    }

    private boolean getBasicVisibility() {
        if (mPager != null) {
            return mPager.getCurrentItem() == Panel.BASIC.getOrder() && CalculatorSettings.basicPanel(getContext());
        } else if (mLargePager != null) {
            return mLargePager.getCurrentItem() == LargePanel.BASIC.getOrder() && CalculatorSettings.basicPanel(getContext());
        }
        return false;
    }

    private boolean getAdvancedVisibility() {
        if (mPager != null) {
            return mPager.getCurrentItem() == Panel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(getContext());
        } else if (mSmallPager != null) {
            return mSmallPager.getCurrentItem() == SmallPanel.ADVANCED.getOrder() && CalculatorSettings.advancedPanel(getContext());
        }
        return false;
    }

    private boolean getHexVisibility() {
        if (mPager != null) {
            return mPager.getCurrentItem() == Panel.HEX.getOrder() && CalculatorSettings.hexPanel(getContext());
        } else if (mSmallPager != null) {
            return mSmallPager.getCurrentItem() == SmallPanel.HEX.getOrder() && CalculatorSettings.hexPanel(getContext());
        }
        return false;
    }

    private boolean getMatrixVisibility() {
        if (mPager != null) {
            return mPager.getCurrentItem() == Panel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(getContext());
        } else if (mLargePager != null) {
            return mLargePager.getCurrentItem() == LargePanel.MATRIX.getOrder() && CalculatorSettings.matrixPanel(getContext());
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_history:
                mHistory.clear();
                mLogic.onClear();
                mHistoryAdapter.notifyDataSetInvalidated();
                break;

            case R.id.show_history:
                mPulldown.animateSliderOpen();
                break;

            case R.id.hide_history:
                mPulldown.animateSliderClosed();
                break;

            case R.id.basic:
                if (!getBasicVisibility()) {
                    if (mPager != null) {
                        mPager.setCurrentItem(Panel.BASIC.getOrder());
                    } else if (mLargePager != null) {
                        mLargePager.setCurrentItem(LargePanel.BASIC.getOrder());
                    }
                }
                break;

            case R.id.advanced:
                if (!getAdvancedVisibility()) {
                    if (mPager != null) {
                        mPager.setCurrentItem(Panel.ADVANCED.getOrder());
                    } else if (mSmallPager != null) {
                        mSmallPager.setCurrentItem(SmallPanel.ADVANCED.getOrder());
                    }
                }
                break;

            case R.id.function:
                if (!getFunctionVisibility()) {
                    if (mPager != null) {
                        mPager.setCurrentItem(Panel.FUNCTION.getOrder());
                    } else if (mSmallPager != null) {
                        mSmallPager.setCurrentItem(SmallPanel.FUNCTION.getOrder());
                    }
                }
                break;

            case R.id.graph:
                if (!getGraphVisibility()) {
                    if (mPager != null) {
                        mPager.setCurrentItem(Panel.GRAPH.getOrder());
                    } else if (mLargePager != null) {
                        mLargePager.setCurrentItem(LargePanel.GRAPH.getOrder());
                    }
                }
                break;

            case R.id.matrix:
                if (!getMatrixVisibility()) {
                    if (mPager != null) {
                        mPager.setCurrentItem(Panel.MATRIX.getOrder());
                    } else if (mLargePager != null) {
                        mLargePager.setCurrentItem(LargePanel.MATRIX.getOrder());
                    }
                }
                break;

            case R.id.hex:
                if (!getHexVisibility()) {
                    if (mPager != null) {
                        mPager.setCurrentItem(Panel.HEX.getOrder());
                    } else if (mSmallPager != null) {
                        mSmallPager.setCurrentItem(SmallPanel.HEX.getOrder());
                    }
                }
                break;

            case R.id.lock:
                setPagingEnabled(false);
                break;

            case R.id.unlock:
                setPagingEnabled(true);
                break;

            case R.id.settings:
                Intent intent = new Intent(this, Preferences.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(final Bundle state) {
        super.onSaveInstanceState(state);

        if (mPager != null) {
            state.putInt(STATE_CURRENT_VIEW, mPager.getCurrentItem());
        }

        if (mSmallPager != null) {
            state.putInt(STATE_CURRENT_VIEW_SMALL, mSmallPager.getCurrentItem());
        }

        if (mLargePager != null) {
            state.putInt(STATE_CURRENT_VIEW_LARGE, mLargePager.getCurrentItem());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mLogic.updateHistory();
        mPersist.setDeleteMode(mLogic.getDeleteMode());
        mPersist.setMode(mLogic.mBaseModule.getMode());
        mPersist.save();
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mPulldown.isSliderOpen() && !clingActive) {
            mPulldown.animateSliderClosed();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK && mPager != null && !getBasicVisibility() && CalculatorSettings.basicPanel(getContext()) && !clingActive) {
            mPager.setCurrentItem(Panel.BASIC.getOrder());
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK && mSmallPager != null && mLargePager != null && !(getAdvancedVisibility() && getBasicVisibility())
                && CalculatorSettings.basicPanel(getContext()) && CalculatorSettings.advancedPanel(getContext()) && !clingActive) {
            mSmallPager.setCurrentItem(SmallPanel.ADVANCED.getOrder());
            mLargePager.setCurrentItem(LargePanel.BASIC.getOrder());
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    @Override
    public void onDeleteModeChange() {
        updateDeleteMode();
    }

    private void setUpHistory() {
        registerForContextMenu(mHistoryView);
        mHistoryView.setAdapter(mHistoryAdapter);
        mHistoryView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        mHistoryView.setStackFromBottom(true);
        mHistoryView.setFocusable(false);
        mHistoryView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                int deleteMode = mLogic.getDeleteMode();
                if (mDisplay.getText().isEmpty()) {
                    deleteMode = Logic.DELETE_MODE_CLEAR;
                }
                mDisplay.insert(((HistoryLine)view).getHistoryEntry().getEdited());
                mLogic.setDeleteMode(deleteMode);
            }
        });
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        View history = mHistoryAdapter.getView(info.position, null, null);
        if (history instanceof HistoryLine) {
            ((HistoryLine)history).onCreateContextMenu(menu);
        }
    }

    private Context getContext() {
        return Calculator.this;
    }

    /* Cling related */
    private boolean isClingsEnabled() {
        // disable clings when running in a test harness
        if (ActivityManager.isRunningInTestHarness()) {
            return false;
        }
        return true;
    }

    private Cling initCling(final int clingId, final int[] positionData, final float revealRadius, final boolean showHand, final boolean animate) {
        setPagingEnabled(false);
        clingActive = true;

        Cling cling = (Cling)findViewById(clingId);
        if (cling != null) {
            cling.init(this, positionData, revealRadius, showHand);
            cling.setVisibility(View.VISIBLE);
            cling.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            if (animate) {
                cling.buildLayer();
                cling.setAlpha(0f);
                cling.animate().alpha(1f).setInterpolator(new AccelerateInterpolator()).setDuration(Cling.SHOW_CLING_DURATION).setStartDelay(0).start();
            } else {
                cling.setAlpha(1f);
            }
        }
        return cling;
    }

    private void dismissCling(final Cling cling, final String flag, final int duration) {
        setPagingEnabled(true);
        clingActive = false;

        if (cling != null) {
            cling.dismiss();
            ObjectAnimator anim = ObjectAnimator.ofFloat(cling, "alpha", 0f);
            anim.setDuration(duration);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    cling.setVisibility(View.GONE);
                    cling.cleanup();
                    CalculatorSettings.saveKey(getContext(), flag, true);
                };
            });
            anim.start();
        }
    }

    private void removeCling(final int id) {
        setPagingEnabled(true);
        clingActive = false;

        final View cling = findViewById(id);
        if (cling != null) {
            final ViewGroup parent = (ViewGroup)cling.getParent();
            parent.post(new Runnable() {
                @Override
                public void run() {
                    parent.removeView(cling);
                }
            });
        }
    }

    public void showFirstRunSimpleCling(final boolean animate) {
        // Enable the clings only if they have not been dismissed before
        if (isClingsEnabled() && !CalculatorSettings.isDismissed(getContext(), Cling.SIMPLE_CLING_DISMISSED_KEY)) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int[] location = new int[3];
            location[0] = 0;
            location[1] = size.y / 2;
            location[2] = 10;
            initCling(R.id.simple_cling, location, 0, true, animate);
        } else {
            removeCling(R.id.simple_cling);
        }
    }

    public void showFirstRunMatrixCling(final boolean animate) {
        // Enable the clings only if they have not been dismissed before
        if (isClingsEnabled() && !CalculatorSettings.isDismissed(getContext(), Cling.MATRIX_CLING_DISMISSED_KEY)) {
            View v;
            if (mPager != null) {
                v = ((PageAdapter)mPager.getAdapter()).mMatrixPage.findViewById(R.id.matrix);
            } else if (mLargePager != null) {
                v = ((LargePageAdapter)mLargePager.getAdapter()).mMatrixPage.findViewById(R.id.matrix);
            } else {
                v = null;
            }
            int[] location = new int[3];

            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    mListener.onClick(v);
                    dismissMatrixCling(v);
                    v.setOnClickListener(mListener);
                }
            });

            v.getLocationOnScreen(location);
            location[0] = location[0] + v.getWidth() / 2;
            location[1] = location[1] + v.getHeight() / 2;
            location[2] = -1;
            initCling(R.id.matrix_cling, location, v.getWidth() / 2, false, animate);
        } else {
            removeCling(R.id.matrix_cling);
        }
    }

    public void showFirstRunHexCling(final boolean animate) {
        // Enable the clings only if they have not been dismissed before
        if (isClingsEnabled() && !CalculatorSettings.isDismissed(getContext(), Cling.HEX_CLING_DISMISSED_KEY)) {
            initCling(R.id.hex_cling, null, 0, false, animate);
        } else {
            removeCling(R.id.hex_cling);
        }
    }

    public void showFirstRunGraphCling(final boolean animate) {
        // Enable the clings only if they have not been dismissed before
        if (isClingsEnabled() && !CalculatorSettings.isDismissed(getContext(), Cling.GRAPH_CLING_DISMISSED_KEY)) {
            initCling(R.id.graph_cling, null, 0, false, animate);
        } else {
            removeCling(R.id.graph_cling);
        }
    }

    public void dismissSimpleCling(final View v) {
        Cling cling = (Cling)findViewById(R.id.simple_cling);
        dismissCling(cling, Cling.SIMPLE_CLING_DISMISSED_KEY, Cling.DISMISS_CLING_DURATION);
    }

    public void dismissMatrixCling(final View v) {
        Cling cling = (Cling)findViewById(R.id.matrix_cling);
        dismissCling(cling, Cling.MATRIX_CLING_DISMISSED_KEY, Cling.DISMISS_CLING_DURATION);
    }

    public void dismissHexCling(final View v) {
        Cling cling = (Cling)findViewById(R.id.hex_cling);
        dismissCling(cling, Cling.HEX_CLING_DISMISSED_KEY, Cling.DISMISS_CLING_DURATION);
    }

    public void dismissGraphCling(final View v) {
        Cling cling = (Cling)findViewById(R.id.graph_cling);
        dismissCling(cling, Cling.GRAPH_CLING_DISMISSED_KEY, Cling.DISMISS_CLING_DURATION);
    }

    private void runCling(final boolean animate) {
        if (getBasicVisibility()) {
            showFirstRunSimpleCling(animate);
        }
        if (getMatrixVisibility()) {
            showFirstRunMatrixCling(animate);
        }
        if (getHexVisibility()) {
            showFirstRunHexCling(animate);
        }
        if (getGraphVisibility()) {
            showFirstRunGraphCling(animate);
        }
    }

    private void setPagingEnabled(final boolean enabled) {
        if (mPager != null) {
            mPager.setPagingEnabled(enabled);
        }
        if (mSmallPager != null) {
            mSmallPager.setPagingEnabled(enabled);
        }
        if (mLargePager != null) {
            mLargePager.setPagingEnabled(enabled);
        }
    }

    private boolean getPagingEnabled() {
        if (mPager != null) {
            return mPager.getPagingEnabled();
        }
        if (mSmallPager != null) {
            return mSmallPager.getPagingEnabled();
        }
        if (mLargePager != null) {
            return mLargePager.getPagingEnabled();
        }
        return true;
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
        if (state == 0) {
            setPagingEnabled(true);
            runCling(true);
        }
    }

    @Override
    public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(final int position) {
    }
}
