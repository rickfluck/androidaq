package com.androidaq;

import android.app.Application;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formKey = "dE1MZHpMVDl6ZndLYzdiUTV0czEzR3c6MQ")
//logcatArguments = { "-t", "100", "-v", "long", "ActivityManager:I", "MyApp:D", "*:S" }

public class MyApplication extends Application {
	
	 @Override
	  public void onCreate() {
	      // The following line triggers the initialization of ACRA
	      ACRA.init(this);
	      super.onCreate();
	  }
}
