package edu.bit.software_theory_camera;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

public class ResultDialog extends Dialog {
    public ResultDialog(Context context) {
        super(context);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_dialog);
        TextView textView=findViewById(R.id.result);

    }


}