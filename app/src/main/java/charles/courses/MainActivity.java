package charles.courses;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public class ActionType { static final int TASK_ACTIVITY = 0, CHANGE_LISTS_ACTIVITY = 1; }
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
            if ( resultCode == NewTaskActivity.TaskAction.CREATED || resultCode == NewTaskActivity.TaskAction.MODIFIED) {
                //Deserialize the result from the intent bundle
                if ( bundle != null ) {
                    NewTaskActivity.Output result = (NewTaskActivity.Output) bundle.getSerializable(NewTaskActivity.TaskDataMarker);

                    //Adding / modifying the task
                    if ( resultCode == NewTaskActivity.TaskAction.CREATED ) {
                        getTasks().add( result.task_ );
                    } else {
                        getTasks().set( taskBeingModified_, result.task_ );
                    }

                    //We add the task to the history
                    NewTaskActivity.Output copy = (NewTaskActivity.Output) bundle.getSerializable(NewTaskActivity.TaskDataMarker);
                    ArrayList<TaskData> history = storage_.getHistory(currentList_);
                    history.add(0, copy.task_);
                    if ( history.size() > 100 ) {
                        history.remove( 100 );
                    }

                    //Update the list of stores
                    ArrayList<String> stores = storage_.getStores(currentList_);
                    stores.clear();
                    stores.addAll( result.stores_ );
                }
                else {
                    System.out.println( "Error : activity action " + resultCode + " without bundled task" );
                }
             } else if ( resultCode == NewTaskActivity.TaskAction.DELETED ) {
                getTasks().remove(taskBeingModified_);
            }
            //Refresh the views
            if ( resultCode != NewTaskActivity.TaskAction.CANCELED ) {
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
            storage_ = new TaskStorage( getResources() );
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
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String text = "" + currentList_ + "':";
            text += adapter_.adapters_.get( 0 ).toText();
            shareIntent.putExtra(Intent.EXTRA_TEXT, text );
            try {
                startActivity(shareIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast toast = Toast.makeText(this, getResources().getString(R.string.no_available_app), Toast.LENGTH_LONG);
                toast.show();
            }
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
        NewTaskActivity.Input input = new NewTaskActivity.Input();
        input.history_ = storage_.getHistory(currentList_);
        input.stores_ = storage_.getStores(currentList_);
        if ( taskData != null ) {
            input.task_ = taskData;
            taskBeingModified_ = getTasks().indexOf(taskData);
        }
        bundle.putSerializable(NewTaskActivity.TaskDataMarker, input);
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
        //Title : display current task
        setTitle(currentList_);

        //Update task lists
        adapter_.refresh();

        //Update clear menu button : hide if no tasks to clear
        invalidateOptionsMenu();

        //Update completed counter
        ArrayList<TaskData> displayedTasks = adapter_.adapters_.get(0).getDisplayedTasks();
        int nbTasks = displayedTasks.size ();
        TextView counterView = findViewById(R.id.taskCounterView);
        String display = "";
        if ( nbTasks > 0 ) {
            int nbCompleted = 0;
            for ( TaskData task : displayedTasks ) {
                if ( task.completed_ ) {
                    nbCompleted++;
                }
            }
            int percentage = (100 * nbCompleted) / nbTasks;
            display = nbCompleted + "/" + nbTasks + "(" + percentage + "%)";
        }
        counterView.setText( display );
    }

    //Returns the tasks of the current list
    ArrayList<TaskData> getTasks() {
        return storage_.getTasks(currentList_);
    }
}
