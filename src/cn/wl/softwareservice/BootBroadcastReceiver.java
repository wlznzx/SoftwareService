package cn.wl.softwareservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
			// ¿ªÆôSoftwareService
	        Intent _intent = new Intent(context, SoftwareService.class);
	        _intent.putExtra(Contants.KEY_IS_BOOT, true);
			context.startService(_intent);
		}
	}

}
