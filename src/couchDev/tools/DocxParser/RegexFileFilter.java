package couchDev.tools.DocxParser;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFileFilter implements FileFilter
 {
	private Pattern filterPattern;	
	
	public RegexFileFilter(String regex){
		filterPattern = Pattern.compile(regex);
	}
	
	public boolean accept(File file) {
		if (!file.isDirectory()){
			Matcher m = filterPattern.matcher(file.getName().toLowerCase());
			return m.find();	
		}else{
			return true;			
		}
	}

}
