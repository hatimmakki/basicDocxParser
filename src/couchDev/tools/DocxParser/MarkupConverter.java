package couchDev.tools.DocxParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

public class MarkupConverter {

	String header;
	String footer;
	String encoding;
	File fin;
	File fout;
	HashMap<String,String> pairs;
	BufferedReader in;
	Writer out;
	int requiredBufferSize = 0;
	char[][] keys; 
	String[] values;
	int[] hits;
	LinkedList<Character> rollingWindow = new LinkedList<Character>();
		
	public MarkupConverter(File fileIn,File fileOut,String enc,HashMap<String,String> conversions){
		fin = fileIn;
		fout = fileOut;
		fout.delete();
		encoding = enc;
		pairs = conversions;
		header="";
		footer="";
	}
	public void addHeader(String newHeader){
		header=newHeader;
	}
	public void addFooter(String newFooter){
		footer=newFooter;
	}
	public boolean prepare() throws FileNotFoundException,IOException{
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fout),encoding));
			in = new BufferedReader(new InputStreamReader(new FileInputStream(fin),encoding));
			
			out.write(header);
						
			ArrayList<Entry<String, String>> pairsArr = new ArrayList<Entry<String, String>>(pairs.entrySet());
			Collections.sort(pairsArr,new StringLengthComparitor());
			//okay its sorted, get the first entry
			Entry<String, String> firstEntry = pairsArr.get(0);
			//initialise all the variable we're gonna use in our loop
			requiredBufferSize = firstEntry.getKey().length();
			if (firstEntry.getValue().length()>requiredBufferSize){
				requiredBufferSize=firstEntry.getValue().length();
			}
			keys = new char[pairsArr.size()][]; 
			values = new String[pairsArr.size()];
			hits = new int[pairsArr.size()];

			int i=0;
			for(Entry<String, String> entry : pairsArr) {
				values[i]=entry.getValue();
				keys[i]=entry.getKey().toCharArray();				
				i++;
			}
			//null working vars we no longer need
			pairsArr=null;
			firstEntry=null;
			
			return true;
	}
	public boolean step() throws FileNotFoundException,IOException{
		
		Character charToWrite;
		char currentCharacter;
		
		if(in.ready()){
			
			currentCharacter = (char) in.read();
			rollingWindow.add(currentCharacter); 
			
			//loop to compare keys to string and do replacements
			for(int i=0;i<keys.length;i++){
				//count matching chars
				if(currentCharacter==keys[i][hits[i]]){
					hits[i]++;
				}else{
					hits[i]=0;
				}
				//check if we have a full pattern match
				if (hits[i]==keys[i].length){					
					//remove matched characters
					rollingWindow.subList(rollingWindow.size()-keys[i].length,rollingWindow.size()).clear();
					//flush buffer
					while (rollingWindow.size() > 0){
						charToWrite = rollingWindow.removeFirst();
						out.write(charToWrite);
					}
					//output our value
					out.write(values[i]);
					//rest our counter and break out
					hits[i]=0;
					break;
				}
			}
			if (rollingWindow.size()==requiredBufferSize){
				//output char to roll window on one
				Character cockfox = rollingWindow.removeFirst();
				out.write(cockfox);
			}
			return true;
		}else{
			return false;
		}
	}
	
	public boolean finish() throws FileNotFoundException,IOException{
		//empty the last of the buffer
		while (rollingWindow.size() > 0){
			Character cockfox = rollingWindow.removeFirst();
			out.write(cockfox);
		}
		out.write(footer);
		out.close();
		return true;
	}
	
}
class StringLengthComparitor implements Comparator<Entry<String,String>>{
	
	public int compare(Entry<String, String> lhs, Entry<String, String> rhs) {
		if(lhs.getKey().length() == rhs.getKey().length()){
			return 0;	
		}
		if(lhs.getKey().length() > rhs.getKey().length()){
			return -1;
		}else{
			return 1;
		}
	}
}