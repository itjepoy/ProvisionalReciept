package com.cremcash.provisionalreciept.ui.info;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cremcash.provisionalreciept.LoginActivity;
import com.cremcash.provisionalreciept.R;
import com.cremcash.provisionalreciept.SQLiteHandler;
import com.cremcash.provisionalreciept.SessionManager;
import com.cremcash.provisionalreciept.databinding.FragmentInfoBinding;

public class InfoFragment extends Fragment {

    private FragmentInfoBinding binding;
    private InfoViewModel mViewModel;
    private SessionManager session;
    private SQLiteHandler db;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Use ViewBinding to inflate the layout
        binding = FragmentInfoBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize ViewModel
        mViewModel = new ViewModelProvider(this).get(InfoViewModel.class);

        // Initialize session and DB
        session = new SessionManager(requireContext());
        db = new SQLiteHandler(requireContext());

        // Set up logout button
        binding.btnLogout.setOnClickListener(view -> logoutUser());

        return root;
    }

    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();
        // Launching the login activity
        Intent intent = new Intent(InfoFragment.this.getContext(), LoginActivity.class);
        startActivity(intent);
        this.requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}