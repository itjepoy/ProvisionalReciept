package com.cremcash.provisionalreciept.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.cremcash.provisionalreciept.DashboardPagerAdapter;
import com.cremcash.provisionalreciept.R;
import com.cremcash.provisionalreciept.ReceiptActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class DashboardFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        DashboardPagerAdapter adapter = new DashboardPagerAdapter(getActivity());
        viewPager.setAdapter(adapter);
        viewPager.setPageTransformer((page, position) -> {
            float scale = 1 - Math.abs(position) * 0.2f;
            page.setScaleX(scale);
            page.setScaleY(scale);
            page.setAlpha(0.5f + (1 - Math.abs(position)) * 0.5f);
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Database");
                tab.setIcon(R.drawable.ic_database); // your drawable
            } else {
                tab.setText("Local");
                tab.setIcon(R.drawable.ic_excel); // your drawable
            }
        }).attach();

        return view;
    }

}
