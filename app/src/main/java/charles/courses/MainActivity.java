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
    public class ActionType { static final int TASK_ACTIVITY = 0, CHANGE_LISTS_ACTIVITY = 1; }
    class TaskAction { static final int CREATED = 0, MODIFIED = 1, CANCELED = 2, DELETED = 3; }
    public static String TaskDataMarker = "TaskData";
    public static String TaskHistoryMarker = "TaskHistory";
    PageAdapter adapter_;
    protected TaskStorage storage_ = null;
    String currentList_ = "";
    protected String backupFile_ = "CoursesBackup.save";
    final Handler periodicChecker_ = new Handler();
    int taskBeingModified_ = 0;
    Runnable periodicCallback_;

    ViewPager taskPager_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initToolbar();

        //Reload data from previous executions
        loadBackup();

        adapter_ = new PageAdapter(this);
        taskPager_ = findViewById(R.id.TaskPager);
        taskPager_.setAdapter(adapter_);

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(taskPager_);

        periodicCallback_ = new Runnable() {
            @Override
            public void run() {
                for ( TaskData task : getTasks() ) {
                    if ( task.recurrence_ != null && task.recurrence_.nextAvailableDate().compareTo( new Date() ) < 0) {
                        task.recurrence_.waitingNextOccurence_ = false;
                        task.completed_ = false;
                    }
                }
                taskUpdate();

                //Check again in one minute's time
                periodicChecker_.postDelayed( this, 60 * 1000 );
            }
        };
    }

    void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_dehaze_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SelectItemActivity.class);
                Bundle bundle = new Bundle();
                SelectItemActivity.Input items = new SelectItemActivity.Input();
                items.title_ = getResources().getString(R.string.edit_lists_activity);
                items.values_ = storage_.getLists();
                items.selected_ = currentList_;
                bundle.putSerializable(SelectItemActivity.InputMarker, items);
                intent.putExtras(bundle);
                startActivityForResult(intent, ActionType.CHANGE_LISTS_ACTIVITY);
            }
        });
    }

    @Override
    protected void onResume() {
        //Launch periodic updates
        periodicChecker_.post( periodicCallback_ );
        super.onResume();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bundle bundle = data.getExtras();
        // Check which request we're responding to
        if (requestCode == ActionType.TASK_ACTIVITY) {
            if ( resultCode == TaskAction.CREATED || resultCode == TaskAction.MODIFIED) {
                //Deserialize the result from the intent bundle
                if ( bundle != null ) {
                    TaskData decodedTask = (TaskData) bundle.getSerializable(TaskDataMarker);

                    //Adding / modifying the task
                    if ( resultCode == TaskAction.CREATED ) {
                        getTasks().add( decodedTask );
                    } else {
                        getTasks().set( taskBeingModified_, decodedTask );
                    }

                    //Add the task to the history of tasks
                    ArrayList<TaskData> history = storage_.getHistory(currentList_);
                    history.add((TaskData) bundle.getSerializable(TaskDataMarker));
                    if ( history.size() > 100 ) {
                        history.remove( 0 );
                    }
                }
                else {
                    System.out.println( "Error : activity action " + resultCode + " without bundled task" );
                }
             } else if ( resultCode == TaskAction.DELETED ) {
                getTasks().remove(taskBeingModified_);
            }
            //Refresh the views
            if ( resultCode != TaskAction.CANCELED ) {
                taskUpdate();
            }
        } else if ( requestCode == ActionType.CHANGE_LISTS_ACTIVITY ) {
            if ( bundle != null ) {
                SelectItemActivity.Result result = (SelectItemActivity.Result) bundle.getSerializable( SelectItemActivity.ResultMarker );
                for ( String removed : result.removedItems_ ) {
                    storage_.removeList(removed);
                }
                for ( SelectItemActivity.ValueChange modified : result.modifiedItems_ ) {
                    storage_.renameList( modified.oldValue_, modified.newValue_ );
                }
                for ( String added: result.addedItems_ ) {
                    storage_.newList( added );
                }
                //Update the selected list
                if ( storage_.getLists().size() > 1 ) {
                    currentList_ = result.selected_;
                } else {
                    //Fail safe in case no lists are remaining : we reuse the default use
                    currentList_ = storage_.getLists().get(0);
                }
                taskUpdate();
            } else {
                System.out.println( "Error : activity action " + resultCode + " without bundled task" );
            }
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
            out.writeObject(currentList_);
            out.writeObject(storage_);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ( currentList_ == null || storage_ == null ) {
            currentList_ = getResources().getString(R.string.default_list_name);
            storage_ = new TaskStorage( currentList_ );
        }
    }

    protected void loadBackup()
    {
        try (
            FileInputStream inputStream = openFileInput( backupFile_ );
            ObjectInputStream in = new ObjectInputStream(inputStream);
        )
        {
            currentList_ = (String) in.readObject();
            storage_ = (TaskStorage) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ( storage_ == null || currentList_ == null || currentList_.isEmpty() ) {
            currentList_ = getResources().getString(R.string.default_list_name);
            storage_ = new TaskStorage( currentList_ );
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

        //Find if there is at least one completed task : if not, hide the clear button
        MenuItem clearButton = menu.findItem(R.id.clear_tasks);
        boolean tasksToClear = false;
        for ( TaskData task : getTasks() ) {
            if ( task.completed_ ) {
                tasksToClear = true;
                break;
            }
        }
        clearButton.setVisible(tasksToClear);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
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
        Bundle bundle = new Bundle();
        if ( taskData != null ) {
            bundle.putSerializable(MainActivity.TaskDataMarker, taskData);
            taskBeingModified_ = getTasks().indexOf(taskData);
        }
        bundle.putSerializable(MainActivity.TaskHistoryMarker, storage_.getHistory(currentList_));
        intent.putExtras(bundle);
        startActivityForResult(intent, ActionType.TASK_ACTIVITY);
    }

    //Erases all Tasks that have been completed
    public void clearTasks() {
        ArrayList<TaskData> toRemove = new ArrayList<>();
        for ( TaskData task : getTasks() ) {
            if ( task.completed_  ) {
                if ( task.recurrence_ != null ) {
                    task.recurrence_.waitingNextOccurence_ = true;
                    task.completed_ = false;
                } else {
                    toRemove.add( task );
                }
            }
        }
        getTasks().removeAll(toRemove);
        taskUpdate();
    }

    void taskUpdate() {
        //Titre : affichage du nom de la liste courante
        setTitle(currentList_);

        //Update clear menu button : hide if no tasks to clear
        invalidateOptionsMenu();

        //Update task lists
        adapter_.refresh();
    }

    //Returns the tasks of the current list
    ArrayList<TaskData> getTasks() {
        return storage_.getTasks(currentList_);
    }
}
