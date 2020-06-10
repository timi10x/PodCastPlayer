package com.example.podplayer.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.SearchEvent
import android.view.View
import android.widget.SearchView
import com.example.podplayer.R
import com.example.podplayer.repository.ItunesRepo
import com.example.podplayer.service.ItunesService
import timber.log.Timber

class PodcastActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)

    }

    private fun performSearch(term : String){
        val itunesService = ItunesService.instance
        val itunsRepo = ItunesRepo(itunesService)

        itunsRepo.searchByTerm(term) {
            Timber.tag(TAG).i("Results = $it")
        }
    }

    private fun handleIntent(intent: Intent){
        if (Intent.ACTION_SEARCH == intent.action){
            val query = intent.getStringExtra(SearchManager.QUERY)
            performSearch(query)
        }
    }


    //this method allows for updated intents to be received
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent!!)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)
        val searchMenuItem = menu!!.findItem(R.id.search_item)
        val searchView = searchMenuItem?.actionView as SearchView

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        searchView.setSearchableInfo(
            searchManager.getSearchableInfo(componentName)
        )
        return true
    }
    companion object{
         val TAG = javaClass.simpleName
    }
}