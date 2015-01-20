package com.kakenavi.wifireconnect.android;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class MainActivity extends Activity {

	WifiManager m_wifiManager;
	TextView m_statusTxtView;
	wifiConnectionBroadcastReceiver m_wifiConnectionReceiver;
	IntentFilter m_wifiConnectionIntentFilter;
	boolean m_waitCheck;
	Timer m_timer = null;
	Handler m_handle = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 初期化
		m_wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
		m_statusTxtView = (TextView)findViewById(R.id.textView_Status);
		m_wifiConnectionReceiver = new wifiConnectionBroadcastReceiver();
		m_wifiConnectionIntentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		m_waitCheck = false;

		registerReceiver(m_wifiConnectionReceiver, m_wifiConnectionIntentFilter );
	}

	@Override
	protected void onDestroy(){
		unregisterReceiver(m_wifiConnectionReceiver);
	}

	@Override
	protected void onResume()	{
		super.onResume();
		
		// インターネット接続を確認する
		m_statusTxtView.setText(getString(R.string.Msg_Status_Now_Check));
		CheckInternetConnction();

		// wi-fiをいったんオフにしてオンにする
		m_statusTxtView.setText(getString(R.string.Msg_Action_Wifi_Reconnect));
		if( ! m_wifiManager.isWifiEnabled()) {
			if ( ! SetWifiDisable() ) {
				Toast.makeText(this, getString(R.string.Msg_Result_Failed_Reconnect), Toast.LENGTH_LONG).show();
			}
		}
		
		if ( ! SetWifiEnable() ) {
			Toast.makeText(this, getString(R.string.Msg_Result_Failed_Reconnect), Toast.LENGTH_LONG).show();
		}

		m_waitCheck = true;
	
		m_timer = new Timer();
		m_timer.schedule(new OperationTimeOutTimer(), 10000);
	}
	
	private void CheckInternetConnction(){
		if (ping()){
			ShowToast(getString(R.string.Msg_Status_Connected_Internet));
			// インターネットに接続されているなら、Toastを表示して終了する
			applicationExit();
		}else{
			ShowToast(getString(R.string.Msg_Status_Not_Connected_Internet));
		}
	}
	
	private void ShowToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();		
	}
	
	/**
	* adb shell で実行可能なping コマンドを実行することでネットワークの疎通確認を行います
	* http://syucream.hatenablog.jp/entry/20110807/1312743379
	*
	* @return 接続可能ならtrue, 不可能ならfalse
	*/
	private static boolean ping(){
		Runtime runtime = Runtime.getRuntime();
		Process proc = null;
		try {
			proc = runtime.exec("ping -c 1 www.google.com");
			proc.waitFor();
		} catch (Exception e) {
			return false;
		}
		int exitVal = proc.exitValue();
		if ( exitVal == 0 ) {
			return true;
		} else {
			return false;
		}
}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private boolean SetWifiDisable(){
		m_wifiManager.setWifiEnabled(false);
		int timeOutCounter = 0;
		while (m_wifiManager.getWifiState() != WifiManager.WIFI_STATE_DISABLED){
			try {
				Thread.sleep(100);
				timeOutCounter++;
				if (timeOutCounter > 100)
				{
					return false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
		}
			return true;
	}

	private boolean SetWifiEnable(){
		m_wifiManager.setWifiEnabled(true);
		int timeOutCounter = 0;
		while (m_wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED){
			try {
				Thread.sleep(100);
				timeOutCounter++;
				if (timeOutCounter > 100)
				{
					return false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
		}
			return true;
	}

	@SuppressLint("HandlerLeak")
	private void applicationExit() {
		synchronized (this) {
			Message msg = new Message();
			msg.obj = "exit";  
			m_handler.sendMessage(msg);
		}
	}
	
	private	Handler	m_handler	=	new	Handler()	{
		public	void	handleMessage(Message msg)	{
			if ( ((String)msg.obj).equals("exit") ) {
				m_timer.cancel();
				finish();
			}
		
			
		}
	};
	
	private	class wifiConnectionBroadcastReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent){
			// アプリケーションは通知を待っている状態かチェックする
			if ( m_waitCheck ) {
				if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
					CheckInternetConnction();
					applicationExit();
				}
			}
		}
	}

	private class OperationTimeOutTimer extends TimerTask {
		@Override
		public void run() {
			m_handle.post(new Runnable() {
				@Override
				public void run() {
					ShowToast(getString(R.string.Msg_Result_Failed_Reconnect));
					applicationExit();
				}
			});
		}
	}
}


