package com.example.podplayer.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.podplayer.R
import com.example.podplayer.adapter.PodcastListAdapter
import com.example.podplayer.repository.ItunesRepo
import com.example.podplayer.repository.PodcastRepo
import com.example.podplayer.service.ItunesService
import com.example.podplayer.viewmodel.PodcastViewModel
import com.example.podplayer.viewmodel.SearchViewModel
import kotlinx.android.synthetic.main.activity_podcast.*
import timber.log.Timber


class PodcastActivity : AppCompatActivity(),
    PodcastListAdapter.PodcastListAdapterListener {
    lateinit var searchViewModel: SearchViewModel
    lateinit var podcastListAdapter: PodcastListAdapter
    lateinit var searchMenuItem: MenuItem
    lateinit var podcastViewModel: PodcastViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setupToolbar()
        setupViewModels()
        podcastViewModel = ViewModelProviders.of(this)
            .get(PodcastViewModel::class.java)
        podcastViewModel.podcastRepo = PodcastRepo()
        updateControls()
        handleIntent(intent)
        addBackStackListener()
    }

    private fun performSearch(term: String) {
        showProgressBar()
        searchViewModel.searchPodcasts(term) { results ->
            hideProgressBar()
            toolbar.title = getString(R.string.search_results)
            podcastListAdapter.setSearchData(results)
        }
    }

    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        val feedUrl = podcastSummaryViewData.feedUrl?:return
        showProgressBar()
        podcastViewModel.getPodcast(podcastSummaryViewData){
            hideProgressBar()
            if (it !=null){
                showDetailsFragment()
            }else{
                showError("Error loading feed $feedUrl")
            }
        }
    }

    private fun addBackStackListener(){
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0){
                podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            performSearch(query!!)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun updateControls() {
        podcastRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        podcastRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            podcastRecyclerView.context,
            layoutManager.orientation
        )
        podcastRecyclerView.addItemDecoration(dividerItemDecoration)
        podcastListAdapter = PodcastListAdapter(null, this, this)
        podcastRecyclerView.adapter = podcastListAdapter

    }

    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {
        var podcastDetailsFragment = supportFragmentManager
            .findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?

        if (podcastDetailsFragment == null) {
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }
        return podcastDetailsFragment
    }

    private fun setupViewModels() {
        val service = ItunesService.instance
        searchViewModel = ViewModelProviders.of(this).get(
            SearchViewModel::class.java
        )
        searchViewModel.itunesRepo = ItunesRepo(service)
    }

    private fun showDetailsFragment() {
        val podcastDetailsFragment = createPodcastDetailsFragment()
        supportFragmentManager.beginTransaction().add(
            R.id.podcastDetailsContainer, podcastDetailsFragment, TAG_DETAILS_FRAGMENT
        ).addToBackStack("DetailsFragment").commit()
        podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    //this method allows for updated intents to be received
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent!!)
    }

    private fun showError(message: String){
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)
        searchMenuItem = menu!!.findItem(R.id.search_item)
        val searchView = searchMenuItem.actionView as SearchView

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        searchView.setSearchableInfo(
            searchManager.getSearchableInfo(componentName)
        )
        if (supportFragmentManager.backStackEntryCount>0){
            podcastRecyclerView.visibility = View.INVISIBLE
        }
        if (podcastRecyclerView.visibility == View.INVISIBLE){
            searchMenuItem.isVisible = false
        }
        return true
    }

    companion object {
        val TAG = javaClass.simpleName
        val TAG_DETAILS_FRAGMENT = "DetailsFragment"
    }
}