package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerLoginAdapter extends FragmentStateAdapter {
    public ViewPagerLoginAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position==0){
            return new SignUpFragment();
        }else{
            return new SigninFragment();
        }

    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
