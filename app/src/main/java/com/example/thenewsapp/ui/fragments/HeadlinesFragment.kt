package com.example.thenewsapp.ui.fragments

// Remove unused imports like Context, LayoutInflater, Button, TextView, CardView if accessing via binding
import android.os.Bundle
import android.view.View
import android.widget.AbsListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer // Explicit import if needed
import androidx.lifecycle.ViewModelProvider // Preferred way to get ViewModel
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// VERIFY these imports match your project structure
import com.example.newsprojectpractice.R
import com.example.newsprojectpractice.databinding.FragmentHeadlinesBinding
import com.example.newsprojectpractice.databinding.ItemErrorBinding // Generated from item_error.xml

import com.example.thenewsapp.adapters.NewsAdapter
import com.example.thenewsapp.models.Article // Assuming Article is your model
import com.example.thenewsapp.ui.NewsActivity
import com.example.thenewsapp.ui.NewsViewModel
import com.example.thenewsapp.util.Constants
import com.example.thenewsapp.util.Resource


class HeadlinesFragment : Fragment(R.layout.fragment_headlines) {

    private lateinit var newsViewModel: NewsViewModel
    private lateinit var newsAdapter: NewsAdapter
    private var _binding: FragmentHeadlinesBinding? = null
    private val binding get() = _binding!!

    // This will hold the binding for the included item_error.xml
    private var _itemErrorBinding: ItemErrorBinding? = null
    private val itemErrorBinding get() = _itemErrorBinding!!


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHeadlinesBinding.bind(view)

        // Initialize the binding for the included error layout
        // "itemHeadlinesErrorLayout" is the ID of your <include> tag in fragment_headlines.xml
        _itemErrorBinding = binding.itemHeadlinesErrorLayout // This accesses the generated binding for the include

        // Initialize ViewModel
        newsViewModel = ViewModelProvider(requireActivity()).get(NewsViewModel::class.java)
        // Or if you strictly need the instance from NewsActivity:
        // newsViewModel = (activity as NewsActivity).newsViewModel // Ensure 'newsViewModel' casing matches in NewsActivity

        setupHeadlinesRecycler()

        newsAdapter.setOnItemClickListener { article -> // 'it' is the Article object
            val bundle = Bundle().apply {
                putSerializable("article", article)
            }
            // VERIFY this navigation action ID from your nav_graph.xml
            findNavController().navigate(R.id.action_headlinesFragment2_to_articleFragment, bundle)
        }

        newsViewModel.headlines.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success<*> -> {
                    hideProgressBar()
                    hideErrorMessage()
                    response.data?.let { newsResponse ->
                        newsAdapter.differ.submitList(newsResponse.articles?.toList()) // Handle null articles list
                        val totalPages = newsResponse.totalResults / Constants.QUERY_PAGE_SIZE + 2
                        isLastPage = newsViewModel.headlinesPage >= totalPages // Use >= for safety
                        if (isLastPage) {
                            binding.recyclerHeadlines.setPadding(0, 0, 0, 0)
                        }
                    }
                }
                is Resource.Error<*> -> {
                    hideProgressBar()
                    response.message?.let { message ->
                        showErrorMessage("An error occurred: $message")
                    }
                }
                is Resource.Loading<*> -> {
                    if(!isScrolling){ // Only show main progress bar if not paginating
                        showProgressBar()
                        hideErrorMessage() // Hide error when starting to load
                    }
                }
            }
        })

        // Set click listener on the retry button from the included layout's binding
        itemErrorBinding.retryButton.setOnClickListener {
            newsViewModel.getHeadlines("us") // Or your default/selected country
            // Consider hiding the error message immediately on retry for better UX
            hideErrorMessage()
        }

        // Initial data load if the list is empty
        if (newsAdapter.differ.currentList.isEmpty()) {
            newsViewModel.getHeadlines("us")
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
        itemErrorBinding.root.visibility = View.GONE // Use GONE to remove from layout
        isError = false
    }

    private fun showErrorMessage(message: String = "Please check your internet connection") {
        itemErrorBinding.root.visibility = View.VISIBLE
        itemErrorBinding.errorText.text = message // Access errorText via itemErrorBinding
        isError = true
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (dy > 0) { // Check only when scrolling down
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount

                val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
                val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
                val isNotAtBeginning = firstVisibleItemPosition >= 0
                val isTotalMoreThanVisible = totalItemCount >= Constants.QUERY_PAGE_SIZE

                val shouldPaginate = isNotLoadingAndNotLastPage && isAtLastItem &&
                        isNotAtBeginning && isTotalMoreThanVisible &&
                        isScrolling && !isError

                if (shouldPaginate) {
                    newsViewModel.getHeadlines("us") // Or your current country/query
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

    private fun setupHeadlinesRecycler() {
        newsAdapter = NewsAdapter()
        binding.recyclerHeadlines.apply {
            // Corrected: remove extra curly braces
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(requireContext()) // Use requireContext()
            addOnScrollListener(this@HeadlinesFragment.scrollListener)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerHeadlines.adapter = null // Important for RecyclerView cleanup
        _binding = null // Clear binding reference
        _itemErrorBinding = null // Clear included layout binding
    }
}
