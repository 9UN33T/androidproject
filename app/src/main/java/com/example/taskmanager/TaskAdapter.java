package com.example.taskmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
    private List<Task> tasks;
    private final SimpleDateFormat dateFormat;

    public TaskAdapter(List<Task> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position < tasks.size()) {
            Task task = tasks.get(position);
            holder.taskName.setText(task.getTaskName());
            holder.taskTime.setText(dateFormat.format(new Date(task.getDueDateTime())));
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView taskName;
        TextView taskTime;

        ViewHolder(View view) {
            super(view);
            taskName = view.findViewById(R.id.taskName);
            taskTime = view.findViewById(R.id.taskTime);
        }
    }
}