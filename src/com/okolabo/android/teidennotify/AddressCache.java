package com.okolabo.android.teidennotify;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;

public class AddressCache {

	private static HashMap<String,SoftReference<ArrayList<String>>> cache = new HashMap<String,SoftReference<ArrayList<String>>>();
	
	public static ArrayList<String> getData(String key) {
	    if (cache.containsKey(key)) {
	    	SoftReference<ArrayList<String>> ref = cache.get(key);
	    	if (ref != null) {
	    		return ref.get();
	    	}
	    }  
	    return null;  
	}  
	
	public static void setData(String key, ArrayList<String> list) {
	    cache.put(key, new SoftReference<ArrayList<String>>(list));
	}
	
	public static boolean hasData(String key) {
		return cache.containsKey(key);
	}
	
	public static void clear() {
		cache.clear();
	}
}
