package com.htc.datausagemonitor.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.util.TypedValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.htc.datausagemonitor.R;


/**
 * Created by Bill on 2017/8/29.
 */

public class DisclaimerDialogFragment extends DialogFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.warning_dialog_fragment,null);
        TextView title = (TextView)view.findViewById(R.id.terms_title);
        TextView summary = (TextView)view.findViewById(R.id.content);
        TextView readtext =(TextView)view.findViewById(R.id.read_tips);
        Button cancalButton = (Button)view .findViewById(R.id.cancel);
        Button okButton =(Button)view.findViewById(R.id.ok);
        readtext.setVisibility(View.GONE);

        BufferedReader br = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.eula_zh)));
        String eulaStr = "";
        String buf;
        try {
            while ((buf = br.readLine()) != null) {
                eulaStr += buf + "\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        title.setText(getString(R.string.click_disclaimer));
        summary.setText(eulaStr);

/*        cancalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });*/
        cancalButton.setVisibility(View.GONE);

        okButton.setText(R.string.ok);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        return view;
    }
}
