package com.salmanmalvasi.studentcc.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import com.salmanmalvasi.studentcc.R;
import com.salmanmalvasi.studentcc.adapters.MarksCourseSliderAdapter;
import com.salmanmalvasi.studentcc.helpers.AppDatabase;
import com.salmanmalvasi.studentcc.interfaces.ClassStatsDao;
import com.salmanmalvasi.studentcc.interfaces.MarksDao;
import com.salmanmalvasi.studentcc.models.Course;
import com.salmanmalvasi.studentcc.models.Mark;

public class PerformanceFragment extends Fragment {
    private FirebaseAnalytics mFirebaseAnalytics;
    private AppBarLayout appBarLayout;
    private ViewPager2 viewPagerMarks;
    private TabLayout tabLayoutCourses;
    private List<Course.AllData> courses = new ArrayList<>();
    private MarksCourseSliderAdapter marksSliderAdapter;
    private MarksDao marksDao;
    private ClassStatsDao classStatsDao;

    public PerformanceFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext());
        View view = inflater.inflate(R.layout.fragment_performance, container, false);

        appBarLayout = view.findViewById(R.id.app_bar);
        viewPagerMarks = view.findViewById(R.id.view_pager_marks);
        tabLayoutCourses = view.findViewById(R.id.tab_layout_courses);
        LinearLayout header = view.findViewById(R.id.linear_layout_header);


        float pixelDensity = getResources().getDisplayMetrics().density;

        getParentFragmentManager().setFragmentResultListener("customInsets", this, (requestKey, result) -> {
            int systemWindowInsetLeft = result.getInt("systemWindowInsetLeft");
            int systemWindowInsetTop = result.getInt("systemWindowInsetTop");
            int systemWindowInsetRight = result.getInt("systemWindowInsetRight");
            int bottomNavigationHeight = result.getInt("bottomNavigationHeight");

            appBarLayout.setPadding(
                    systemWindowInsetLeft,
                    systemWindowInsetTop,
                    systemWindowInsetRight,
                    0
            );

            if (viewPagerMarks != null) {
                viewPagerMarks.setPageTransformer((page, position) -> page.setPadding(
                        systemWindowInsetLeft,
                        0,
                        systemWindowInsetRight,
                        (int) (bottomNavigationHeight + 20 * pixelDensity)));
            }
        });

        appBarLayout.addOnOffsetChangedListener((appBarLayout1, verticalOffset) -> {
            if (header != null && header.getHeight() > 0) {
                float alpha = 1 - ((float) (-1 * verticalOffset) / header.getHeight());
                header.setAlpha(alpha);
            }
        });

        AppDatabase appDatabase = AppDatabase.getInstance(requireActivity().getApplicationContext());
        marksDao = appDatabase.marksDao();
        classStatsDao = appDatabase.classStatsDao();

        TextView marksLastUpdated = view.findViewById(R.id.text_view_marks_last_updated);
        updateMarksLastUpdatedIndicator(marksLastUpdated);

        getParentFragmentManager().setFragmentResultListener("dataUpdated", this, (requestKey, result) -> {
            if (isAdded()) {
                loadMarksData();
                updateMarksLastUpdatedIndicator(marksLastUpdated);
            }
        });

        // Page change listener to mark course as read and update badges
        viewPagerMarks.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                markCourseAtPositionAsRead(position);
            }
        });

        loadMarksData();
        return view;
    }

    private void updateMarksLastUpdatedIndicator(TextView textView) {
        if (textView == null || getContext() == null) return;
        String formatted = com.salmanmalvasi.studentcc.sync.DataFreshnessManager.getInstance()
                .getFormattedLastUpdated(requireContext(), com.salmanmalvasi.studentcc.sync.DataFreshnessManager.DATASET_MARKS);
        if (!"Not updated yet".equals(formatted)) {
            textView.setText(formatted);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private void loadMarksData() {
        io.reactivex.rxjava3.core.Single.zip(
                marksDao.getCourses(),
                marksDao.getAllMarks(),
                (courseList, allMarks) -> new Object[]{courseList, allMarks})
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Object[]>() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {}

                    @Override
                    public void onSuccess(@io.reactivex.rxjava3.annotations.NonNull Object[] result) {
                        @SuppressWarnings("unchecked")
                        List<Course.AllData> courseList = (List<Course.AllData>) result[0];
                        @SuppressWarnings("unchecked")
                        List<Mark.AllData> allMarks = (List<Mark.AllData>) result[1];

                        PerformanceFragment.this.courses = courseList;
                        if (courseList == null || courseList.isEmpty()) {
                            tabLayoutCourses.setVisibility(View.GONE);
                            viewPagerMarks.setVisibility(View.GONE);
                            return;
                        }

                        // Map marks to course ID
                        Map<Integer, List<Mark.AllData>> marksByCourseId = new LinkedHashMap<>();
                        for (Course.AllData c : courseList) {
                            if (c.courseId != null) {
                                marksByCourseId.put(c.courseId, new ArrayList<>());
                            }
                        }

                        for (Mark.AllData mark : allMarks) {
                            if (mark.courseId != null && marksByCourseId.containsKey(mark.courseId)) {
                                marksByCourseId.get(mark.courseId).add(mark);
                            }
                        }

                        tabLayoutCourses.setVisibility(View.VISIBLE);
                        viewPagerMarks.setVisibility(View.VISIBLE);

                        marksSliderAdapter = new MarksCourseSliderAdapter(courseList, marksByCourseId, marksDao, classStatsDao);
                        viewPagerMarks.setAdapter(marksSliderAdapter);

                        // Attach TabLayoutMediator
                        new TabLayoutMediator(tabLayoutCourses, viewPagerMarks, (tab, position) -> {
                            Course.AllData course = courseList.get(position);
                            String title = course.courseCode != null ? course.courseCode : "Course " + (position + 1);
                            tab.setText(title);

                            // Unread count marker
                            if (course.unreadMarkCount != null && course.unreadMarkCount > 0) {
                                BadgeDrawable badge = tab.getOrCreateBadge();
                                badge.setNumber(course.unreadMarkCount);
                                badge.setVisible(true);
                            } else {
                                tab.removeBadge();
                            }
                        }).attach();

                        // Mark initial course as read
                        if (!courseList.isEmpty()) {
                            markCourseAtPositionAsRead(0);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        android.util.Log.e("PerformanceFragment", "Error loading marks", e);
                    }
                });
    }

    private void markCourseAtPositionAsRead(int position) {
        if (courses == null || position < 0 || position >= courses.size()) {
            return;
        }
        Course.AllData course = courses.get(position);
        if (course != null && course.courseId != null && course.unreadMarkCount != null && course.unreadMarkCount > 0) {
            marksDao.setMarksRead(course.courseId)
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        course.unreadMarkCount = 0;
                        TabLayout.Tab tab = tabLayoutCourses.getTabAt(position);
                        if (tab != null) {
                            tab.removeBadge();
                        }
                        // Refresh bottom navigation badge
                        getParentFragmentManager().setFragmentResult("getUnreadCount", new Bundle());
                    }, throwable -> android.util.Log.e("PerformanceFragment", "Failed to mark marks as read", throwable));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getParentFragmentManager().setFragmentResult("getUnreadCount", new Bundle());
    }
}
