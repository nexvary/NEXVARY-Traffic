package com.nexvary.trafficlab;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.Button;

/** Bundled licensed multicolor PNG assets, never theme-tinted or emoji glyphs. */
public final class LabIcons {
    private LabIcons(){}
    public static int resource(int old){
        if(old==R.drawable.ic_dashboard)return R.drawable.color_home;
        if(old==R.drawable.ic_back)return R.drawable.color_back;
        if(old==R.drawable.ic_website)return R.drawable.color_globe;
        if(old==R.drawable.ic_email)return R.drawable.color_folder;
        if(old==R.drawable.ic_language)return R.drawable.color_globe;
        if(old==R.drawable.ic_lock)return R.drawable.color_lock;
        if(old==R.drawable.ic_history)return R.drawable.color_folder;
        if(old==R.drawable.ic_about)return R.drawable.color_info;
        if(old==R.drawable.ic_shield)return R.drawable.color_shield;
        if(old==R.drawable.ic_radar)return R.drawable.color_network;
        if(old==R.drawable.ic_intercept)return R.drawable.color_search;
        if(old==R.drawable.ic_repeater)return R.drawable.color_play;
        if(old==R.drawable.ic_flow)return R.drawable.color_chart;
        return old;
    }
    public static void button(Button button,int icon){
        if(icon==0)return;Context c=button.getContext();Drawable d=c.getDrawable(resource(icon));int size=(int)(24*c.getResources().getDisplayMetrics().density+.5f);d.setBounds(0,0,size,size);
        button.setCompoundDrawableTintList(null);button.setCompoundDrawables(d,null,null,null);button.setCompoundDrawablePadding(size/3);
    }
    public static void insets(android.view.View root){
        if(android.os.Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets bars=i.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return i;});else root.setFitsSystemWindows(true);
    }
    public static void tagged(Button button,String tag){
        int icon=tag.contains("back")?R.drawable.color_back:tag.contains("stop")||tag.contains("drop")||tag.contains("clear")?R.drawable.color_stop:
            tag.contains("export")||tag.contains("archive")||tag.contains("save")?R.drawable.color_folder:tag.contains("certificate")?R.drawable.color_shield:
            tag.contains("vpn")?R.drawable.color_network:tag.contains("settings")?R.drawable.color_gear:tag.contains("start")||tag.contains("lesson")||tag.contains("test")||tag.contains("forward")?R.drawable.color_play:R.drawable.color_arrow;
        button(button,icon);
    }
}
