package com.cremcash.provisionalreciept;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.cremcash.provisionalreciept.ui.dashboard.DatabaseItemFragment;

public class DashboardPagerAdapter extends FragmentStateAdapter {

    public DashboardPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new DatabaseItemFragment();
        else return new ExcelItemFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}


