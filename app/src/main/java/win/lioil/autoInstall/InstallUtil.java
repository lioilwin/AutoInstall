package win.lioil.autoInstall;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import java.io.File;

/**
 * 安装相关工具
 */
public class InstallUtil {
    private static final String TAG = InstallUtil.class.getSimpleName();

    /**
     * 检查系统设置，并显示设置对话框
     */
    public static void checkSetting(final Context cxt) {
        if (isSettingOpen(cxt))
            return;
        new AlertDialog.Builder(cxt)
                .setTitle(R.string.unknow_setting_title)
                .setMessage(R.string.unknow_setting_msg)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        jumpToInstallSetting(cxt);
                    }
                })
                .show();
    }

    /**
     * 检查系统设置：是否允许安装来自未知来源的应用
     */
    private static boolean isSettingOpen(Context cxt) {
        boolean canInstall;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) // Android 8.0
            canInstall = cxt.getPackageManager().canRequestPackageInstalls();
        else
            canInstall = Settings.Secure.getInt(cxt.getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1;
        return canInstall;
    }

    /**
     * 跳转到系统设置：允许安装来自未知来源的应用
     */
    private static void jumpToInstallSetting(Context cxt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) // Android 8.0
            cxt.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + cxt.getPackageName())));
        else
            cxt.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
    }

    /**
     * 安装APK
     *
     * @param apkFile APK文件的本地路径
     */
    public static void install(Context cxt, File apkFile) {
        AccessibilityUtil.wakeUpScreen(cxt); //唤醒屏幕,以便辅助功能模拟用户点击"安装"
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0以上不允许Uri包含File实际路径，需要借助FileProvider生成Uri（或者调低targetSdkVersion小于Android 7.0欺骗系统）
                uri = FileProvider.getUriForFile(cxt, cxt.getPackageName() + ".fileProvider", apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(apkFile);
            }
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            cxt.startActivity(intent);
        } catch (Throwable e) {
            Toast.makeText(cxt, "安装失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}