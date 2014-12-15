package com.androidaq;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class WaveFormPlotThread extends Thread {
	private SurfaceHolder holder;
	private WaveFormView plot_area;
	private boolean _run = false;
	
	public WaveFormPlotThread(SurfaceHolder surfaceHolder, WaveFormView view){
		holder = surfaceHolder;
		plot_area = view;
	}
	public void setRunning(boolean run){
		_run = run;
	}
	
	@Override
	public void run(){
		Canvas c;
		while(_run){
			c = null;
			try{
				c = holder.lockCanvas(null);
				synchronized (holder) {
					plot_area.PlotPoints(c);
				}
			}finally{
				if(c!=null){
					holder.unlockCanvasAndPost(c);
				}
			}
		}
	}
}
