
package com.fusionleap.android.calculator.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.fusionleap.calculator.R;

public class PreferencesFragment extends PreferenceFragment {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
        Preference about = findPreference("ABOUT");
        if (about != null) {
            String versionName = "";
            try {
                versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            about.setTitle(about.getTitle() + " " + versionName);

            about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    showPolicyUpdateDialog();
                    return true;
                }
            });
        }
    }

    /**
     * Show Policy update dialog
     */
    private void showPolicyUpdateDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
        alert.setCancelable(true);
        String fileName = "file:///android_asset/legal/privacy_policy.html";

        WebView webView = new WebView(getActivity());
        webView.setWebViewClient(new LegalPolicyWebViewclient(getActivity()));
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
        alert.setPositiveButton(R.string.cling_dismiss, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                dialog.dismiss();
            }
        });

        alert.show();
    }
}
