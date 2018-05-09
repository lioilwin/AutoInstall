package win.lioil.autoInstall;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import java.io.File;

public class AutoInstallUtil {
    private static final String TAG = AutoInstallUtil.class.getSimpleName();

    /**
     * 检查系统设置，并显示设置对话框
     */
    public static void checkSetting(final Context cxt) {
        if (isSettingOpen(cxt))
            return;
        new AlertDialog.Builder(cxt)
                .setTitle("请设置")
                .setMessage("在安全中，找到并勾选\"未知来源\"")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        jumpToSetting(cxt);
                    }
                })
                .show();
    }

    /**
     * 检查系统设置：是否允许安装来自未知来源的应用
     */
    private static boolean isSettingOpen(Context cxt) {
        return Settings.Secure.getInt(cxt.getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1;
    }

    /**
     * 跳转到系统设置：允许安装来自未知来源的应用
     */
    private static void jumpToSetting(Context cxt) {
        cxt.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
    }

    /**
     * 安装APK
     *
     * @param apkPath APK文件的本地路径
     */
    public static void install(Context cxt, String apkPath) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
            cxt.startActivity(intent);
        } catch (Throwable e) {
            Log.e(TAG, "install: " + e.getMessage());
        }
    }
}