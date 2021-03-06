package com.aliyun.ai.viapi.activty;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.aliyun.ai.viapi.R;
import com.aliyun.ai.viapi.VIAPISdkApp;
import com.aliyun.ai.viapi.ui.fragment.HomeFragment;
import com.aliyun.ai.viapi.ui.fragment.HumanSegmentFragment;
import com.aliyun.ai.viapi.ui.fragment.ImageSegmentFragment;
import com.aliyun.ai.viapi.ui.fragment.OnFragmentCallback;
import com.aliyun.ai.viapi.ui.homepage.HomeTabPageId;

public class MainHomeActivity extends AppCompatActivity implements OnFragmentCallback {
    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_home);
        switchFragment(HomeFragment.newInstance("", ""), true);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public void openTabPage(HomeTabPageId pageId) {
        switch (pageId) {
            case HUMAN_SEGMENT_PAGE:
                switchFragment(HumanSegmentFragment.newInstance("", ""), true);
                break;
            case PICTRRE_SEGMENT_PAGE:
                switchFragment(ImageSegmentFragment.newInstance("", ""), true);
                break;
            case FACEBEAUTY_PAGE:
            case FACE_LANDMASK_PAGE:
            case HUMAN_BODY_LANDMASK_PAGE:
            default:
                Toast.makeText(VIAPISdkApp.getContext(),
                        "ζδΈζ―ζ... ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void bannerClick(int position) {

    }

    private void switchFragment(Fragment fragment, boolean isVAnim) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (isVAnim) {
            transaction.setCustomAnimations(
                    R.anim.slide_left_in,
                    R.anim.slide_left_out
            );
        }
        if (fragment instanceof HomeFragment) {
            transaction.replace(R.id.fragment_host, fragment).commitAllowingStateLoss();
        } else {
            transaction.replace(R.id.fragment_host, fragment).addToBackStack("").commitAllowingStateLoss();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FragmentManager manager = getSupportFragmentManager();
        for (int i = 0; i < manager.getFragments().size(); i++) {
            Fragment fragment = manager.getFragments().get(i);
            if (fragment != null && fragment instanceof ImageSegmentFragment) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }
        super.onBackPressed();
    }
}