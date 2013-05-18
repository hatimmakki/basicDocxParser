package couchDev.tools.DocxParser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public class Extractor extends AsyncTask<Uri,Integer,Boolean>{
	
	private Context parent;
	private String filename;
	private File cachePath;
	private boolean done=false;
	private boolean waitForIt=false;
    boolean invertColors;
    String textSize;
    private Uri fileUri; 
	
	@SuppressLint("NewApi")
	public Extractor(Context parentThread,String size,boolean color){
		parent = parentThread;
	    invertColors = color;
	    textSize = size;           
		
		if (Build.VERSION.SDK_INT >= 8) {
			cachePath = parent.getExternalCacheDir();    
	    }		
		if (cachePath==null){
			cachePath = parent.getCacheDir();
		}
		 File extractTo = new File(cachePath,"document.xml");
		 extractTo.delete();
	}
	public boolean isDone(){
		return done;		
	}
	public void setParent(Context parentThread){
		parent = parentThread;
		waitForIt=false;
		if (
				this.getStatus() == AsyncTask.Status.FINISHED 
				&& !isCancelled()
				&& !done
				){
			DoPostExecute();
		}
	}
	protected Boolean doInBackground(Uri... Uris) {
        //check in loop, can call .cancel from outside but you need to handle it
		//onCancelled will be called.
		filename=Uris[0].getLastPathSegment();
		return unpackZipToCache(Uris[0]);
    }
    protected void onPostExecute(Boolean result) {
    	if (!waitForIt && !result){
      		 ((DocxParserActivity) parent).notDocx();
      		 ((DocxParserActivity) parent).DoneLoading();            		  
      	 	}
    	if (!waitForIt && result){
    		DoPostExecute();
    	}
    }
    public void WaitForIt(){
    	waitForIt=true;
    	parent=null;
    }
    private void DoPostExecute(){
    	if (!isCancelled()){
	    	((DocxParserActivity) parent).DoneLoading();
	    
	    	Intent mIntent = new Intent(parent, ShowFile.class);
	    	mIntent.putExtra("filename", filename);
			mIntent.putExtra("cachePath", cachePath);
			mIntent.putExtra("fileUri", fileUri);
			mIntent.putExtra("invertColors", invertColors);
			mIntent.putExtra("textSize", textSize);
			mIntent.putExtra("externalLoad", ((DocxParserActivity) parent).isExternalOpen());
			
			if ( ((DocxParserActivity) parent).isExternalOpen()){
				mIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			}
			
			parent.startActivity(mIntent);
			done=true;
    	}
    }
    private boolean unpackZipToCache(Uri filePath)
    {       
    	 this.fileUri = filePath;
         InputStream is;
         ZipInputStream zin;
         try
         {
        	 if (filePath.getScheme().equals("http") || filePath.getScheme().equals("https")){
        		is = new URL(filePath.toString()).openStream();
 	        }else{
 	        	is = parent.getContentResolver().openInputStream(filePath);
 	        }
             zin = new ZipInputStream(new BufferedInputStream(is));          
             ZipEntry ze;
             boolean wasDocx = false;
             while ((ze = zin.getNextEntry()) != null) 
             {
        		if (isCancelled()){
        			return false;
                }
                 String filename = ze.getName();
                 if (filename.equals("word/document.xml")){
                   wasDocx = true;
                   File extractTo = new File(cachePath,"document.xml");
                   FileOutputStream fos = new FileOutputStream(extractTo);
              	   int byteNumber;
              	   byte [] buffer = new byte[1024];
              	   while (0<(byteNumber=zin.read(buffer))){
              	      fos.write(buffer,0,byteNumber);
              	   }
              	   fos.close();
              	   
              	 HashMap<String, String> conversions = new HashMap<String, String>();
              	 conversions.put("tab/>","z/>&nbsp;&nbsp;&nbsp;&nbsp;");
              	 conversions.put("w:val=\"","class=\"c");
              	 conversions.put("tc>","td>");
              	 conversions.put("tbl>","table>");
              	 conversions.put("</w:","</");              	
              	 conversions.put("<w:","<");
              	
              	MarkupConverter xmlToHTML = new MarkupConverter(
              			 new File(cachePath,"document.xml"),
              			 new File(cachePath,"document.parsed"),
              			 "utf-8",
              			 conversions
      			 );
                
                String textSizing = "font-size:14pt !important;";                
                if (textSize.equals("Smaller")){
                	textSizing = "font-size:10pt !important;";
                }
                if (textSize.equals("Small")){
                	textSizing = "font-size:12pt !important;";
                }
                if (textSize.equals("Large")){
                	textSizing = "font-size:16pt !important;";
                }
                if (textSize.equals("Larger")){
                	textSizing = "font-size:18pt !important;";
                }
                String colouring = "color:white;background-color:black;";
                String bulletColour = "background-color:white;";
                if (invertColors){
                	colouring = "color:black;background-color:white;";
                	bulletColour = "background-color:black;";
                }
                
              	xmlToHTML.addHeader("<html>" +
              			"<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=utf-8\">" +
              			"<head>" +
              			"<style type='text/css'>" +
              			"	body{"+colouring+" "+textSizing+"}" +
              			"	p{"+colouring+" "+textSizing+"}" +
              			"	rPr{list-style:outside;}" +
              			"	*{text-decoration:none;font-weight:normal;font-style:normal;}" +
              			"	drawing{display:none;}" +
              			"	numPr{"+bulletColour+" display:inline-block; position:relative; height:3px; width:3px; vertical-align:middle;margin-right:5px;margin-top:-3px;}" +
              			"</style>" +              			          			
              			"</head>");
              	xmlToHTML.addFooter("</html>");
              	
              	try{
              		xmlToHTML.prepare();
              		while(xmlToHTML.step()){
              			if (isCancelled()){
                			return false;
                        }
              		}
              		if (isCancelled()){
            			return false;
                    }
              		xmlToHTML.finish();
              	} catch (FileNotFoundException e) {
        			return false;
        		} catch (IOException e) {
        			return false;
        		}	
				return true;
             	}
                 zin.closeEntry();
             }             
             zin.close();
             if(!wasDocx){
            	 return false;            	 
             }
         } 
         catch(IOException e)
         {
             e.printStackTrace();
             Log.d("zip",e.toString());
             return false;
         }
        return false;
    }
}