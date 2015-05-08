package com.cloud.ci.plugins.gssh;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Utils {
	
	public static InputStream getInputStreamFromString(String s){
		return new ByteArrayInputStream(s.getBytes());
	}
	
	public static String getStringFromStream(InputStream is) {
		if (null == is) {
			throw new RuntimeException(
					"Convert Stream to String failed as input stream is null");
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		char[] buffer = new char[2048];
		try {
			int len = -1;
			while (-1 != (len = br.read(buffer))) {
				sb.append(buffer, 0, len);
			}
		} catch (IOException e) {
			throw new RuntimeException("Convert Stream to String failed !", e);
		} finally {
			if(null != is){
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
		String content = sb.toString();
		return content;
	}
}
