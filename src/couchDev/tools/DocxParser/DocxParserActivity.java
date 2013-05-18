package couchDev.tools.DocxParser;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class DocxParserActivity extends Activity implements IFolderItemListener {

    FolderLayout localFolders;
    Extractor extractor;
    File file;
    Boolean externalOpen = false;
    boolean loading = false;
    Activity showFile = null;
    ProgressDialog dialog;
    private KillReceiver mKillReceiver;
    private ReloadReceiver mReloadReceiver;
    boolean invertColors;
    String textSize;
    boolean reloadOnResume;
       
    public boolean getReloadOnResume(){
    	return reloadOnResume;    	
    }
    public void handleURI(Uri uri){
    	if (uri != null){
        	externalOpen=true;
        	if (extractor != null){
        		extractor.cancel(true);
        	}
        	file = new File(uri.toString());
        	loadingDialog();
        	extractor = new Extractor(this,textSize,invertColors);
        	extractor.execute(uri);        	
    	}    	
    }
   
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
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
            	extractor=null;
            	cleanTempFiles();
            	this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onNewIntent(Intent newCall){
    	 Uri uri = null;
         try {
             uri = newCall.getData();
         } catch(Exception e) {
         	Log.e("error",e.getMessage());
         }
         handleURI(uri);
    }
    
    @Override
    protected void onDestroy() {      
        unregisterReceiver(mKillReceiver);
        unregisterReceiver(mReloadReceiver);
        if (dialog!=null){
    		dialog.dismiss();
        	dialog=null;	
    	}
        super.onDestroy();
    }
    
    @SuppressLint("NewApi")
	public void cleanTempFiles(){
    	
    	 File cachePath = null;
         if (Build.VERSION.SDK_INT >= 8) {
 			cachePath = this.getExternalCacheDir();    
 	     }
         if (cachePath==null){
 			cachePath = this.getCacheDir();
 		}
 		File extractTo = new File(cachePath,"document.xml");
 		extractTo.delete();
 		File parsedTo = new File(cachePath,"document.parsed");
 		parsedTo.delete();
    }
        
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        
        mKillReceiver = new KillReceiver();
        registerReceiver(mKillReceiver, IntentFilter.create("kill","kill/all"));
        mReloadReceiver = new ReloadReceiver();
        registerReceiver(mReloadReceiver, IntentFilter.create("ReloadDocx","reload/Docx"));
                        
        Uri uri = null;
        try {
            uri = getIntent().getData();
        } catch(Exception e) {
        	Log.e("error",e.getMessage());
        }
        this.getPrefs();
        SettingsHolder settings = (SettingsHolder)this.getLastNonConfigurationInstance();
        if (settings!=null){
        	loading=settings.loading;
        	reloadOnResume=settings.reloadOnResume;
            extractor=settings.extractor;
            showFile=settings.showFile;
            if(extractor!=null && !extractor.isCancelled() && uri==null){
            	extractor.setParent(this);
            }
            file=settings.file;
        }

        setContentView(R.layout.main);
        
        localFolders = (FolderLayout)findViewById(R.id.localfolders);
        
        localFolders.setIFolderItemListener(this);
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
        	localFolders.setDir("./sdcard");
        }else{
        	localFolders.setDir("./");	
        }
        handleURI(uri);
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
            this.invertColors = prefs.getBoolean("invert", false);
            this.textSize = prefs.getString("textsize", "Normal"); 
    }
    
    //Your stuff here for Cannot open Folder
    public void OnCannotFileRead(File file) {
        new AlertDialog.Builder(this)
        .setIcon(R.drawable.ic_launcher)
        .setTitle(
                "[" + file.getName()
                        + "] folder can't be read!")
        .setPositiveButton("OK",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog,
                            int which) {
                    }
                }).show();
    }

    //Your stuff here for file Click
    public void OnFileClicked(File fileClicked) {
    	file = fileClicked;
    	if (extractor != null){
    		extractor.cancel(true);
    	}
    	if(file.exists()){
    		//loading dialog
    		loadingDialog();
    		extractor = new Extractor(this,textSize,invertColors);
    		extractor.execute(Uri.fromFile(file));
    	}else{
    		//error
    		Toast.makeText(this, "File cannot be opened", Toast.LENGTH_SHORT).show();
    	}
    }
    public void setShowFile(Activity a){
    	showFile = a;
    }
    
    public void killShowFile(){
    	if (showFile != null){
    		showFile.finish();
    	}    	
    }
    
    protected void onResume(){
    	super.onResume();
	    if(loading){
	    	loadingDialog();
	    }
	    if(extractor!=null && (extractor.isCancelled()||extractor.isDone())){
	    	DoneLoading();	    	
	    }
	    if(extractor!=null && !extractor.isCancelled()){
	    	extractor.setParent(this);
	    }
	    if (reloadOnResume){
	    	extractor = new Extractor(this,textSize,invertColors);	
    		extractor.execute(Uri.fromFile(file));
    		reloadOnResume=false;
	    }
    }    
    protected void onPause(){
    	if (dialog!=null){
    		dialog.dismiss();
        	dialog=null;	
    	}
    	if(extractor!=null){
    		extractor.WaitForIt();
    	}
    	super.onPause();
    }
    private static class SettingsHolder {
		boolean loading = false;
		boolean reloadOnResume=false;
		Extractor extractor = null;
		File file=null;
		Activity showFile = null;
	}
    public Object onRetainNonConfigurationInstance(){	
    	SettingsHolder bucketObject = new SettingsHolder();
    	bucketObject.loading=loading;
    	bucketObject.reloadOnResume=reloadOnResume;
    	bucketObject.extractor=extractor;
    	bucketObject.file=file;
    	return bucketObject;
    }
    private void loadingDialog(){
    	loading=true;
    	if (dialog!=null){
    		dialog.dismiss();
        	dialog=null;	
    	}
    	try {
			dialog = ProgressDialog.show(this, "", 
			        "Loading "+URLDecoder.decode(file.getName(), "UTF-8")+" Please wait...", true);
		} catch (UnsupportedEncodingException e) {
			//if this doesn't happen i don't care
			//e.printStackTrace();
		}
    	dialog.setCancelable(true);
    	dialog.setOnCancelListener(
			new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface arg0) {
					extractor.cancel(true);
					DoneLoading();
				}
            });
    }
    public void notDocx(){
    	Toast.makeText(this, "File cannot be opened", Toast.LENGTH_SHORT).show();
    }
    public void DoneLoading(){
    	if (dialog!=null){
    		dialog.dismiss();
        	dialog=null;	
    	}
    	loading=false;
    }
    public boolean isExternalOpen(){
    	return externalOpen;    	
    }
    @SuppressLint("NewApi")
	@Override  
    public void onBackPressed() {
    	if (localFolders.isRoot()){
    		if (Build.VERSION.SDK_INT >= 5) {
    			super.onBackPressed();    
     	     }
    		
    	}else{
    		localFolders.setDir(
    			new File(localFolders.getCurrPath()).getParent()
    		);
    	}
    }
    private final class KillReceiver extends BroadcastReceiver {
    	@Override
        public void onReceive(Context context, Intent intent) {
    		((DocxParserActivity) context).cleanTempFiles();
    		finish();
        }
    }
    private final class ReloadReceiver extends BroadcastReceiver {
    	@Override
        public void onReceive(Context context, Intent intent) {
    		reloadOnResume = true;
        }
    }
    
    
}
