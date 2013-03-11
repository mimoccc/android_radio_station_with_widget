package com.mjdev.fun_radio;

import com.mjdev.fun_radio.R;
import android.app.ActivityGroup;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;

public class fun_radio extends ActivityGroup {
  private enum MenuId {
    CLOSE,
    ABOUT
  }
public static final String EXTRA_TITLE = "";
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    this.getWindow().setFlags(
    		WindowManager.LayoutParams.FLAG_FULLSCREEN,
    		WindowManager.LayoutParams.FLAG_FULLSCREEN );
    setVolumeControlStream(AudioManager.STREAM_MUSIC);
    setContentView(R.layout.main);
    Intent i = new Intent(this, ListenActivity.class);
    i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    Window w = getLocalActivityManager().startActivity(ListenActivity.class.getName(), i);
    View v = w.getDecorView();
    ((ViewGroup) findViewById(R.id.MediaPlayer)).addView(v, new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(Menu.NONE, MenuId.CLOSE.ordinal(), Menu.NONE, "Close")
        .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
        .setAlphabeticShortcut('c');
    menu.add(Menu.NONE, MenuId.ABOUT.ordinal(), Menu.NONE,
        R.string.msg_main_menu_about).setIcon(android.R.drawable.ic_menu_help)
        .setAlphabeticShortcut('e');
    return (super.onCreateOptionsMenu(menu));
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == MenuId.CLOSE.ordinal()) {
      finish();
      return true;
    } else if (item.getItemId() == MenuId.ABOUT.ordinal()) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
  @Override
  protected void onDestroy() { super.onDestroy();  }
}
