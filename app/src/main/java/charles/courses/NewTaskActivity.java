package charles.courses;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;

public class NewTaskActivity extends AppCompatActivity {

    protected Intent result_ = new Intent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);

        //By default, the new task is considered canceled
         setResult( MainActivity.NewTaskAction.CANCELED, result_ );
    }

    public void onConfirm(View view) {
        AutoCompleteTextView input_task = findViewById(R.id.NewTaskInput);
        String name = input_task.getText().toString();
        if ( !name.isEmpty() ) {
            //Intanciation of a TaskData object
            TaskData taskData = new TaskData();
            taskData.name_ = name;
            AutoCompleteTextView input_qty = findViewById(R.id.NewTaskQuantityInput);
            taskData.qty_ = input_qty.getText().toString();

            //Serialization
            Bundle bundle = new Bundle();
            bundle.putSerializable(MainActivity.TaskDataMarker, taskData);
            result_.putExtras(bundle);
            setResult( MainActivity.NewTaskAction.CREATED, result_);
        }
        finish();
    }

    public void onCancel(View view)
    {
        finish();
    }
}
