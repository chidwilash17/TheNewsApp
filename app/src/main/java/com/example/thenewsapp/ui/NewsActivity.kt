package com.example.thenewsapp.ui

import android.os.Bundle
// import androidx.activity.enableEdgeToEdge // You can keep or remove if not actively using edge-to-edge
import androidx.appcompat.app.AppCompatActivity
// import androidx.core.view.ViewCompat // Keep or remove based on usage
// import androidx.core.view.WindowInsetsCompat // Keep or remove based on usage
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.newsprojectpractice.R // Ensure this R is correct for your main module
import com.example.newsprojectpractice.databinding.ActivityNewsBinding // Correct import
// import com.example.thenewsapp.adapters.NewsAdapter // Not used in this Activity directly
import com.example.thenewsapp.db.ArticleDatabase
import com.example.thenewsapp.repository.NewsRepository

class NewsActivity : AppCompatActivity() {

    lateinit var newsviewModel: NewsViewModel // Consider renaming to 'viewModel' for convention
    // and making it private if only used within this class

    private lateinit var binding: ActivityNewsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Call this if you intend to use edge-to-edge display

        // ** CORRECT WAY TO INITIALIZE VIEW BINDING **
        binding = ActivityNewsBinding.inflate(layoutInflater)
        setContentView(binding.root) // Use the root of the binding object

        // Now 'binding' is initialized and can be used safely

        val newsRepository = NewsRepository(ArticleDatabase(this))
        val viewModelProviderFactory = NewsViewModelProviderFactory(application, newsRepository)
        newsviewModel = ViewModelProvider(this, viewModelProviderFactory).get(NewsViewModel::class.java)

        // Ensure R.id.newsNavHostFragment exists in your activity_news.xml
        // and is a NavHostFragment or FragmentContainerView for navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.newsNavHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Ensure binding.bottomNavigationView corresponds to an ID in your activity_news.xml
        // (e.g., android:id="@+id/bottomNavigationView" in your XML)
        binding.bottomNavigationView.setupWithNavController(navController)

        // Example of handling window insets if you keep enableEdgeToEdge
        // ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content_id_from_xml)) { v, insets ->
        //     val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        //     v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        //     insets
        // }
    }
}

