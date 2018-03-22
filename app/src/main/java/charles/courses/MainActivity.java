package charles.courses;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public class ActionType { static final int TASK_ACTIVITY = 0; }
    class TaskAction { static final int CREATED = 0, MODIFIED = 1, CANCELED = 2, DELETED = 3; }
    public static String TaskDataMarker = "TaskData";
    PageAdapter adapter_;
    protected ArrayList<TaskData> items_ = new ArrayList<>();
    protected String backupFile_ = "CoursesBackup.save";
    final Handler periodicChecker_ = new Handler();
    int taskBeingModified_ = 0;
    Runnable periodicCallback_;

    ViewPager taskPager_;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Reload data from previous executions
        loadBackup();

        adapter_ = new PageAdapter(this, items_);
        taskPager_ = findViewById(R.id.TaskPager);
        taskPager_.setAdapter(adapter_);

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(taskPager_);

        periodicCallback_ = new Runnable() {
            @Override
            public void run() {
                for ( TaskData task : items_ ) {
                    if ( task.recurrence_ != null && task.recurrence_.nextAvailableDate().compareTo( new Date() ) < 0) {
                        task.recurrence_.waitingNextOccurence_ = false;
                        task.completed_ = false;
                    }
                }
                adapter_.refresh();

                //Check again in one minute's time
                periodicChecker_.postDelayed( this, 60 * 1000 );
            }
        };
    }

    @Override
    protected void onResume() {
        //Launch periodic updates
        periodicChecker_.post( periodicCallback_ );
        super.onResume();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ActionType.TASK_ACTIVITY) {
            if ( resultCode == TaskAction.CREATED || resultCode == TaskAction.MODIFIED) {
                //Deserialize the result from the intent bundle
                Bundle bundle = data.getExtras();
                if ( bundle != null ) {
                    TaskData decodedTask = (TaskData) bundle.getSerializable(TaskDataMarker);

                    //Adding / modifying the task
                    if ( resultCode == TaskAction.CREATED ) {
                        items_.add( decodedTask );
                    } else {
                        items_.set( taskBeingModified_, decodedTask );
                    }
                }
                else {
                    System.out.println( "Error : activity action " + resultCode + " without bundled task" );
                }
             } else if ( resultCode == TaskAction.DELETED ) {
                items_.remove(taskBeingModified_);
            }
        }
        //Refresh the views
        if ( resultCode != TaskAction.CANCELED ) {
            adapter_.refresh();
        }
    }

    //Store all tasks to a file
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
        //Backup task data to a file
        backupData();

        //We stop checking for updates
        periodicChecker_.removeCallbacks(periodicCallback_);

        //Parent method
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
        if (id == R.id.clear_tasks ) {
            clearTasks();
        } else if ( id == R.id.share_tasks ){
            System.out.println( "Share button clicked" );
        }

        return super.onOptionsItemSelected(item);
    }

    //Called on NewTaskButton click
    public void onNewTask(View view)
    {
        launchTaskActivity(null);
    }

    public void launchTaskActivity(TaskData taskData)
    {
        Intent intent = new Intent(MainActivity.this, NewTaskActivity.class);
        if ( taskData != null ) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(MainActivity.TaskDataMarker, taskData);
            intent.putExtras(bundle);
            taskBeingModified_ = items_.indexOf(taskData);
        }
        startActivityForResult(intent, ActionType.TASK_ACTIVITY);
    }

    //Erases all Tasks that have been completed
    public void clearTasks()
    {
        ArrayList<TaskData> toRemove = new ArrayList<>();
        for ( TaskData task : items_) {
            if ( task.completed_  ) {
                if ( task.recurrence_ != null ) {
                    task.recurrence_.waitingNextOccurence_ = true;
                    task.completed_ = false;
                } else {
                    toRemove.add( task );
                }
            }
        }
        items_.removeAll(toRemove);
        adapter_.refresh();
    }
}
