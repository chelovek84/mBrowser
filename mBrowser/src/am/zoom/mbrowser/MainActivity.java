package am.zoom.mbrowser;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class MainActivity extends Activity {
	protected WebView myWebView;
	protected FrameLayout videoFrame;
	
	protected AlertDialog dialog;
	protected TextView uri;
	
	protected WebChromeClient mClient;
	protected View mCustomView;
	protected CustomViewCallback mCustomViewCallback;
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		//---------------------views part-----------------------//
		myWebView = (WebView)findViewById(R.id.mWebView);
		videoFrame = (FrameLayout) findViewById(R.id.videoFrame);
		View prompt = getPrompt();
		uri = (TextView)prompt.findViewById(R.id.uri);
		
		//---------------------webview part-----------------------//
		mClient = new WebChromeClient(){
			@Override
			public void onShowCustomView(View view, CustomViewCallback callback) {
				myWebView.setVisibility(View.GONE);
				
				mCustomViewCallback = callback;
				mCustomView = view;
				
				videoFrame.addView(view);
				videoFrame.setVisibility(View.VISIBLE);
				videoFrame.bringToFront();
			}
			
			@Override
			public void onHideCustomView() {
				if(mCustomView!=null){
					videoFrame.setVisibility(View.GONE);
					videoFrame.removeAllViews();
					
					mCustomView = null;
					mCustomViewCallback.onCustomViewHidden();
					
					myWebView.setVisibility(View.VISIBLE);
				}
			}
		};
		
		myWebView.setWebViewClient(new WebViewClient(){
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				if(!url.equals("about:blank")){
					View layout = getLayoutInflater().inflate(R.layout.toast, (ViewGroup)findViewById(R.id.toastWrapper));
					
					TextView text = (TextView)layout.findViewById(R.id.toastText);
					text.setText(url);
					
					Toast toast = new Toast(MainActivity.this);
					toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
					toast.setDuration(Toast.LENGTH_SHORT);
					toast.setView(layout);
					toast.show();
				}
				
				super.onPageStarted(view, url, favicon);
			}
		});
		myWebView.setWebChromeClient(mClient);
		myWebView.setDownloadListener(new DownloadListener() {
			
			@Override
			public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
				String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
				
				Toast.makeText(MainActivity.this, "Downloading " + fileName, Toast.LENGTH_LONG).show();
				
				Request request = new DownloadManager.Request(Uri.parse(url));
				request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
				
				final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
				dm.enqueue(request);
			}
			
		});
		WebSettings webSettings = myWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		myWebView.loadUrl("about:blank");
		
		//---------------------prompt part-----------------------//
		uri.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_GO) {
					String uriValue = uri.getText().toString();
					
					if(uriValue.startsWith("http://") || uriValue.startsWith("https://")){
						myWebView.loadUrl(uriValue);
					}
					else{
						String host = Uri.parse("http://" + uriValue).getHost();
						if(host==null || host.indexOf(".")==-1 || host.indexOf(".")==0 || host.indexOf(".")==host.length()-1){
							search(uriValue);
						}
						else{
							myWebView.loadUrl("http://" + uriValue);
						}
					}
					
					dialog.cancel();
					
					handled = true;
				}
				return handled;
			}
		});
		
		dialog = new AlertDialog.Builder(this).setView(prompt).create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_back:
			
			if(myWebView.canGoBack())
			{
				myWebView.goBack();
			}
			
			return true;
		case R.id.action_forward:
			
			if(myWebView.canGoForward())
			{
				myWebView.goForward();
			}
			
			return true;
		case R.id.action_exit:
			myWebView.loadUrl("about:blank");
			
			this.finish();
			
			return true;
		case R.id.action_reload:
			
			myWebView.reload();
			
			return true;
		case R.id.action_load:
			
			String currentUrl = myWebView.getUrl();
			if( currentUrl.startsWith("http://") || currentUrl.startsWith("https://") ){
				uri.setText(currentUrl);
			}
			
			dialog.show();
			
			return true;
		default:
			return true;
		}
	};
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if(mCustomView!=null){
				mClient.onHideCustomView();
			}
			else{
				openOptionsMenu();
			}
			
			return true;
		}
		else
		{
			return super.onKeyDown(keyCode, event);
		}
	}
	
	protected void search(String query){
		String keywords = null;
		try {
			keywords = URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if(keywords!=null){
			myWebView.loadUrl("https://www.google.com/search?q=" + keywords);
		}
	}
	
	@SuppressLint("InflateParams")
	private View getPrompt(){
		return getLayoutInflater().inflate(R.layout.prompt, null);
	}
}
