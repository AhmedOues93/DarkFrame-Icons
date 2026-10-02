package com.darkframe.icons.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.darkframe.icons.data.DarkFrameCatalog

class StylesActivity:AppCompatActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(40,56,40,40);setBackgroundColor(0xFF090B0E.toInt())}
  root.addView(TextView(this).apply{text="Complete Looks";textSize=30f;setTextColor(0xFFF4F4F4.toInt())})
  root.addView(TextView(this).apply{text="Choose a visual system, then apply each supported component.";textSize=14f;setTextColor(0xFF9EA3AD.toInt());setPadding(0,8,0,22)})
  DarkFrameCatalog.styles.forEach{s->root.addView(TextView(this).apply{text=s.title+(if(s.tier.name=="PRO")"  • PRO" else "  • FREE");textSize=20f;setTextColor(0xFFF0F0F0.toInt());setPadding(0,24,0,24)})}
  setContentView(ScrollView(this).apply{addView(root)})
 }
}
