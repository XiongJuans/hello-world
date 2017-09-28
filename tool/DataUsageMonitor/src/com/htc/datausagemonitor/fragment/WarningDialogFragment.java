package com.htc.datausagemonitor.fragment;

import android.app.Dialog;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.htc.datausagemonitor.R;

/**
 * Created by Bill on 2017/8/2.
 */

public class WarningDialogFragment extends DialogFragment {

    private WarningDialogDismissListener listener;
    private boolean needFinishActivity;
    private TextView summary;
    private TextView readtext;
    private Button okButton;
    private Button cancalButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.warning_dialog_fragment,null);

        summary = (TextView)rootview.findViewById(R.id.content);
        readtext =(TextView)rootview.findViewById(R.id.read_tips);
        cancalButton = (Button)rootview .findViewById(R.id.cancel);
        okButton =(Button)rootview.findViewById(R.id.ok);
        Spannable summaryTips = new SpannableString(summary.getText());
        Spannable readTips = new SpannableString(readtext.getText());

        ClickableSpan tenPrivacyClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                String url = getString(R.string.tenprivacy_url);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        };

        ClickableSpan privacyClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                String url = getString(R.string.privacy_url);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        };

        ClickableSpan disclaimerClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                getFragmentManager().beginTransaction().add(new DisclaimerDialogFragment(), "disclaimer").commit();
            }
        };
        int textLengthten = summary.getText().length();
        int tenprivacyLength = getString(R.string.tencent_privacy).length();
        int tenprivacyStart = summary.getText().toString().indexOf(getString(R.string.tencent_privacy));
        summaryTips.setSpan(tenPrivacyClickableSpan,tenprivacyStart,tenprivacyStart +tenprivacyLength,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        summary.setText(summaryTips);
        summary.setMovementMethod(LinkMovementMethod.getInstance());
        int textLength = readtext.getText().length();
//        ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE);
//        ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE);
        int privacyLength = getString(R.string.click_htc_privacy).length();
        int disclaimerLength = getString(R.string.click_disclaimer).length();

        int disclaimerStart = readtext.getText().toString().indexOf(getString(R.string.click_disclaimer));

        readTips.setSpan(disclaimerClickableSpan, disclaimerStart, disclaimerStart + disclaimerLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        readTips.setSpan(privacyClickableSpan, textLength - privacyLength, textLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        readTips.setSpan(colorSpan, textLength - privacyLength, textLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        readTips.setSpan(colorSpan, disclaimerStart, disclaimerStart + disclaimerLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        readtext.setText(readTips);
        readtext.setMovementMethod(LinkMovementMethod.getInstance());

        cancalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                needFinishActivity = true;
                listener.onDialogDismiss(needFinishActivity);
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                needFinishActivity = false;
                dismiss();
            }
        });

        setCancelable(false);

        return rootview;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        listener.onDialogDismiss(needFinishActivity);
    }

    public void setOnDismissListener(WarningDialogDismissListener listener) {
        this.listener = listener;
    }
}
