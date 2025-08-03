package com.example.thenewsapp.ui

import android.app.Application // Import Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel // Use AndroidViewModel for Application context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.thenewsapp.models.Article
import com.example.thenewsapp.models.NewsResponse
import com.example.thenewsapp.repository.NewsRepository
import com.example.thenewsapp.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

// 1. Inherit from AndroidViewModel and pass Application to the constructor
class NewsViewModel(
    app: Application, // Pass Application to the constructor
    private val newsRepository: NewsRepository // 2. Inject NewsRepository
) : AndroidViewModel(app) { // Inherit from AndroidViewModel

    val headlines: MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    var headlinesPage = 1
    private var headlinesResponse: NewsResponse? = null // Keep this private if only used internally

    val searchNews: MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    var searchNewsPage = 1
    private var searchNewsResponse: NewsResponse? = null // Keep this private
    private var newSearchQuery: String? = null // Keep this private
    private var oldSearchQuery: String? = null // Keep this private

    // Initial data load example (optional, depending on your app's logic)
    // init {
    //     getHeadlines("us") // Example: Load US headlines initially
    // }

    fun getHeadlines(countryCode: String) = viewModelScope.launch {
        headlinesInternet(countryCode)
    }

    fun searchNews(searchQuery: String) = viewModelScope.launch {
        // Reset page for new search if the query is different
        if (newSearchQuery != searchQuery) {
            searchNewsPage = 1
            searchNewsResponse = null // Clear previous results for a new query
        }
        newSearchQuery = searchQuery // Update current search query
        searchNewsInternet(searchQuery)
    }

    private fun handleHeadlinesResponse(response: Response<NewsResponse>): Resource<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                headlinesPage++
                if (headlinesResponse == null) {
                    headlinesResponse = resultResponse
                } else {
                    val oldArticles = headlinesResponse?.articles
                    val newArticles = resultResponse.articles
                    // Ensure articles lists are mutable and handle nulls safely
                    if (oldArticles is ArrayList) { // Or ensure it's a MutableList
                        oldArticles.addAll(newArticles)
                    } else { // Fallback if headlinesResponse.articles was not mutable or null
                        headlinesResponse = resultResponse // Or handle more gracefully
                    }
                }
                return Resource.Success(headlinesResponse ?: resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    private fun handleSearchNewsResponse(response: Response<NewsResponse>): Resource<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                // Logic for resetting page is now partly in searchNews function for clarity
                if (searchNewsResponse == null || newSearchQuery != oldSearchQuery) {
                    // searchNewsPage is reset in searchNews() if query changes
                    oldSearchQuery = newSearchQuery
                    searchNewsResponse = resultResponse
                } else {
                    searchNewsPage++
                    val oldArticles = searchNewsResponse?.articles
                    val newArticles = resultResponse.articles
                    if (oldArticles is ArrayList) { // Or ensure it's a MutableList
                        oldArticles.addAll(newArticles)
                    } else {
                        searchNewsResponse = resultResponse
                    }
                }
                return Resource.Success(searchNewsResponse ?: resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    fun addToFavourites(article: Article) = viewModelScope.launch {
        newsRepository.upsert(article)
    }

    fun getFavouriteNews() = newsRepository.getFavouriteNews() // This returns LiveData, which is fine

    fun deleteArticle(article: Article) = viewModelScope.launch {
        newsRepository.deleteArticle(article)
    }

    // 3. Corrected internetConnection check
    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private suspend fun headlinesInternet(countryCode: String) {
        headlines.postValue(Resource.Loading())
        try {
            if (hasInternetConnection()) { // Use the corrected method
                val response = newsRepository.getHeadlines(countryCode, headlinesPage)
                headlines.postValue(handleHeadlinesResponse(response))
            } else {
                headlines.postValue(Resource.Error("No internet connection"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> headlines.postValue(Resource.Error("Network Failure: ${t.message}"))
                else -> headlines.postValue(Resource.Error("Conversion Error: ${t.message}"))
            }
        }
    }

    private suspend fun searchNewsInternet(searchQuery: String) {
        // newSearchQuery is already set in the public searchNews function
        searchNews.postValue(Resource.Loading())
        try {
            if (hasInternetConnection()) { // Use the corrected method
                val response = newsRepository.searchNews(searchQuery, searchNewsPage)
                searchNews.postValue(handleSearchNewsResponse(response))
            } else {
                searchNews.postValue(Resource.Error("No internet connection"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> searchNews.postValue(Resource.Error("Network Failure: ${t.message}"))
                else -> searchNews.postValue(Resource.Error("Conversion Error: ${t.message}"))
            }
        }
    }
}
