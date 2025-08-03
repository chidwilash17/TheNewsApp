package com.example.thenewsapp.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
// TODO: Verify this R class. If ArticleFragment is in 'com.example.thenewsapp' module,
// it should ideally be 'com.example.thenewsapp.R'.
import com.example.newsprojectpractice.R
// TODO: Verify this databinding class name. If layout is fragment_article.xml,
// FragmentArticleBinding is usually correct for the module it resides in.
import com.example.newsprojectpractice.databinding.FragmentArticleBinding
import com.example.thenewsapp.models.Article // Ensure this path is correct
import com.example.thenewsapp.ui.NewsActivity
import com.example.thenewsapp.ui.NewsViewModel
import com.google.android.material.snackbar.Snackbar

class ArticleFragment : Fragment(R.layout.fragment_article) {

     lateinit var newsViewModel: NewsViewModel
     val args: ArticleFragmentArgs by navArgs() // This should now work
     lateinit var binding: FragmentArticleBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentArticleBinding.bind(view)
        newsViewModel = (activity as NewsActivity).newsviewModel
        val article=args.article
        binding.webView.apply{
            webViewClient= WebViewClient()
            article.url?.let { loadUrl(it) }
        }






        binding.fab.setOnClickListener {
            // viewModel should be initialized if we've reached this point due to earlier checks
            newsViewModel.addToFavourites(article) // 'article' is non-null here
            Snackbar.make(view, "Article saved successfully", Snackbar.LENGTH_SHORT).show()
        }
    }
}

