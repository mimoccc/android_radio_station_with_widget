package com.mjdev.fun_radio.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class PlsParser implements PlaylistParser {
  private final BufferedReader reader;
  public PlsParser(File file) throws FileNotFoundException {
    this.reader = new BufferedReader(new FileReader(file), 1024);
  }
  @Override
  public String getNextUrl() {
    String url = "";
    while (true) {
      try {
        url = parseLine(reader.readLine());
        if (url == null || !url.equals("")) break;
      } catch (IOException e) { e.printStackTrace(); }
    }
    return url;
  }
  private String parseLine(String line) {
    if (line == null) return null;
    String trimmed = line.trim();
    if (trimmed.indexOf("http") >= 0) return trimmed.substring(trimmed.indexOf("http"));
    return "";
  }
}
