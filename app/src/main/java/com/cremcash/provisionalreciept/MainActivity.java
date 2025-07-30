package com.cremcash.provisionalreciept;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.cremcash.provisionalreciept.databinding.ActivityMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fab = findViewById(R.id.fab);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_info)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.navigation_dashboard) {
                binding.fab.setVisibility(View.VISIBLE);
                // Optional: bring nav bar back in case it's hidden from before
                binding.bottomNavCard.animate()
                        .translationY(0f)
                        .setDuration(300)
                        .start();
            } else {
                binding.fab.setVisibility(View.GONE);
                // REMOVE THIS: it hides the nav bar too
                // binding.bottomNavCard.animate()
                //         .translationY(binding.bottomNavCard.getHeight() + 100)
                //         .setDuration(300)
                //         .start();
            }
        });



        // Handle FAB click
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReceiptActivity.class);
            startActivityForResult(intent, 1001);
        });

    }

}