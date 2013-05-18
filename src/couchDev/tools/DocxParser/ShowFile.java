package couchDev.tools.DocxParser;

import java.io.File;
import java.net.MalformedURLException;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
//import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

public class ShowFile extends Activity {
	
	private Boolean invertColors;
    private String textSize;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
    public void onStart(){
    	super.onStart();
    	this.getPrefs();
    }
    
    private void getPrefs(){
            // Get the xml/preferences.xml preferences
            SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(this.getBaseContext());
            
			if (
			  invertColors != prefs.getBoolean("invert", false)
			  ||
			  !textSize.equals(prefs.getString("textsize", "Normal"))
			){
				if (this.getIntent().getExtras().getBoolean("externalLoad")){
					Toast.makeText(this,"Changes will be applied next time you open a file", Toast.LENGTH_LONG).show();
				}else{				
					invertColors = prefs.getBoolean("invert", false);
					textSize = prefs.getString("textsize", "Normal");
					//settings changed
					Toast.makeText(this,"Reloading to apply changes", Toast.LENGTH_LONG).show();
					//reload the document
					Intent intent = new Intent("ReloadDocx");
	            	intent.setType("reload/Docx");
	            	sendBroadcast(intent);
	            	this.finish();
				}
			}			
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_link:
            	Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.couchdev.co.uk"));
            	startActivity(browserIntent);
                return true;
            case R.id.menu_preferences:
            	Intent settingsActivity = new Intent(getBaseContext(),
                        Preferences.class);
        		startActivity(settingsActivity);        		        		
                return true;
            case R.id.menu_exit:
            	Intent intent = new Intent("kill");
            	intent.setType("kill/all");
            	sendBroadcast(intent);
            	this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public void toastThis(String str){
    	Toast.makeText(this.getBaseContext(),str,Toast.LENGTH_LONG).show();
    }
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SettingsHolder settings = (SettingsHolder)this.getLastNonConfigurationInstance();
        if (settings!=null){
        }
        
        setContentView(R.layout.file);
        TextView header = (TextView)findViewById(R.id.filename);
        String headerText = this.getIntent().getStringExtra("filename");
        if(headerText.equals("RAW")){
        	headerText="Email Attachment";        	
        }
        header.setText(headerText);
        
        this.invertColors = (Boolean)this.getIntent().getExtras().get("invertColors");
        this.textSize = (String)this.getIntent().getExtras().get("textSize");
        		
        WebView mWebView = (WebView) findViewById(R.id.content);
        //WebSettings webSettings = mWebView.getSettings();
        //webSettings.setJavaScriptEnabled(true);        
        //webSettings.setBuiltInZoomControls(true);
        //webSettings.setSupportZoom(true);
        //mWebView.invokeZoomPicker();
        
        final ProgressDialog pd = ProgressDialog.show(this, "", "Rendering...",true);
                        
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
            	super.onPageFinished(view,url);
                if(pd!=null&&pd.isShowing())
                {
                	try {
                		pd.dismiss();	                		
                    } catch (Exception e) {
                        
                    }
                }
            }
            @Override
            public void onLoadResource(WebView view, String url){
            	super.onLoadResource(view, url);            	
            }
        });
        
        try {
			mWebView.loadUrl((new File((File) this.getIntent().getSerializableExtra("cachePath"),"document.parsed")).toURL().toString());
		} catch (MalformedURLException e) {
			mWebView.loadData("Error Loading File", "text/html", "utf-8");
		}
    }
    private static class SettingsHolder {
		
	}
    public Object onRetainNonConfigurationInstance(){	
    	SettingsHolder bucketObject = new SettingsHolder();
    	//add your variables and edit above class for statefulness
    	return bucketObject;
    }
    @Override  
    public void onBackPressed() {
    	if (this.getIntent().getExtras().getBoolean("externalLoad")){
    		Intent intent = new Intent("kill");
        	intent.setType("kill/all");
        	sendBroadcast(intent);
    	}
    	this.finish();
    }
}