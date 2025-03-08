import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.WearableRecyclerView;
import androidx.wear.widget.WearableLinearLayoutManager;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class TaskManagerActivity extends Activity {
    private static final int SPEECH_REQUEST_CODE = 0;
    private static final String CHANNEL_ID = "task_channel";
    private static final String PREFS_NAME = "TaskManagerPrefs";
    private static final String TASKS_KEY = "tasks";

    private WearableRecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> tasks = new ArrayList<>();
    private String pendingTaskName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        setupRecyclerView();
        loadTasks();

        findViewById(R.id.addTaskButton).setOnClickListener(v -> startVoiceInput());
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new WearableLinearLayoutManager(this));
        adapter = new TaskAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            pendingTaskName = results.get(0);
            showDateTimePicker();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            calendar.set(Calendar.MINUTE, timePicker.getMinute());
            addNewTask(calendar.getTimeInMillis());
        });

        timePicker.show(getFragmentManager(), "timePicker");
    }

    private void addNewTask(long dueDateTime) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        Task task = new Task(taskId, pendingTaskName, dueDateTime);
        tasks.add(task);
        adapter.setTasks(tasks);
        saveTasks();
        scheduleNotification(task);
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
        Gson gson = new Gson();
        String json = prefs.getString(TASKS_KEY, null);
        if (json != null) {
            tasks = gson.fromJson(json, new TypeToken<List<Task>>(){}.getType());
            adapter.setTasks(tasks);
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Task Notifications",
                NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void scheduleNotification(Task task) {
        Intent intent = new Intent(this, TaskManagerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Task Due Soon")
                .setContentText("Task: " + task.getTaskName() + " (ID: " + task.getTaskId() + ")")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (task.isDueWithinHour()) {
            notificationManager.notify(task.getTaskId().hashCode(), builder.build());
        }
    }
}