## AutoInstall 自动安装基本步骤
	
### 1.manifest添加辅助服务, res/xml配置辅助功能
	在AndroidManifest.xml中
	<application ...>
		<!--
		android:label(可选) 在辅助功能(无障碍)的系统设置会使用该名称,若不设置,则会使用<application android:label
		android:process(可选) 把该服务设在单独进程中,进程名以[冒号:]开头，是本应用的私有进程，其它应用无法访问
		android:permission(必需) 添加权限以确保只有系统可以绑定到该服务
		-->
		<service
			android:name=".AutoInstallService"
			android:label="@string/aby_label"
			android:process=":install"
			android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
			<intent-filter>
				<action android:name="android.accessibilityservice.AccessibilityService" />
			</intent-filter>
			
			<!--在xml文件配置辅助功能,也可在onServiceConnected()中使用setServiceInfo()动态配置-->
			<meta-data
				android:name="android.accessibilityservice"
				android:resource="@xml/accessibility_config" />
		</service>
	</application>
	
	在res/xml/accessibility_config中
	<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
		android:accessibilityEventTypes="typeAllMask"
		android:accessibilityFeedbackType="feedbackGeneric"
		android:accessibilityFlags="flagDefault"
		android:canRetrieveWindowContent="true"
		android:description="@string/aby_desc"
		android:notificationTimeout="100"/>		
    <!--android:packageNames="com.android.packageinstaller" 国内有不少手机都是自定义安装界面，packageNames不固定-->

		<!--
		android:description 辅助功能描述
		android:packageNames 指定辅助功能监听的包名，不指定表示监听所有应用
		android:accessibilityEventTypes 事件类型，typeAllMask表示接收所有事件
		android:accessibilityFlags 查找截点方式，一般配置为flagDefault默认方式
		android:accessibilityFeedbackType 操作按钮以后给用户的反馈类型，包括声音，震动等
		android:notificationTimeout 响应超时
		android:canRetrieveWindowContent 是否允许提取窗口的节点信息
		-->
		
	注意：[来源豌豆荚 http://www.infoq.com/cn/articles/android-accessibility-installing ]
		在一些使用虚拟键盘的APP中，经常会出现这样的逻辑
		Button button = (Button) findViewById(R.id.button);
		String num = (String) button.getText();
		在一般情况下，getText方法的返回值是Java.lang.String类的实例，上面这段代码可以正确运行。
		但是在开启Accessibility Service之后，如果没有指定 packageNames, 系统会对所有APP的UI都进行Accessible的处理。
		在这个例子中的表现就是getText方法的返回值变成了android.text.SpannableString类的实例
		（Java.lang.String和android.text.SpannableString都实现了java.lang.CharSequence接口），进而造成目标APP崩溃。
		所以强烈建议在注册Accessibility Service时指定目标APP的packageName，
		以减少手机上其他应用的莫名崩溃（代码中有这样的逻辑的各位，也请默默的改为调用toString()方法吧）。
	
### 2.继承服务AccessibilityService,实现自动安装
	public class AutoInstallService extends AccessibilityService {
		private static final String TAG = AutoInstallService.class.getSimpleName();
		private static final int DELAY_PAGE = 320; // 页面切换时间
		private final Handler mHandler = new Handler();
		
		......

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
	}
	
### 3.退出"辅助功能/无障碍"设置
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
		
		......
	}
	
### 4.开启"辅助功能/无障碍"设置
	public class AccessibilityUtil {
		......
		/**
		 * 检查系统设置：是否开启辅助服务
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
	}
	
### 5.允许"未知来源"设置
	public class AutoInstallUtil {
		......

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
		 * @param apkPath APK文件的本地路径
		 */
		public static void install(Context cxt, String apkPath) {
			try {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				// Android高版本安装器不允许直接访问File，需要借助FileProvider(或使用取巧方法：调低targetSdkVersion)
				intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
				cxt.startActivity(intent);
			} catch (Throwable e) {
				Log.e(TAG, "install: " + e.getMessage());
			}
		}
	}