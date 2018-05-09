package win.lioil.autoInstall;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEditText = findViewById(R.id.apk_path);
        AutoInstallUtil.checkSetting(this); //"未知来源"设置
//        AccessibilityUtil.checkSetting(MainActivity.this, AutoInstallService.class); //"辅助功能"设置
    }

    public void start(View view) {
        AutoInstallUtil.install(this, mEditText.getText().toString());
    }



}