package win.lioil.autoInstall;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

/**
 * 辅助功能/无障碍相关工具
 */
public class AccessibilityUtil {
    private static final String TAG = AccessibilityUtil.class.getSimpleName();

    /**
     * 检查系统设置，并显示设置对话框
     *
     * @param service 辅助服务
     */
    public static void checkSetting(final Context cxt, Class service) {
        if (isSettingOpen(service, cxt))
            return;
        new AlertDialog.Builder(cxt)
                .setTitle(R.string.aby_setting_title)
                .setMessage(R.string.aby_setting_msg)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        jumpToSetting(cxt);
                    }
                })
                .show();
    }

    /**
     * 检查系统设置：是否开启辅助服务
     *
     * @param service 辅助服务
     */
    private static boolean isSettingOpen(Class service, Context cxt) {
        try {
            int enable = Settings.Secure.getInt(cxt.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            if (enable != 1)
                return false;
            String services = Settings.Secure.getString(cxt.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (!TextUtils.isEmpty(services)) {
                TextUtils.SimpleStringSplitter split = new TextUtils.SimpleStringSplitter(':');
                split.setString(services);
                while (split.hasNext()) { // 遍历所有已开启的辅助服务名
                    if (split.next().equalsIgnoreCase(cxt.getPackageName() + "/" + service.getName()))
                        return true;
                }
            }
        } catch (Throwable e) {//若出现异常，则说明该手机设置被厂商篡改了,需要适配
            Log.e(TAG, "isSettingOpen: " + e.getMessage());
        }
        return false;
    }

    /**
     * 跳转到系统设置：开启辅助服务
     */
    public static void jumpToSetting(final Context cxt) {
        try {
            cxt.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Throwable e) {//若出现异常，则说明该手机设置被厂商篡改了,需要适配
            try {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                cxt.startActivity(intent);
            } catch (Throwable e2) {
                Log.e(TAG, "jumpToSetting: " + e2.getMessage());
            }
        }
    }

    /**
     * 唤醒点亮和解锁屏幕(60s)
     */
    public static void wakeUpScreen(Context context) {
        try {
            //唤醒点亮屏幕
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isScreenOn()) {
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "wakeUpScreen");
                wl.acquire(60000); // 60s后释放锁
            }

            //解锁屏幕
            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && km.inKeyguardRestrictedInputMode()) {
                KeyguardManager.KeyguardLock kl = km.newKeyguardLock("unLock");
                kl.disableKeyguard();
            }
        } catch (Throwable e) {
            Log.e(TAG, "wakeUpScreen: " + e.getMessage());
        }
    }
}