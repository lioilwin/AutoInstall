package win.lioil.autoInstall;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.io.File;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEditText = findViewById(R.id.apk_path);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1); // 动态申请读取权限
        InstallUtil.checkSetting(this); // "未知来源"设置
        AccessibilityUtil.checkSetting(MainActivity.this, AutoInstallService.class); // "辅助功能"设置
    }

    public void start(View view) {
        InstallUtil.install(MainActivity.this, new File(mEditText.getText().toString()));
    }
}