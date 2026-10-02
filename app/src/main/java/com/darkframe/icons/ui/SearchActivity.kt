package com.darkframe.icons.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.darkframe.icons.data.DarkFrameCatalog

class SearchActivity:AppCompatActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(40,56,40,40);setBackgroundColor(0xFF090B0E.toInt())}
        val input=EditText(this).apply{hint="Search icons";setTextColor(0xFFF4F4F4.toInt());setHintTextColor(0xFF777B82.toInt())}
        val results=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        fun render(q:String){results.removeAllViews();DarkFrameCatalog.search(q).forEach{item->results.addView(TextView(this).apply{text=item.label;textSize=18f;setPadding(8,22,8,22);setTextColor(0xFFF4F4F4.toInt())})}}
        input.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int)=render(s?.toString()?:"");override fun afterTextChanged(s:Editable?) {}})
        root.addView(input);root.addView(ScrollView(this).apply{addView(results)});setContentView(root);render("")
    }
}
