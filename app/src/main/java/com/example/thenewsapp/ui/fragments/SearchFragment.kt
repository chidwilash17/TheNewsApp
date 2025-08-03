package com.example.thenewsapp.ui.fragments

// import android.R.attr.editable // This import is problematic and likely not needed directly
import android.os.Bundle
import android.text.Editable // Use this for addTextChangedListener
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.AbsListView
// Unused imports:
// import android.content.Context
// import android.view.LayoutInflater
// import android.view.ViewGroup
// import android.widget.Button // Access via binding
// import android.widget.TextView // Access via binding
// import androidx.cardview.widget.CardView // Access via binding

import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider // Preferred way to get ViewModel
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// VERIFY YOUR R CLASS AND BINDING IMPORTS
import com.example.newsprojectpractice.R
import com.example.newsprojectpractice.databinding.FragmentSearchBinding
// ItemErrorBinding will be generated if item_error.xml has a <layout> tag
import com.example.newsprojectpractice.databinding.ItemErrorBinding

import com.example.thenewsapp.adapters.NewsAdapter
import com.example.thenewsapp.ui.NewsActivity
import com.example.thenewsapp.ui.NewsViewModel
import com.example.thenewsapp.util.Constants
import com.example.thenewsapp.util.Constants.Companion.SEARCH_NEWS_TIME_DELAY
import com.example.thenewsapp.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment(R.layout.fragment_search) { // Ensure R.layout.fragment_search is correct

    private lateinit var newsViewModel: NewsViewModel
    private lateinit var newsAdapter: NewsAdapter
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // This will hold the binding for the included item_error.xml
    private var _itemErrorBinding: ItemErrorBinding? = null
    private val itemErrorBinding get() = _itemErrorBinding!!


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)

        // Initialize the binding for the included error layout
        // 'itemSearchErrorLayout' should be the ID of your <include> tag in fragment_search.xml
        _itemErrorBinding = binding.itemSearchErrorLayout


        // Initialize ViewModel
        newsViewModel = ViewModelProvider(requireActivity()).get(NewsViewModel::class.java)
        // newsViewModel = (activity as NewsActivity).newsViewModel // Alternative if NewsActivity holds the instance directly

        setupSearchRecycler() // Initializes newsAdapter

        newsAdapter.setOnItemClickListener { article -> // Give 'it' a meaningful name
            val bundle = Bundle().apply {
                putSerializable("article", article)
            }
            // VERIFY your navigation action ID
            findNavController().navigate(R.id.action_searchFragment2_to_articleFragment, bundle)
        }

        var job: Job? = null
        // Correct usage of addTextChangedListener lambda
        binding.searchEdit.addTextChangedListener { text: Editable? ->
            job?.cancel()
            job = MainScope().launch {
                delay(SEARCH_NEWS_TIME_DELAY)
                text?.let {
                    if (it.toString().isNotEmpty()) {
                        newsViewModel.searchNews(it.toString())
                    }
                }
            }
        }

        newsViewModel.searchNews.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success<*> -> {
                    hideProgressBar()
                    hideErrorMessage()
                    response.data?.let { newsResponse ->
                        newsAdapter.differ.submitList(newsResponse.articles.toList())
                        val totalPages = newsResponse.totalResults / Constants.QUERY_PAGE_SIZE + 2 // +1 for int div, +1 for 0-index
                        isLastPage = newsViewModel.searchNewsPage >= totalPages // Use >= for safety
                        if (isLastPage) {
                            binding.recyclerSearch.setPadding(0, 0, 0, 0)
                        }
                    }
                }
                is Resource.Error<*> -> {
                    hideProgressBar()
                    response.message?.let { message ->
                        showErrorMessage("An error occurred: $message") // Pass message
                    }
                }
                is Resource.Loading<*> -> {
                    if(!isScrolling) { // Only show main progress if not paginating
                        showProgressBar()
                        hideErrorMessage()
                    }
                }
            }
        })

        // Set click listener on the retry button from the included layout
        itemErrorBinding.retryButton.setOnClickListener {
            if (binding.searchEdit.text.toString().isNotEmpty()) {
                newsViewModel.searchNews(binding.searchEdit.text.toString())
            } else {
                hideErrorMessage() // Or show a message to enter search term
            }
        }
    }

    private var isError = false
    private var isLoading = false
    private var isLastPage = false
    private var isScrolling = false

    private fun hideProgressBar() {
        binding.paginationProgressBar.visibility = View.INVISIBLE
        isLoading = false
    }

    private fun showProgressBar() {
        binding.paginationProgressBar.visibility = View.VISIBLE
        isLoading = true
    }

    private fun hideErrorMessage() {
        itemErrorBinding.root.visibility = View.GONE // Use GONE for the included layout's root
        isError = false
    }

    private fun showErrorMessage(message: String = "Please check your internet connection") {
        itemErrorBinding.root.visibility = View.VISIBLE
        itemErrorBinding.errorText.text = message // Access errorText via itemErrorBinding
        isError = true
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() { // Make it private val
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (dy > 0) { // Only check when scrolling down
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount

                val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
                val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
                val isNotAtBeginning = firstVisibleItemPosition >= 0
                val isTotalMoreThanVisible = totalItemCount >= Constants.QUERY_PAGE_SIZE

                val shouldPaginate = isNotLoadingAndNotLastPage && isAtLastItem &&
                        isNotAtBeginning && isTotalMoreThanVisible && isScrolling && !isError

                if (shouldPaginate) {
                    newsViewModel.searchNews(binding.searchEdit.text.toString())
                    isScrolling = false // Reset after initiating load
                }
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                isScrolling = true
            }
        }
    }

    private fun setupSearchRecycler() {
        newsAdapter = NewsAdapter() // Initialize adapter
        binding.recyclerSearch.apply {
            // Remove extra curly braces here
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(activity) // or requireContext()
            addOnScrollListener(this@SearchFragment.scrollListener)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerSearch.adapter = null // Important for RecyclerView cleanup
        _binding = null // Clear binding reference
        _itemErrorBinding = null // Clear included layout binding
    }
}
