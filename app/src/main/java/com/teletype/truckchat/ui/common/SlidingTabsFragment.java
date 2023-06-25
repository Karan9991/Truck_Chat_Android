package com.teletype.truckchat.ui.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.android.common.view.SlidingTabLayout;
import com.teletype.truckchat.R;
import com.teletype.truckchat.ui.conversations.ConversationsFragment;
import com.teletype.truckchat.ui.conversations.starred.StarredConversationsFragment;
import com.teletype.truckchat.ui.news.NewsFragment;
import com.teletype.truckchat.ui.settings.DialogAvatarPreference;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;

import java.lang.ref.WeakReference;


public class SlidingTabsFragment extends Fragment {

    private String[] mTitles;
    private SlidingTabLayout mSlidingTabLayout;
    private ViewPager mViewPager;

    public static SlidingTabsFragment newInstance(Bundle args) {

        SlidingTabsFragment fragment = new SlidingTabsFragment();
        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sliding_tabs, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mViewPager = (ViewPager) view.findViewById(R.id.slidingTabsFragment_viewpager);

        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.slidingTabsFragment_slidingTabLayout);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        mTitles = activity.getResources().getStringArray(R.array.tab_titles);
        mViewPager.setAdapter(new MyPagerAdapter(getFragmentManager()));
        mSlidingTabLayout.setViewPager(mViewPager);
        mViewPager.setOffscreenPageLimit(3);
        if (Utils.isFirstRun(activity)) {
            mViewPager.setCurrentItem(2);
        } else {
            mViewPager.setCurrentItem(1);
            checkNickname();
        }

        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 2) {
                    MyPagerAdapter adapter = (MyPagerAdapter) mViewPager.getAdapter();
                    if (adapter != null) {
                        ConversationsFragment.InterWebFragment fragment = (ConversationsFragment.InterWebFragment) adapter.getRegisteredFragment(position);
                        if (fragment != null) {
                            fragment.reloadUrl();
                        }
                    }
                }
                if (position == 4) {
                    MyPagerAdapter adapter = (MyPagerAdapter) mViewPager.getAdapter();
                    if (adapter != null) {
                        NewsFragment fragment = (NewsFragment) adapter.getRegisteredFragment(position);
                        if (fragment != null) {
                            fragment.makeRequest();
                        }
                    }
                }
                if ( position == 1) {
                    checkNickname();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public void getConversationsFragment(int position) {
        MyPagerAdapter adapter = (MyPagerAdapter) mViewPager.getAdapter();
        if (adapter != null) {
            ConversationsFragment fragment = (ConversationsFragment) adapter.getRegisteredFragment(1);
            if (fragment != null) {
                fragment.onListItemClick(null, null, position, 0);
            }
        }
    }

    public void actionSelectAll() {
        MyPagerAdapter adapter = (MyPagerAdapter) mViewPager.getAdapter();
        ConversationsFragment fragment = (ConversationsFragment) adapter.getRegisteredFragment(1);
        if (fragment != null) {
            mViewPager.setCurrentItem(1);
            fragment.actionSelectAll();
        }
    }

    public boolean onBackPressed() {
        if (mViewPager.getCurrentItem() == 3) {
            MyPagerAdapter adapter = (MyPagerAdapter) mViewPager.getAdapter();
            ConversationsFragment.InterWebFragment fragment = (ConversationsFragment.InterWebFragment) adapter.getRegisteredFragment(3);
            if (fragment.goBack()) {
                return false;
            }
        } else if (mViewPager.getCurrentItem() == 1) {
            if (Utils.getAvatar(getContext()) == -1) {
                checkAvatar();
            } else {
                return true;
            }
        }

        return true;
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        private final SparseArray<WeakReference<Fragment>> registeredFragments;

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);

            registeredFragments = new SparseArray<>();
        }

        @Override
        public int getCount() {
            return mTitles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: // Starred
                    return StarredConversationsFragment.newInstance(null);

                case 1: // All Chats
                    return ConversationsFragment.newInstance(null);

                case 2: // Sponsors
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.EXTRA_URI, Constants.WEBPAGE_SPONSORS);
                    bundle.putBoolean(Constants.EXTRA_OPEN_IN_NEW_WINDOW, true);
                    return ConversationsFragment.InterWebFragment.newInstance(bundle);

                case 3: // Trucking Company Reviews
                    Bundle bundle2 = new Bundle();
                    bundle2.putString(Constants.EXTRA_URI, Constants.WEBPAGE_REVIEWS);
                    bundle2.putBoolean(Constants.EXTRA_OPEN_IN_NEW_WINDOW, false);
                    return ConversationsFragment.InterWebFragment.newInstance(bundle2);

                case 4: // News
                    return NewsFragment.newInstance(null);
            }

            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, new WeakReference<>(fragment));
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);

            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            WeakReference<Fragment> weakFragment = registeredFragments.get(position);
            if (weakFragment != null) {
                return weakFragment.get();
            }

            return null;
        }
    }

    private void checkNickname() {
        final SharedPreferences sharedPref = Utils.getSharedPreferences(getContext());
        String userName = sharedPref.getString(Constants.PREFS_MSG_HANDLE, "");

        final EditText edittext = new EditText(getContext());

        if (userName.isEmpty()) {
            final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext())
                    .setCancelable(false)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int length = edittext.getText().length();
                            int start = 0;

                            if (length > 0) {
                                CharSequence nickename = edittext.getText().subSequence(start, length);

                                sharedPref.edit()
                                        .putString(Constants.PREFS_MSG_HANDLE, String.valueOf(nickename))
                                        .apply();
                                dialog.dismiss();
                                checkAvatar();
                            } else {
                                checkNickname();
                            }
                        }
                    });
            alertBuilder.setTitle("Chat Handle");
            alertBuilder.setView(edittext);

            alertBuilder.show();
        } else {
            checkAvatar();
        }
    }

    private void checkAvatar() {
        if (Utils.getAvatar(getContext()) == -1) {
            getAvatar();
        }
    }

    private void getAvatar() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(DialogAvatarPreference.IS_CLOSE_VISIBLE, false);
        android.app.FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        DialogAvatarPreference dialogAvatarPreference = new DialogAvatarPreference();
        dialogAvatarPreference.setArguments(bundle);
        ft.add(R.id.conversationsActivity_frameLayout_content, dialogAvatarPreference);
        ft.addToBackStack(DialogAvatarPreference.class.getName());
        ft.commit();
    }
}
