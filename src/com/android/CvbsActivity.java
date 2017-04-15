package com.android;

public class CvbsActivity {
	public static CvbsActivity init() {
        return new CvbsActivity();
    }
    
	CvbsActivity(){
		FMTInit();
    }
	
	public final void FMTInit(){
		return;
	}
	
	public final long FMTopenDevice(){
		return openDevice();
	}
	
	public final int Cvbs_det(long lHandle){
		// return CvbsDet(lHandle);
		return 1;
	}
	
	public final int Reverse_Gear(long lHandle){
		return ReverseGear(lHandle);
	}
	
	public native long openDevice();
    public native int CvbsDet(long lHandle);
    public native int ReverseGear(long lHandle);
    
	static{
		System.loadLibrary("CVBS-jni");
	}
}
