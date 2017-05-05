package cn.wl.softwareservice;

import java.util.List;
import com.android.CvbsActivity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class SoftwareService extends Service {

	private String TAG = "SoftwareService";
	private boolean DEBUG = true;

	private final static int MSG_DET_CVBS = 1;
	private final static int MSG_DET_CVBS_WHEN_FRIST = 2;
	private final static int MSG_DELAY_START_DET = 3;
	private final static int MSG_DELAY_BOOT_DVR = 4;
	private final static String DVR_SERVICE_NAME = "com.dvr.android.dvr.DVRBackService";
	private final static String DVR_PACKAGE_NAME = "com.dvr.android.dvr";
	private final static String DVR_MAIN_CLASSNAME = "com.dvr.android.dvr.DVRActivity";
	private final static String DVR_CVBS_CLASSNAME = "com.dvr.android.dvr.CVBSActivity";
	private final static String DVR_CVBS_ONLY_PACKAGE_NAME = "cn.wl.cvbscamonly";
	private final static String DVR_CVBS_ONLY_CLASSNAME = "cn.wl.cvbscamonly.CVBSActivity";
	private final static int DET_CVBS_TIME = 800;

	public final static String ACTION_CVBS_OUT = "action.zt.cvbs.out";
	public final static String ACTION_INTENT_CVBS_IN = "android.intent.cvbs.In";
	
	public CvbsActivity mCvbs = null;
	public long m_hCvbs;

	private boolean isCvbsIn = false;
	
	private ComponentName reComponentName;
	
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopDetCvbs();
	}

	@Override
	public IBinder onBind(Intent i) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		boolean _boot = intent.getBooleanExtra(Contants.KEY_IS_BOOT, false);
		if (_boot) {
			// 初始化后拉.
			initDetCvbs();
			// 1.判断有没有在倒车.
			boolean _isCvbsIn = (mCvbs.Reverse_Gear(m_hCvbs) == 0);
			// 2.1 如果在倒车,启动后拉界面.
			if (_isCvbsIn) {
				if (DEBUG)
					Log.d(TAG, " onStartCommand 2.1");
				doStartCVBSActivity();
				mHandler.sendEmptyMessageDelayed(MSG_DET_CVBS_WHEN_FRIST, 500);
			}
			// 2.2 没有倒车,开机启动DVR.
			else {
				if (DEBUG)
					Log.d(TAG, " onStartCommand 2.2");
				doBootDVR();
				mHandler.sendEmptyMessageDelayed(MSG_DELAY_START_DET, 500);
			}
			runInFg();
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void initDetCvbs() {
		// detect CVBS Caemra.
		mCvbs = CvbsActivity.init();
		m_hCvbs = mCvbs.openDevice();
	}

	public void startDetCvbs() {
		if (mCvbs == null) {
			initDetCvbs();
		}
		mHandler.removeMessages(MSG_DET_CVBS);
		mHandler.sendEmptyMessageDelayed(MSG_DET_CVBS, DET_CVBS_TIME);
	}

	public void stopDetCvbs() {
		mHandler.removeMessages(MSG_DET_CVBS);
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MSG_DET_CVBS:
				mHandler.removeMessages(MSG_DET_CVBS);
				doDetCvbs();
				mHandler.sendEmptyMessageDelayed(MSG_DET_CVBS, DET_CVBS_TIME);
				break;
			case MSG_DET_CVBS_WHEN_FRIST:
				mHandler.removeMessages(MSG_DET_CVBS_WHEN_FRIST);
				boolean _isCvbsIn = (mCvbs.Reverse_Gear(m_hCvbs) == 0);
				if (!_isCvbsIn) {
					sendBroadcast(new Intent(ACTION_CVBS_OUT));
					mHandler.sendEmptyMessageDelayed(MSG_DELAY_BOOT_DVR, 1000);
					mHandler.sendEmptyMessageDelayed(MSG_DELAY_START_DET, 1500);
				} else {
					mHandler.sendEmptyMessageDelayed(MSG_DET_CVBS_WHEN_FRIST,
							500);
				}
				break;
			case MSG_DELAY_START_DET:
				mHandler.removeMessages(MSG_DELAY_START_DET);
				startDetCvbs();
				break;
			case MSG_DELAY_BOOT_DVR:
				mHandler.removeMessages(MSG_DELAY_BOOT_DVR);
				doBootDVR();
				// doReActivity();
				break;
			default:
				break;
			}
		}
	};

	private void doDetCvbs() {
		boolean _isCvbsIn = (mCvbs.Reverse_Gear(m_hCvbs) == 0);
		if (isCvbsIn != _isCvbsIn) {
			isCvbsIn = _isCvbsIn;
			if (isCvbsIn) {
				boolean isRunning = isServiceRunning(SoftwareService.this,
						DVR_SERVICE_NAME);
				// getRunningReComponentName(SoftwareService.this);
				// 如果Service存在,发送广播由DVRService处理.
				if (isRunning) {
					if (DEBUG)
						Log.d(TAG, " with Service.");
					sendBroadcast(new Intent(ACTION_INTENT_CVBS_IN));
				}
				// 如果Service不存在,启动CVBSActivity.
				else {
					if (DEBUG)
						Log.d(TAG, " without Service.");
					doStartCVBSActivity();
				}
			} else {
				sendBroadcast(new Intent(ACTION_CVBS_OUT));
				// mHandler.sendEmptyMessageDelayed(MSG_DELAY_BOOT_DVR, 1000);
			}
		}
	}

	/**
	 * 开机启动DVR
	 */
	private void doBootDVR() {
		Intent dvrIntent = new Intent();
		dvrIntent.setComponent(new ComponentName(DVR_PACKAGE_NAME,
				DVR_MAIN_CLASSNAME));
		dvrIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		dvrIntent.putExtra(Contants.KEY_START_RECORD, true);
		try {
			startActivity(dvrIntent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void doReActivity() {
		if(reComponentName == null){
			return;
		}
		Intent _re = new Intent();
		_re.setComponent(reComponentName);
		_re.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		_re.putExtra(Contants.KEY_START_RECORD, true);
		try {
			startActivity(_re);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			reComponentName = null;
		}
	}

	/**
	 * start DVR_CVBS Activity.
	 */
	private void doStartCVBSActivity() {
		Intent i = new Intent();
		i.setComponent(new ComponentName(DVR_PACKAGE_NAME, DVR_CVBS_CLASSNAME));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if (checkApkExist(this, i)) {
			startActivity(i);
		} else {
			i.setComponent(new ComponentName(DVR_CVBS_ONLY_PACKAGE_NAME,
					DVR_CVBS_ONLY_CLASSNAME));
			try {
				startActivity(i);
			} catch (ActivityNotFoundException e) {

			}
		}
	}

	public boolean checkApkExist(Context context, Intent intent) {
		List<ResolveInfo> list = context.getPackageManager()
				.queryIntentActivities(intent, 0);
		if (list.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isServiceRunning(Context context, String serviceName) {
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> runningServiceInfos = am
				.getRunningServices(40);
		if (runningServiceInfos.size() <= 0) {
			return false;
		}
		for (ActivityManager.RunningServiceInfo serviceInfo : runningServiceInfos) {
			if (serviceInfo.service.getClassName().equals(serviceName)) {
				return true;
			}
		}
		return false;
	}

	private void getRunningReComponentName(Context context) {
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		String runningActivity = activityManager.getRunningTasks(1).get(0).topActivity
				.getClassName();
		String runningActivityPck = activityManager.getRunningTasks(1).get(0).topActivity
				.getPackageName();
		if(runningActivity.equals("com.dvr.android.dvr.CVBSActivity"))return;
		reComponentName = new ComponentName(runningActivityPck, runningActivity);
	}

	private void runInFg() {
		Notification.Builder builder = new Notification.Builder(this);
		Notification notification = builder.getNotification();
		startForeground(1, notification);
	}
}
