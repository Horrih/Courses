package charles.courses;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public class ActionType { static final int NEW_TASK = 0; }
    public class NewTaskAction { static final int CREATED = 0, MODIFIED = 1, CANCELED = 2, DELETED = 3; }
    public static String TaskDataMarker = "TaskData";
    protected ListView list_;
    protected ArrayList<TaskData> items_ = new ArrayList<>();
    protected String backupFile_ = "CoursesBackup.save";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Reload data from previous executions
        loadBackup();

        //Creation of an adapter linking the task list with the ListView
        list_=findViewById(R.id.VuePrinci);
        TaskAdapter adapter=new TaskAdapter(MainActivity.this, R.layout.task_view, items_);
        list_.setAdapter(adapter);
        list_.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected item text from ListView
                TaskData taskData = (TaskData) parent.getItemAtPosition(position);
                taskData.completed_  = !taskData.completed_;
                ((BaseAdapter)list_ .getAdapter()).notifyDataSetChanged();
            }
        });

        //Popup NewTaskActivity on click on the NewTaskButton
        FloatingActionButton newTaskButton = findViewById(R.id.NewTaskButton);
        newTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                Intent intent = new Intent(MainActivity.this, NewTaskActivity.class);
                startActivityForResult(intent, ActionType.NEW_TASK);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ActionType.NEW_TASK) {
            if ( resultCode == NewTaskAction.CREATED ) {
                Bundle bundle = data.getExtras();
                if ( bundle != null) {
                    TaskData task = (TaskData) bundle.getSerializable(TaskDataMarker);
                    items_.add( task );
                }
            }
        }
    }

    protected void backupData()
    {
        try (
            FileOutputStream outputStream = openFileOutput( backupFile_, Context.MODE_PRIVATE);
            ObjectOutputStream out = new ObjectOutputStream(outputStream);
         )
        {
            out.writeObject(items_);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    protected void loadBackup()
    {
        try (
            FileInputStream inputStream = openFileInput( backupFile_ );
            ObjectInputStream in = new ObjectInputStream(inputStream);
        )
        {
            items_ = (ArrayList<TaskData>) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop()
    {
        backupData();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
