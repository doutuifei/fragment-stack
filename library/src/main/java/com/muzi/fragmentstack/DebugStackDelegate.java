package com.muzi.fragmentstack;

import android.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

public class DebugStackDelegate {
    private FragmentActivity mActivity;
    private AlertDialog mStackDialog;

    public DebugStackDelegate(FragmentActivity activity) {
        this.mActivity = activity;
    }

    public void onPostCreate(FrameLayout container) {
        if (container == null) {
            View root = mActivity.findViewById(android.R.id.content);
            if (root instanceof FrameLayout) {
                container = (FrameLayout) root;
            }
        }
        if (container == null) {
            return;
        }
        View floatButton = createFloatButton();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END;
        int dp18 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, mActivity.getResources().getDisplayMetrics());
        params.topMargin = dp18 * 7;
        params.rightMargin = dp18;
        floatButton.setLayoutParams(params);
        container.addView(floatButton);
    }

    public View createFloatButton() {
        ImageView stackView = new ImageView(mActivity);
        stackView.setImageResource(R.drawable.fragmentation_ic_stack);
        int dp18 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, mActivity.getResources().getDisplayMetrics());
        stackView.setOnTouchListener(new StackViewTouchListener(stackView, dp18 / 4));
        stackView.setOnClickListener(v -> showFragmentStackHierarchyView());
        return stackView;
    }

    /**
     * 调试相关:以dialog形式 显示 栈视图
     */
    public void showFragmentStackHierarchyView() {
        if (mStackDialog != null && mStackDialog.isShowing()) return;
        DebugHierarchyViewContainer container = new DebugHierarchyViewContainer(mActivity);
        container.bindFragmentRecords(getFragmentRecords());
        container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mStackDialog = new AlertDialog.Builder(mActivity)
                .setView(container)
                .setPositiveButton(android.R.string.cancel, null)
                .setCancelable(true)
                .create();
        mStackDialog.show();
    }

    /**
     * 调试相关:以log形式 打印 栈视图
     */
    public void logFragmentRecords(String tag) {
        List<DebugFragmentRecord> fragmentRecordList = getFragmentRecords();
        if (fragmentRecordList == null) return;

        StringBuilder sb = new StringBuilder();

        for (int i = fragmentRecordList.size() - 1; i >= 0; i--) {
            DebugFragmentRecord fragmentRecord = fragmentRecordList.get(i);

            if (i == fragmentRecordList.size() - 1) {
                sb.append("═══════════════════════════════════════════════════════════════════════════════════\n");
                if (i == 0) {
                    sb.append("\t栈顶\t\t\t").append(fragmentRecord.fragmentName).append("\n");
                    sb.append("═══════════════════════════════════════════════════════════════════════════════════");
                } else {
                    sb.append("\t栈顶\t\t\t").append(fragmentRecord.fragmentName).append("\n\n");
                }
            } else if (i == 0) {
                sb.append("\t栈底\t\t\t").append(fragmentRecord.fragmentName).append("\n\n");
                processChildLog(fragmentRecord.childFragmentRecord, sb, 1);
                sb.append("═══════════════════════════════════════════════════════════════════════════════════");
                Log.i(tag, sb.toString());
                return;
            } else {
                sb.append("\t↓\t\t\t").append(fragmentRecord.fragmentName).append("\n\n");
            }

            processChildLog(fragmentRecord.childFragmentRecord, sb, 1);
        }
    }

    private List<DebugFragmentRecord> getFragmentRecords() {
        List<DebugFragmentRecord> fragmentRecordList = new ArrayList<>();

        List<Fragment> fragmentList = mActivity.getSupportFragmentManager().getFragments();

        if (fragmentList == null || fragmentList.size() < 1) return null;

        for (Fragment fragment : fragmentList) {
            addDebugFragmentRecord(fragmentRecordList, fragment);
        }
        return fragmentRecordList;
    }

    private void processChildLog(List<DebugFragmentRecord> fragmentRecordList, StringBuilder sb, int childHierarchy) {
        if (fragmentRecordList == null || fragmentRecordList.size() == 0) return;

        for (int j = 0; j < fragmentRecordList.size(); j++) {
            DebugFragmentRecord childFragmentRecord = fragmentRecordList.get(j);
            for (int k = 0; k < childHierarchy; k++) {
                sb.append("\t\t\t");
            }
            if (j == 0) {
                sb.append("\t子栈顶\t\t").append(childFragmentRecord.fragmentName).append("\n\n");
            } else if (j == fragmentRecordList.size() - 1) {
                sb.append("\t子栈底\t\t").append(childFragmentRecord.fragmentName).append("\n\n");
                processChildLog(childFragmentRecord.childFragmentRecord, sb, ++childHierarchy);
                return;
            } else {
                sb.append("\t↓\t\t\t").append(childFragmentRecord.fragmentName).append("\n\n");
            }

            processChildLog(childFragmentRecord.childFragmentRecord, sb, childHierarchy);
        }
    }

    private List<DebugFragmentRecord> getChildFragmentRecords(Fragment parentFragment) {
        List<DebugFragmentRecord> fragmentRecords = new ArrayList<>();

        List<Fragment> fragmentList = parentFragment.getChildFragmentManager().getFragments();
        if (fragmentList == null || fragmentList.size() < 1) return null;

        for (int i = fragmentList.size() - 1; i >= 0; i--) {
            Fragment fragment = fragmentList.get(i);
            addDebugFragmentRecord(fragmentRecords, fragment);
        }
        return fragmentRecords;
    }

    private void addDebugFragmentRecord(List<DebugFragmentRecord> fragmentRecords, Fragment fragment) {
        if (fragment != null) {
            int backStackCount = fragment.getFragmentManager().getBackStackEntryCount();
            CharSequence name = fragment.getClass().getSimpleName();
            if (backStackCount == 0) {
                name = span(name, "* ");
            } else {
                for (int j = 0; j < backStackCount; j++) {
                    FragmentManager.BackStackEntry entry = fragment.getFragmentManager().getBackStackEntryAt(j);
                    if ((entry.getName() != null && entry.getName().equals(fragment.getTag()))
                            || (entry.getName() == null && fragment.getTag() == null)) {
                        break;
                    }
                    if (j == backStackCount - 1) {
                        name = span(name, "* ");
                    }
                }
            }

            fragmentRecords.add(new DebugFragmentRecord(name + ":" + fragment, getChildFragmentRecords(fragment)));
        }
    }

    private CharSequence span(CharSequence name, String str) {
        return str + name;
    }

    private class StackViewTouchListener implements View.OnTouchListener {
        private View stackView;
        private float dX, dY = 0f;
        private float downX, downY = 0f;
        private boolean isClickState;
        private int clickLimitValue;

        StackViewTouchListener(View stackView, int clickLimitValue) {
            this.stackView = stackView;
            this.clickLimitValue = clickLimitValue;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float X = event.getRawX();
            float Y = event.getRawY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isClickState = true;
                    downX = X;
                    downY = Y;
                    dX = stackView.getX() - event.getRawX();
                    dY = stackView.getY() - event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(X - downX) < clickLimitValue && Math.abs(Y - downY) < clickLimitValue && isClickState) {
                        isClickState = true;
                    } else {
                        isClickState = false;
                        stackView.setX(event.getRawX() + dX);
                        stackView.setY(event.getRawY() + dY);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (X - downX < clickLimitValue && isClickState) {
                        stackView.performClick();
                    }
                    break;
                default:
                    return false;
            }
            return true;
        }
    }
}
