package com.cse.coari.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cse.coari.R
import com.cse.coari.adapter.NewsRecyclerAdapter
import com.cse.coari.data.GetNewsDTO
import com.cse.coari.data.GetNewsDTOItem
import com.cse.coari.data.NewsDTO
import com.cse.coari.retrofit.RetrofitBuilder
import kotlinx.android.synthetic.main.activity_news.*
import kotlinx.android.synthetic.main.activity_notice.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class NewsActivity : AppCompatActivity() {

    private lateinit var newsList: GetNewsDTO
    private val searchList: GetNewsDTO = GetNewsDTO()
    private lateinit var adapter: NewsRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        val functionName = object {}.javaClass.enclosingMethod.name
        Log.i(TAG, "$functionName : Activity Start")

        RetrofitBuilder.api.getNews().enqueue(object : Callback<GetNewsDTO>{
            override fun onResponse(call: Call<GetNewsDTO>, response: Response<GetNewsDTO>) {
                if (response.isSuccessful){
                    newsList = response.body()!!
                    searchList.addAll(newsList)

                    adapter = NewsRecyclerAdapter(this@NewsActivity, newsList)
                    news_recycler.adapter = adapter

                    val layout = LinearLayoutManager(this@NewsActivity)
                    news_recycler.layoutManager = layout
                    news_recycler.setHasFixedSize(true)
                }
            }

            override fun onFailure(call: Call<GetNewsDTO>, t: Throwable) {
                Log.i(TAG, "$functionName : GET News Data fail")
            }

        })

        news_edit_text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.i(NoticeActivity.TAG, "beforeTextChanged")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.i(NoticeActivity.TAG, "onTextChanged")
            }

            override fun afterTextChanged(s: Editable?) {
                val text = news_edit_text.text.toString()
                search(text)
            }
        })

        val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        news_edit_text.setOnEditorActionListener{ textView, actionId, event ->
            var handled = false

            val isEnterEvent = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER
            val isEnterUpEvent = isEnterEvent && event?.action == KeyEvent.ACTION_UP

            if (actionId == EditorInfo.IME_ACTION_SEARCH || isEnterUpEvent) {
                imm.hideSoftInputFromWindow(news_edit_text.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                search(textView.text.toString())
                news_edit_text.apply {
                    clearFocus()
                    isFocusable = false
                    isFocusableInTouchMode = true
                    isFocusable = true
                }
                handled = true
            }
            handled
        }

    }

    override fun onBackPressed() {
        if(news_edit_text.hasFocus()){
            news_edit_text.apply{
                clearFocus()
                isFocusable = false
                setText("")
                isFocusableInTouchMode = true
                isFocusable = true
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun search(text: String){
        newsList.clear()

        if(text.isEmpty()){
            newsList.addAll(searchList)
        } else {
            for(i in searchList.indices step (1)){
                if(searchList[i].title.toLowerCase(Locale.ROOT).contains(text)){
                    newsList.add(searchList[i])
                }
            }
        }

        adapter.notifyDataSetChanged()
    }

    companion object{
        const val TAG = "NewsActivity"
    }
}