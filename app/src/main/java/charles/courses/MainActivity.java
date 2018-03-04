package charles.courses;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ExpandableListView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public class ActionType { static final int NEW_TASK = 0; }
    class NewTaskAction { static final int CREATED = 0, MODIFIED = 1, CANCELED = 2, DELETED = 3; }
    public static String TaskDataMarker = "TaskData";
    protected ExpandableListView list_;
    protected PageAdapter adapter_;
    protected ArrayList<TaskData> items_ = new ArrayList<>();
    protected String backupFile_ = "CoursesBackup.save";

    ViewPager taskPager_;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Reload data from previous executions
        loadBackup();

        adapter_ = new PageAdapter(this.getSupportFragmentManager(), this, items_);
        taskPager_ = findViewById(R.id.TaskPager);
        taskPager_.setAdapter(adapter_);

        // Watch for button clicks.
        final ArrayList<Button> buttons = new ArrayList<>();

        //Tab 1 : tasks by store
        Button button = findViewById(R.id.StorePageButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskPager_.setCurrentItem(0);
            }
        });
        buttons.add( button );

        //Tab 2 : tasks by reason
        button = findViewById(R.id.ReasonPageButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskPager_.setCurrentItem(1);
            }
        });
        buttons.add( button );

        //Tab 3 : recurring tasks
        button = findViewById(R.id.IncomingPageButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskPager_.setCurrentItem(2);
            }
        });
        buttons.add( button );

        taskPager_.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position ){
                for ( int i = 0; i < buttons.size(); i++ ) {
                    buttons.get( i ).setBackgroundResource( ( i == position ) ? R.drawable.tab_style_active : R.drawable.tab_style_inactive);
                }
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
                    adapter_.refresh();
                    //list_.expandGroup( adapter_.getTaskGroup( task ) );
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

    //Called on NewTaskButton click
    public void onNewTask(View view)
    {
        Intent intent = new Intent(MainActivity.this, NewTaskActivity.class);
        startActivityForResult(intent, ActionType.NEW_TASK);
    }

    //Called on ClearTaskButton : erases all Tasks that have been completed
    public void onClearTask(View view)
    {
        ArrayList<TaskData> toRemove = new ArrayList<>();
        for ( TaskData task : items_) {
            if ( task.completed_) {
                toRemove.add( task );
            }
        }
        items_.removeAll(toRemove);
        adapter_.refresh();
    }
}
