package com.mjdev.fun_radio.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.util.Log;

public class M3uParser implements PlaylistParser {
  private final BufferedReader reader;
  public M3uParser(File file) throws FileNotFoundException {
    this.reader = new BufferedReader(new FileReader(file), 16384);
  }
  @Override
  public String getNextUrl() {
    String url = "";
    while (true) {
      try {
        url = reader.readLine();
        if (url == null || isValidLine(url)) break;
      } catch (IOException e) { e.printStackTrace(); }
    }
    Log.i("m3uparser",url);
    return url;
  }
  private boolean isValidLine(String line) {
    String trimmed = line.trim();
    return trimmed.length() > 0 && trimmed.charAt(0) != '#' && trimmed.charAt(0) != '<';
  }
}