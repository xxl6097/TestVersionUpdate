package com.uuxia.version;

import android.app.ActivityGroup;
import android.os.Bundle;

public class TestVersionUpdateActivity extends ActivityGroup {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_main);
		UpdateHander.checkVersion(this, "http://files.cnblogs.com/xiaxiaoli/version.xml");
	}
}
