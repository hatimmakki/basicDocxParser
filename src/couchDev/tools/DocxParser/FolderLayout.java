package couchDev.tools.DocxParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class FolderLayout extends LinearLayout implements OnItemClickListener {

 Context context;
 IFolderItemListener folderListener;
 private List<String> item = null;
 private List<String> path = null;
 private String root = "/";
 private boolean isRoot = true;
 private TextView myPath;
 private ListView lstView;
 private String currPath;

 public FolderLayout(Context context, AttributeSet attrs) {
     super(context, attrs);

     this.context = context;

     LayoutInflater layoutInflater = (LayoutInflater) context
             .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
     		layoutInflater.inflate(R.layout.folderview, this);

     myPath = (TextView) findViewById(R.id.path);
     lstView = (ListView) findViewById(R.id.list);

     Log.i("FolderView", "Constructed");
     getDir(root, lstView);

 }
 public boolean isRoot(){
	return isRoot;
 }
 public String getCurrPath(){
	return currPath;
 }
 public void setIFolderItemListener(IFolderItemListener folderItemListener) {
     this.folderListener = folderItemListener;
 }

 //Set Directory for view at any time
 public void setDir(String dirPath){
     getDir(dirPath, lstView);
 }

 private void getDir(String dirPath, ListView v) {

	 if (dirPath.equals(".")){
		 dirPath = root;
	 }
	 myPath.setText("Location: " + dirPath);
     item = new ArrayList<String>();
     path = new ArrayList<String>();
     this.currPath = dirPath;
     File f = new File(dirPath);
     File[] files = f.listFiles(new RegexFileFilter(".*\\.docx$"));

     if (!dirPath.equals(root)&&!dirPath.equals("./")) {
         item.add(root);
         path.add(root);
         item.add("../");
         path.add(f.getParent());
         this.isRoot=false;
     }else{
    	 this.isRoot=true;    	 
     }  
     for (int i = 0; i < files.length; i++) {
         File file = files[i];
         path.add(file.getPath());
         if (file.isDirectory()){
             item.add(file.getName() + "/");
         }else{
        	 item.add(file.getName());
         }

     }

     Log.i("Folders", files.length + "");
     setItemList(item);

 }

 //can manually set Item to display, if u want
 public void setItemList(List<String> item){
     ArrayAdapter<String> fileList = new ArrayAdapter<String>(context,
             R.layout.row, item);

     lstView.setAdapter(fileList);
     lstView.setOnItemClickListener(this);
 }


 public void onListItemClick(ListView l, View v, int position, long id) {
     File file = new File(path.get(position));
     if (file.isDirectory()) {
         if (file.canRead())
             getDir(path.get(position), l);
         else {
        	 //what to do when folder is unreadable
             if (folderListener != null) {
                 folderListener.OnCannotFileRead(file);

             }

         }
     } else {

//what to do when file is clicked
//You can add more,like checking extension,and performing separate actions
         if (folderListener != null) {
             folderListener.OnFileClicked(file);
         }

     }
 }

 public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
     onListItemClick((ListView) arg0, arg0, arg2, arg3);
 }

}
