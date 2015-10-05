package couchDev.tools.DocxParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
//import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public class ShowFile extends Activity {
	
	protected FrameLayout webViewPlaceholder;
	protected WebView webView;
	private Boolean invertColors;
    private String textSize;
	
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {	
      super.onSaveInstanceState(outState);
   
      // Save the state of the WebView
      webView.saveState(outState);
    }
     
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {     
      super.onRestoreInstanceState(savedInstanceState);
   
      // Restore the state of the WebView
      webView.restoreState(savedInstanceState);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    { 
      if (webView != null)
      {
        // Remove the WebView from the old placeholder
        webViewPlaceholder.removeView(webView);
      }
   
      super.onConfigurationChanged(newConfig);
      
      // Load the layout resource for the new configuration
      setContentView(R.layout.file);
   
      // Reinitialize the UI
      initUI();
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.file);
        
        this.invertColors = (Boolean)this.getIntent().getExtras().get("invertColors");
        this.textSize = (String)this.getIntent().getExtras().get("textSize");
        
        // Initialize the UI
        initUI();
    }
    protected void initUI()
    {   
    	//set header
        String headerText = this.getIntent().getStringExtra("filename");
        if(headerText.equals("RAW")){
        	headerText="Email Attachment";        	
        }
    	setTitle(headerText);
    	
    	// Retrieve UI elements
        webViewPlaceholder = ((FrameLayout)findViewById(R.id.webViewPlaceholder));
        
        // Initialize the WebView if necessary
        if (webView == null)
        {	
          //let the user know we're doing it
          final ProgressDialog pd = ProgressDialog.show(this, "", "Rendering...",true);
          
          // Create the webview
          webView = new WebView(this);
          
          // Load file
          try {       	
        	  webView.loadUrl((new File((File) this.getIntent().getSerializableExtra("cachePath"),"document.parsed")).toURL().toString());
  			} catch (MalformedURLException e) {
  			  webView.loadData("Error Loading File", "text/html", "utf-8");
  			}
          
          webView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
          webView.getSettings().setSupportZoom(true);
          webView.getSettings().setBuiltInZoomControls(true);
          webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
          webView.setScrollbarFadingEnabled(true);
          webView.getSettings().setLoadsImagesAutomatically(true);
     
          // Load the URLs inside the WebView, not in the external web browser
          webView.setWebViewClient(new WebViewClient() {
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
         }
        
        // Attach the WebView to its placeholder
        webViewPlaceholder.addView(webView);
      
    }
    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
          sb.append(line).append("\n");
        }
        return sb.toString();
    }

    public String getStringFromFile () throws Exception {
        File fl = new File((File) this.getIntent().getSerializableExtra("cachePath"),"document.parsed");
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();        
        return ret;
    }
    public void toastThis(String str){
    	Toast.makeText(this.getBaseContext(),str,Toast.LENGTH_LONG).show();
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
}