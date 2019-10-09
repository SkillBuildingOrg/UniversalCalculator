/**
 * 
 */
package com.fusionleap.android.calculator.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;


/**
 * @author joshiroh
 *
 */
public final class LegalPolicyWebViewclient extends WebViewClient {
    private Context context;

    /**
     * Default constructor
     * @param context
     */
    public LegalPolicyWebViewclient(final Context context) {
        this.context = context;
    }

    @Override
    public final boolean shouldOverrideUrlLoading(final WebView view, final String type) {
        if (type.startsWith("tel:")) {
            final Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse(type));
            context.startActivity(callIntent);
        } else {
            final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(type));
            context.startActivity(browserIntent);
        }
        return true;
    }
}
