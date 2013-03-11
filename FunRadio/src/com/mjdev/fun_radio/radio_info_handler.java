package com.mjdev.fun_radio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class radio_info_handler extends DefaultHandler {
	private enum tag { none, skladba, interpret, album, program, linka};
	private radio_info rinfo = null;
	private tag in_tag = tag.none;
	public  radio_info getRadioInfo() { return this.rinfo; }
	@Override
	public void startDocument() throws SAXException { this.rinfo = new radio_info(); }
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		if      (localName.equals("skladba"))     in_tag = tag.skladba;
		else if (localName.equals("interpret"))   in_tag = tag.interpret;
		else if (localName.equals("album"))       in_tag = tag.album; 
		else if (localName.equals("program"))     in_tag = tag.program;
		else if (localName.equals("linka"))       in_tag = tag.linka;
		else in_tag = tag.none;
	}
	public void characters(char ch[], int start, int length) {
        String chars = (new String(ch).substring(start, start + length));
    	if (in_tag == tag.skladba)    rinfo.track       = chars;
    	if (in_tag == tag.interpret)  rinfo.author      = chars;
    	if (in_tag == tag.album)      rinfo.album       = chars;
    	if (in_tag == tag.program)    rinfo.program     = chars;
    	if (in_tag == tag.linka)      {
    		rinfo.picture_url = chars;
    		if(rinfo.picture_url!=null) rinfo.picture = get_remote_image(rinfo.picture_url);
    	}
    }
	public void endElement(String uri, String name, String qName) throws SAXException { 
		in_tag = tag.none;
		Log.i("funradio",rinfo.author);
		Log.i("funradio",rinfo.track);
	}
	public static Bitmap  get_remote_image (String aURL) {
		try {
			URL url = new URL((aURL).replace(" ", "%20"));
			URLConnection conn = url.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			Bitmap bm = BitmapFactory.decodeStream(bis);
			bis.close();
			is.close();
			return(bm);
		} catch (IOException e) { return null; }
	}
}