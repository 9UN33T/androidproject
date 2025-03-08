package com.example.taskmanager;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.widget.WearableRecyclerView;
import androidx.wear.widget.WearableLinearLayoutManager;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import android.os.Build;
import android.provider.Settings;
import android.app.AlarmManager;

public class TaskManagerActivity extends FragmentActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    public static final String CHANNEL_ID = "task_channel";
    private static final String PREFS_NAME = "TaskManagerPrefs";
    private static final String TASKS_KEY = "tasks";
    private static final int EXACT_ALARM_PERMISSION_REQUEST_CODE = 124;


    private WearableRecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> tasks;
    private String pendingTaskName;

    private final ActivityResultLauncher<Intent> speechRecognizer = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    List<String> results = result.getData().getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS);
                    if (results != null && !results.isEmpty()) {
                        pendingTaskName = results.get(0);
                        showTimePicker();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tasks = new ArrayList<>();
        createNotificationChannel();
        checkPermissions();
        setupRecyclerView();
        loadTasks();

        findViewById(R.id.addTaskButton).setOnClickListener(v -> startVoiceInput());
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Voice input permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }
    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        WearableLinearLayoutManager layoutManager = new WearableLinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new TaskAdapter(tasks);
        recyclerView.setAdapter(adapter);
        recyclerView.requestFocus();
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        try {
            speechRecognizer.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .build();

        timePicker.addOnPositiveButtonClickListener(view -> {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            calendar.set(Calendar.MINUTE, timePicker.getMinute());
            calendar.set(Calendar.SECOND, 0);
            addNewTask(calendar.getTimeInMillis());
        });

        timePicker.show(getSupportFragmentManager(), "timePicker");
    }

    private void addNewTask(long dueDateTime) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        Task task = new Task(taskId, pendingTaskName, dueDateTime);
        tasks.add(task);
        adapter.notifyDataSetChanged();
        saveTasks();
        scheduleNotification(task);
        Toast.makeText(this, "Task added: " + pendingTaskName, Toast.LENGTH_SHORT).show();
    }

    private void saveTasks() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(tasks);
        editor.putString(TASKS_KEY, json);
        editor.apply();
    }

    private void loadTasks() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(TASKS_KEY, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Task>>(){}.getType();
            List<Task> loadedTasks = gson.fromJson(json, type);
            if (loadedTasks != null) {
                tasks.clear();
                tasks.addAll(loadedTasks);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Task Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for task reminders");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void scheduleNotification(Task task) {
        if (!canScheduleExactAlarms()) {
            Toast.makeText(this, "Please allow exact alarms in settings", Toast.LENGTH_LONG).show();
            requestExactAlarmPermission();
            return;
        }

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("taskId", task.getTaskId());
        intent.putExtra("taskName", task.getTaskName());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                task.getTaskId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            long notificationTime = task.getDueDateTime() - (15 * 60 * 1000);
            if (notificationTime > System.currentTimeMillis()) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            notificationTime,
                            pendingIntent
                    );
                } catch (SecurityException e) {
                    Toast.makeText(this, "Permission required for exact alarms", Toast.LENGTH_LONG).show();
                    requestExactAlarmPermission();
                }
            }
        }
    }
}