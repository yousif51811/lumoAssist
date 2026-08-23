package dev.yousif51811.lumoassist;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

public class OptionsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.options);

        Switch autoInject = findViewById(R.id.switch_auto_inject);
    }
}
