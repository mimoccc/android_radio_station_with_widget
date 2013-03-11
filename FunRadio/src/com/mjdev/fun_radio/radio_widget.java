package com.mjdev.fun_radio;

import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import com.mjdev.base.launcher_intent;
import com.mjdev.fun_radio.R;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

public class radio_widget extends AppWidgetProvider {
	public  static int                 wcd_alpha    = 0;
	public  static int                 wcd_alpha_up = 0;
	public  static boolean             disabled     = false;
	private static ConnectivityManager cm           = null;
	private static PowerManager        pm           = null;
	public  static AppWidgetManager    appMan;
	private static Context             ctx;
	public  radio_info                 ri;
	private Timer                      timer        = new Timer();
	private TimerTask                  timerTask    = new TimerTask() {public void run(){updateInfo();}};
	private static String              lastText     = "";
	@Override
	public          void onReceive   (Context context, Intent intent) {
		try {
			final String action = intent.getAction();
			if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) super.onReceive(context, intent);
			else if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
				final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
				if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) this.onDeleted(context, new int[] { appWidgetId });
			} 
			else if (TextUtils.equals(action, launcher_intent.Action.ACTION_ITEM_CLICK))   onClick          (context, intent);
			else if (TextUtils.equals(action, launcher_intent.Action.ACTION_VIEW_CLICK))   onClick          (context, intent);
			else {
				super.onReceive(context, intent); 
			}
		} catch (Exception e) { }
	}
	@Override
	public          void onUpdate    (Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		ctx = context;
		appMan = appWidgetManager;
		cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
		timer.scheduleAtFixedRate(timerTask , 0, 20000);
		RemoteViews views   = new RemoteViews(ctx.getPackageName(), R.layout.radio_widget);
		Intent cintent = new Intent(ctx, fun_radio.class);
		views.setOnClickPendingIntent(R.id.wbck, PendingIntent.getActivity(context, 0, cintent, 0));
		views.setOnClickPendingIntent(R.id.wcd, PendingIntent.getActivity(context, 0, cintent, 0));
		views.setOnClickPendingIntent(R.id.wbck1, PendingIntent.getActivity(context, 0, cintent, 0));
		views.setOnClickPendingIntent(R.id.wlabel, PendingIntent.getActivity(context, 0, cintent, 0));
		views.setOnClickPendingIntent(R.id.radio_image, PendingIntent.getActivity(context, 0, cintent, 0));
		views.setOnClickPendingIntent(R.id.radio_text, PendingIntent.getActivity(context, 0, cintent, 0));
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}
	@Override
	public          void onDeleted   (Context context, int[] appWidgetIds) {
		if (timer != null) { 
			timer.cancel(); 
			timer.purge(); 
			timer = null;
		}
		super.onDeleted(context, appWidgetIds);
	}
	public          void updateInfo(){
		try {
			if(isOnline()&&(pm!=null)&&(pm.isScreenOn())){
				URL url = new URL((ctx.getString(R.string.radio_info_url)).replace(" ", "%20"));
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				radio_info_handler rh = new radio_info_handler();
				xr.setContentHandler(rh);
				xr.parse(new InputSource(url.openStream()));
				ri = rh.getRadioInfo();
				if((ri!=null)&&(lastText!=ri.track)) {
					RemoteViews views   = new RemoteViews(ctx.getPackageName(), R.layout.radio_widget);
					String text = "";
					if(ri.author!=null) text += ri.author + "\n";
					if(ri.album!=null)  text += ri.album + " ";
					if(ri.track!=null)  text += ri.track + "\n";
					if(ri.picture!=null) views.setImageViewBitmap(R.id.radio_image, ri.picture);
					views.setTextViewText(R.id.radio_text, text);
					Intent cintent = new Intent(ctx, fun_radio.class);
					views.setOnClickPendingIntent(R.id.radio_image, PendingIntent.getActivity(ctx, 0, cintent, 0));
					views.setOnClickPendingIntent(R.id.radio_text, PendingIntent.getActivity(ctx, 0, cintent, 0));
					appMan.updateAppWidget(new ComponentName(ctx, radio_widget.class), views);
					lastText = ri.track;
				}
				ri = null;
		    }
		} catch(Exception e){ ri=null; Log.e("funradio",e.getMessage());}
	}
	public          boolean isOnline() {
		 if(cm==null) return false;
		 NetworkInfo nf = cm.getActiveNetworkInfo();
		 if(nf==null) return false;
		 return nf.isConnectedOrConnecting();
	}
	public void     onClick(Context ctx, Intent intent) { ctx.startActivity(new Intent("com.mjdev.fun_radio.fun_radio")); }
}