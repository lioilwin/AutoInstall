package win.lioil.autoInstall;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

/**
 * 辅助服务自动安装APP，该服务在单独进程中允许
 */
public class AutoInstallService extends AccessibilityService {
    private static final String TAG = AutoInstallService.class.getSimpleName();
    private static final int DELAY_PAGE = 320; // 页面切换时间
    private final Handler mHandler = new Handler();

    @Override
    protected void onServiceConnected() {
        Log.i(TAG, "onServiceConnected: ");
        Toast.makeText(this, getString(R.string.aby_label) + "开启了", Toast.LENGTH_LONG).show();
        // 服务开启，模拟两次返回键，退出系统设置界面（实际上还应该检查当前UI是否为系统设置界面，但一想到有些厂商可能篡改设置界面，懒得适配了...）
        performGlobalAction(GLOBAL_ACTION_BACK);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        }, DELAY_PAGE);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        Toast.makeText(this, getString(R.string.aby_label) + "停止了，请重新开启", Toast.LENGTH_LONG).show();
        // 服务停止，重新进入系统设置界面
        AccessibilityUtil.jumpToSetting(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || !event.getPackageName().toString()
                .contains("packageinstaller"))//不写完整包名，是因为某些手机(如小米)安装器包名是自定义的
            return;
        /*
        某些手机安装页事件返回节点有可能为null，无法获取安装按钮
        例如华为mate10安装页就会出现event.getSource()为null，所以取巧改变当前页面状态，重新获取节点。
        该方法在华为mate10上生效，但其它手机没有验证...(目前小米手机没有出现这个问题)
        */
        Log.i(TAG, "onAccessibilityEvent: " + event);
        AccessibilityNodeInfo eventNode = event.getSource();
        if (eventNode == null) {
            Log.i(TAG, "eventNode: null, 重新获取eventNode...");
            performGlobalAction(GLOBAL_ACTION_RECENTS); // 打开最近页面
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    performGlobalAction(GLOBAL_ACTION_BACK); // 返回安装页面
                }
            }, DELAY_PAGE);
            return;
        }

        /*
        模拟点击->自动安装，只验证了小米5s plus(MIUI 9.8.4.26)、小米Redmi 5A(MIUI 9.2)、华为mate 10
        其它品牌手机可能还要适配，适配最可恶的就是出现安装广告按钮，误点安装其它垃圾APP（典型就是小米安装后广告推荐按钮，华为安装开始官方安装）
        */
        AccessibilityNodeInfo rootNode = getRootInActiveWindow(); //当前窗口根节点
        if (rootNode == null)
            return;
        Log.i(TAG, "rootNode: " + rootNode);
        if (isNotAD(rootNode))
            findTxtClick(rootNode, "安装"); //一起执行：安装->下一步->打开,以防意外漏掉节点
        findTxtClick(rootNode, "继续安装");
        findTxtClick(rootNode, "下一步");
        findTxtClick(rootNode, "打开");
        // 回收节点实例来重用
        eventNode.recycle();
        rootNode.recycle();
    }

	// 查找安装,并模拟点击(findAccessibilityNodeInfosByText判断逻辑是contains而非equals)
    private void findTxtClick(AccessibilityNodeInfo nodeInfo, String txt) {
        List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByText(txt);
        if (nodes == null || nodes.isEmpty())
            return;
        Log.i(TAG, "findTxtClick: " + txt + ", " + nodes.size() + ", " + nodes);
        for (AccessibilityNodeInfo node : nodes) {
            if (node.isEnabled() && node.isClickable() && (node.getClassName().equals("android.widget.Button")
                    || node.getClassName().equals("android.widget.CheckBox") // 兼容华为安装界面的复选框
            )) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    // 排除广告[安装]按钮
    private boolean isNotAD(AccessibilityNodeInfo rootNode) {
        return isNotFind(rootNode, "还喜欢") //小米
                && isNotFind(rootNode, "官方安装"); //华为
    }

    private boolean isNotFind(AccessibilityNodeInfo rootNode, String txt) {
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(txt);
        return nodes == null || nodes.isEmpty();
    }
	
	@Override
    public void onInterrupt() {
    }
}