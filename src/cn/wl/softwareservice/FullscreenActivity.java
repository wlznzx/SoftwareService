package cn.wl.softwareservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class FullscreenActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent _intent = new Intent(this, SoftwareService.class);
		startService(_intent);
	}
	
}
