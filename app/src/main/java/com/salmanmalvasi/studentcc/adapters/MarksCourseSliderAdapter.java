package com.salmanmalvasi.studentcc.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import com.salmanmalvasi.studentcc.R;
import com.salmanmalvasi.studentcc.helpers.RelativePerformanceCalculator;
import com.salmanmalvasi.studentcc.interfaces.ClassStatsDao;
import com.salmanmalvasi.studentcc.interfaces.MarksDao;
import com.salmanmalvasi.studentcc.models.ClassStats;
import com.salmanmalvasi.studentcc.models.Course;
import com.salmanmalvasi.studentcc.models.CumulativeMark;
import com.salmanmalvasi.studentcc.models.Mark;
import com.salmanmalvasi.studentcc.widgets.PerformanceCard;

public class MarksCourseSliderAdapter extends RecyclerView.Adapter<MarksCourseSliderAdapter.CourseSlideViewHolder> {

    private final List<Course.AllData> courses = new ArrayList<>();
    private final Map<Integer, List<Mark.AllData>> marksByCourseId;
    private final MarksDao marksDao;
    private final ClassStatsDao classStatsDao;

    public MarksCourseSliderAdapter(List<Course.AllData> coursesList,
                                    Map<Integer, List<Mark.AllData>> marksByCourseId,
                                    MarksDao marksDao,
                                    ClassStatsDao classStatsDao) {
        if (coursesList != null) {
            this.courses.addAll(coursesList);
        }
        this.marksByCourseId = marksByCourseId;
        this.marksDao = marksDao;
        this.classStatsDao = classStatsDao;
    }

    @NonNull
    @Override
    public CourseSlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_marks_course_slide, parent, false);
        return new CourseSlideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseSlideViewHolder holder, int position) {
        Course.AllData course = courses.get(position);
        List<Mark.AllData> marks = marksByCourseId != null && course.courseId != null
                ? marksByCourseId.get(course.courseId)
                : null;
        if (marks == null) {
            marks = new ArrayList<>();
        }
        holder.bind(course, marks, marksDao, classStatsDao);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    public Course.AllData getCourse(int position) {
        if (position >= 0 && position < courses.size()) {
            return courses.get(position);
        }
        return null;
    }

    static class CourseSlideViewHolder extends RecyclerView.ViewHolder {
        private final TextView textCourseTitle;
        private final TextView textCourseCode;
        private final View performanceCardsRoot;
        private final RecyclerView recyclerMarks;
        private final TextView textEmpty;

        CourseSlideViewHolder(@NonNull View itemView) {
            super(itemView);
            textCourseTitle = itemView.findViewById(R.id.text_slide_course_title);
            textCourseCode = itemView.findViewById(R.id.text_slide_course_code);
            performanceCardsRoot = itemView.findViewById(R.id.performance_cards_include);
            recyclerMarks = itemView.findViewById(R.id.recycler_slide_marks);
            textEmpty = itemView.findViewById(R.id.text_slide_empty);

            recyclerMarks.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            recyclerMarks.setNestedScrollingEnabled(false);
        }

        void bind(Course.AllData course, List<Mark.AllData> marks, MarksDao marksDao, ClassStatsDao classStatsDao) {
            textCourseTitle.setText(course.courseTitle != null ? course.courseTitle : "Course");
            String code = course.courseCode != null ? course.courseCode : "";
            String type = course.courseType != null ? " · " + course.courseType.toUpperCase() : "";
            textCourseCode.setText(code + type);

            if (marks.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
                recyclerMarks.setVisibility(View.GONE);
            } else {
                textEmpty.setVisibility(View.GONE);
                recyclerMarks.setVisibility(View.VISIBLE);
                recyclerMarks.setAdapter(new MarksItemAdapter(marks, course));
            }

            // Load cumulative metrics
            loadCumulativeCards(course, marks, marksDao, classStatsDao);
        }

        private void loadCumulativeCards(Course.AllData course, List<Mark.AllData> marks,
                                         MarksDao marksDao, ClassStatsDao classStatsDao) {
            if (performanceCardsRoot == null || course.courseCode == null) return;

            PerformanceCard overall = performanceCardsRoot.findViewById(R.id.performance_card_overall);
            PerformanceCard theory = performanceCardsRoot.findViewById(R.id.performance_card_theory);
            PerformanceCard average = performanceCardsRoot.findViewById(R.id.performance_card_average);
            PerformanceCard lab = performanceCardsRoot.findViewById(R.id.performance_card_lab);
            PerformanceCard project = performanceCardsRoot.findViewById(R.id.performance_card_project);

            if (overall != null) overall.setIndeterminate(true);
            if (theory != null) theory.setIndeterminate(true);
            if (average != null) average.hide();
            if (lab != null) lab.setIndeterminate(true);
            if (project != null) project.setIndeterminate(true);

            marksDao.getCumulativeMark(course.courseCode)
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<CumulativeMark>() {
                        @Override
                        public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {}

                        @Override
                        public void onSuccess(@io.reactivex.rxjava3.annotations.NonNull CumulativeMark cm) {
                            if (overall != null) {
                                if (cm.grandTotal != null) {
                                    overall.show();
                                    overall.setIndeterminate(false);
                                    overall.setScore(cm.grandTotal, cm.grandMax);
                                } else {
                                    overall.hide();
                                }
                            }

                            if (theory != null) {
                                if (cm.theoryTotal != null) {
                                    theory.show();
                                    theory.setIndeterminate(false);
                                    theory.setScore(cm.theoryTotal, cm.theoryMax);
                                } else {
                                    theory.hide();
                                }
                            }

                            if (project != null) {
                                if (cm.projectTotal != null) {
                                    project.show();
                                    project.setIndeterminate(false);
                                    project.setScore(cm.projectTotal, cm.projectMax);
                                } else {
                                    project.hide();
                                }
                            }

                            if (lab != null) {
                                if (cm.labTotal != null) {
                                    lab.show();
                                    lab.setIndeterminate(false);
                                    lab.setScore(cm.labTotal, cm.labMax);
                                } else {
                                    lab.hide();
                                }
                            }

                            // Average calculation
                            if (average != null && classStatsDao != null && RelativePerformanceCalculator.isTheoryCourse(course.courseCode)) {
                                classStatsDao.getAllClassStatsForCourse(course.courseCode)
                                        .subscribeOn(Schedulers.single())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new SingleObserver<List<ClassStats>>() {
                                            @Override
                                            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {}

                                            @Override
                                            public void onSuccess(@io.reactivex.rxjava3.annotations.NonNull List<ClassStats> statsList) {
                                                if (statsList != null && !statsList.isEmpty() && marks != null) {
                                                    Double ahead = RelativePerformanceCalculator.getWeightageMarksAheadValue(marks, statsList);
                                                    if (ahead != null) {
                                                        average.show();
                                                        String val = (ahead >= 0 ? "+" : "") + new java.text.DecimalFormat("#.##").format(ahead);
                                                        average.setRelativePerformance(val);
                                                    } else {
                                                        average.hide();
                                                    }
                                                } else {
                                                    average.hide();
                                                }
                                            }

                                            @Override
                                            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                                                average.hide();
                                            }
                                        });
                            } else if (average != null) {
                                average.hide();
                            }
                        }

                        @Override
                        public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                            if (overall != null) overall.hide();
                            if (theory != null) theory.hide();
                            if (project != null) project.hide();
                            if (lab != null) lab.hide();
                            if (average != null) average.hide();
                        }
                    });
        }
    }
}

