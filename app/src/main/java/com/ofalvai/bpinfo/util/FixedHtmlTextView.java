package com.ofalvai.bpinfo.util;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import org.sufficientlysecure.htmltextview.ClickableTableSpan;
import org.sufficientlysecure.htmltextview.DrawTableLinkSpan;
import org.sufficientlysecure.htmltextview.HtmlLocalImageGetter;
import org.sufficientlysecure.htmltextview.HtmlRemoteImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTagHandler;
import org.sufficientlysecure.htmltextview.JellyBeanSpanFixTextView;
import org.sufficientlysecure.htmltextview.LocalLinkMovementMethod;

import java.io.InputStream;
import java.util.Scanner;

/**
 * Temporary fix of the htmltextview library's problem of text selection on Marshmallow and above.
 * The reason the command bubble not appearing is that HtmlTextView overrides onTouchEvent() and
 * basically always returns false. Technically, the mDontConsumeNonUrlClicks field is always set to
 * true, and there's no way to set to false other than copying this class over to our own package,
 * and modifying the field.
 */
public class FixedHtmlTextView extends JellyBeanSpanFixTextView {

    public static final String TAG = "HtmlTextView";
    public static final boolean DEBUG = false;
    boolean mDontConsumeNonUrlClicks = false;
    boolean mLinkHit;
    private boolean removeFromHtmlSpace = false;
    private ClickableTableSpan mClickableTableSpan;
    private DrawTableLinkSpan mDrawTableLinkSpan;

    public FixedHtmlTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public FixedHtmlTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedHtmlTextView(Context context) {
        super(context);
    }

    /**
     * Note that this must be called before setting text for it to work
     */
    public void setRemoveFromHtmlSpace(boolean removeFromHtmlSpace) {
        this.removeFromHtmlSpace = removeFromHtmlSpace;
    }

    public interface ImageGetter {
    }

    public static class LocalImageGetter implements org.sufficientlysecure.htmltextview.HtmlTextView.ImageGetter {
    }

    public static class RemoteImageGetter implements org.sufficientlysecure.htmltextview.HtmlTextView.ImageGetter {
        public String baseUrl;

        public RemoteImageGetter() {
        }

        public RemoteImageGetter(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    /**
     * http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
     */
    static private String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mLinkHit = false;
        boolean res = super.onTouchEvent(event);

        if (mDontConsumeNonUrlClicks) {
            return mLinkHit;
        }
        return res;
    }

    /**
     * Loads HTML from a raw resource, i.e., a HTML file in res/raw/.
     * This allows translatable resource (e.g., res/raw-de/ for german).
     * The containing HTML is parsed to Android's Spannable format and then displayed.
     *
     * @param context Context
     * @param resId   for example: R.raw.help
     */
    public void setHtmlFromRawResource(Context context, int resId, org.sufficientlysecure.htmltextview.HtmlTextView.ImageGetter imageGetter) {
        // load html from html file from /res/raw
        InputStream inputStreamText = context.getResources().openRawResource(resId);

        setHtmlFromString(convertStreamToString(inputStreamText), imageGetter);
    }

    /**
     * Parses String containing HTML to Android's Spannable format and displays it in this TextView.
     *
     * @param html String containing HTML, for example: "<b>Hello world!</b>"
     */
    public void setHtmlFromString(String html, org.sufficientlysecure.htmltextview.HtmlTextView.ImageGetter imageGetter) {
        Html.ImageGetter htmlImageGetter;
        if (imageGetter instanceof org.sufficientlysecure.htmltextview.HtmlTextView.LocalImageGetter) {
            htmlImageGetter = new HtmlLocalImageGetter(getContext());
        } else if (imageGetter instanceof org.sufficientlysecure.htmltextview.HtmlTextView.RemoteImageGetter) {
            htmlImageGetter = new HtmlRemoteImageGetter(this,
                    ((org.sufficientlysecure.htmltextview.HtmlTextView.RemoteImageGetter) imageGetter).baseUrl);
        } else {
            Log.e(TAG, "Wrong imageGetter!");
            return;
        }
        setHtmlFromStringWithHtmlImageGetter(html, htmlImageGetter);
    }

    /**
     * Parses String containing HTML to Android's Spannable format and displays it in this TextView.
     * Using the implementation of Html.ImageGetter provided. This allows using external libraries
     * for fetching and caching images.
     *
     * @param html String containing HTML, for example: "<b>Hello world!</b>"
     * @param htmlImageGetter for fetching images
     */
    public void setHtmlFromStringWithHtmlImageGetter(String html, Html.ImageGetter htmlImageGetter) {
        // this uses Android's Html class for basic parsing, and HtmlTagHandler
        final HtmlTagHandler htmlTagHandler = new HtmlTagHandler();
        htmlTagHandler.setClickableTableSpan(mClickableTableSpan);
        htmlTagHandler.setDrawTableLinkSpan(mDrawTableLinkSpan);
        if (removeFromHtmlSpace) {
            setText(removeHtmlBottomPadding(Html.fromHtml(html, htmlImageGetter, htmlTagHandler)));
        } else {
            setText(Html.fromHtml(html, htmlImageGetter, htmlTagHandler));
        }
        // make links work
        setMovementMethod(LocalLinkMovementMethod.getInstance());
    }

    /**
     * @deprecated
     */
    public void setHtmlFromRawResource(Context context, int resId, boolean useLocalDrawables) {
        if (useLocalDrawables) {
            setHtmlFromRawResource(context, resId, new org.sufficientlysecure.htmltextview.HtmlTextView.LocalImageGetter());
        } else {
            setHtmlFromRawResource(context, resId, new org.sufficientlysecure.htmltextview.HtmlTextView.RemoteImageGetter());
        }
    }

    /**
     * @deprecated
     */
    public void setHtmlFromString(String html, boolean useLocalDrawables) {
        if (useLocalDrawables) {
            setHtmlFromString(html, new org.sufficientlysecure.htmltextview.HtmlTextView.LocalImageGetter());
        } else {
            setHtmlFromString(html, new org.sufficientlysecure.htmltextview.HtmlTextView.RemoteImageGetter());
        }
    }

    public void setClickableTableSpan(ClickableTableSpan clickableTableSpan) {
        this.mClickableTableSpan = clickableTableSpan;
    }

    public void setDrawTableLinkSpan(DrawTableLinkSpan drawTableLinkSpan) {
        this.mDrawTableLinkSpan = drawTableLinkSpan;
    }

    private CharSequence removeHtmlBottomPadding(CharSequence text) {
        if (text == null) {
            return null;
        } else if (text.length() == 0) {
            return text;
        }

        while (text.charAt(text.length() - 1) == '\n') {
            text = text.subSequence(0, text.length() - 1);
        }
        return text;
    }
}
