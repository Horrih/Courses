package charles.courses;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends Activity {
    class ActionType { static final int TASK_ACTIVITY = 0, CHANGE_LISTS_ACTIVITY = 1, CHANGE_RECIPES_ACTIVITY = 2, ADD_RECIPE_ACTIVITY = 3; }
    PageAdapter adapter_;
    final Handler periodicChecker_ = new Handler();
    Runnable periodicCallback_;
    ViewPager taskPager_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initToolbar();

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

        if ( getStorage().tutorial_.step_ != Tutorial.Step.NONE )
            launchTutorial();
    }

    void launchTutorial() {
        //If we are performing the tutorial : go to the next step
        getStorage().tutorial_.step_ = Tutorial.Step.START;
        getStorage().tutorial_.nextTutorialStep(this);
    }

    void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu_24dp);

        final DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.NavigationView);
        navigationView.setNavigationItemSelectedListener(
            new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(MenuItem item) {
                    drawerLayout.closeDrawers();
                    int id = item.getItemId();
                    if (id == R.id.navigation_edit_lists) {
                        Intent intent = new Intent(MainActivity.this, SelectItemActivity.class);
                        SelectItemActivity.Input items = new SelectItemActivity.Input();
                        items.title_ = getResources().getString(R.string.edit_lists_activity);
                        items.values_ = getStorage().tasks_.getLists();
                        items.selected_ = getCurrentList();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(SelectItemActivity.InputMarker, items);
                        intent.putExtras(bundle);
                        startActivityForResult(intent, ActionType.CHANGE_LISTS_ACTIVITY);
                    } else if (id == R.id.navigation_edit_recipes) {
                        Intent intent = new Intent(MainActivity.this, EditRecipesActivity.class);
                        startActivityForResult(intent, ActionType.CHANGE_RECIPES_ACTIVITY);
                    } else if (id == R.id.navigation_tutorial) {
                        launchTutorial();
                    } else {
                        TextView text = item.getActionView().findViewById(R.id.submenu_text);
                        getStorage().currentList_ = text.getText().toString();
                        taskUpdate();
                    }
                    return true;
                }
            }
        );
    }

    @Override
    protected void onResume() {
        //Launch periodic updates
        periodicChecker_.post( periodicCallback_ );
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        TaskStorage tasks = getStorage().tasks_;
        // Check which request we're responding to
        if (requestCode == ActionType.TASK_ACTIVITY) {
            //Task change: update the lists if necessary
            TaskData task = onTaskResult( resultCode, data, getTasks() );
            if ( task != null ) {
                //Update history
                ArrayList<TaskData> history = getStorage().tasks_.getHistory(getCurrentList());
                history.add(0, task);
                if ( history.size() > 1000 )
                    history.remove( 1000 );

                //Refresh the views
                taskUpdate();
            }
        } else if ( requestCode == ActionType.CHANGE_LISTS_ACTIVITY ) {
            Bundle bundle = data.getExtras();
            if ( bundle != null ) {
                SelectItemActivity.Result result = (SelectItemActivity.Result) bundle.getSerializable( SelectItemActivity.ResultMarker );
                for ( String removed : result.removedItems_ )
                    tasks.removeList(removed);

                for ( SelectItemActivity.ValueChange modified : result.modifiedItems_ )
                    tasks.renameList( modified.oldValue_, modified.newValue_ );

                for ( String added: result.addedItems_ ) {
                    tasks.newList( added );
                    tasks.setStores(added, tasks.getStores(getCurrentList()));
                }
                //Update the selected list
                if ( tasks.getLists().size() > 1 )
                    getStorage().currentList_ = result.selected_;

                //Fail safe in case no lists are remaining : we reuse the default use
                else
                    getStorage().currentList_ = tasks.getLists().get(0);

                taskUpdate();
            } else {
                System.out.println( "Error : activity action " + resultCode + " without bundled task" );
            }
        } else if ( requestCode == ActionType.ADD_RECIPE_ACTIVITY ) {
            Bundle bundle = data.getExtras();
            if ( bundle != null ) {
                AddRecipeActivity.Output result = (AddRecipeActivity.Output) bundle.getSerializable(AddRecipeActivity.OutputMarker);
                getTasks().addAll(result.tasks_);
            }
        } else if ( requestCode == ActionType.CHANGE_RECIPES_ACTIVITY ) {
            taskUpdate();
        }
    }

    @Override
    protected void onStop() {
        //Backup task data to a file
        getStorage().backupData();

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
            String text = "" + getStorage().currentList_ + "':";
            text += adapter_.adapters_.get( 0 ).toText();
            shareIntent.putExtra(Intent.EXTRA_TEXT, text );
            try {
                startActivity(shareIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast toast = Toast.makeText(this, getResources().getString(R.string.no_available_app), Toast.LENGTH_LONG);
                toast.show();
            }
        } else if ( id == android.R.id.home ) {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.openDrawer(GravityCompat.START);
        }

        return super.onOptionsItemSelected(item);
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
        setTitle(getCurrentList());

        //Refresh du menu de navigation
        NavigationView navigationView = findViewById(R.id.NavigationView);
        Menu menu = navigationView.getMenu();
        ArrayList<String> lists = getStorage().tasks_.getLists();
        int maxNbItems = 8;
        for ( int i = 0; i < maxNbItems; i++ ) {
            menu.removeItem(i + 1 );
        }
        for ( int i = 0; i < maxNbItems && i < lists.size(); i++ ) {
            MenuItem item = menu.add(Menu.NONE, i + 1, i + 1, null).setActionView(R.layout.submenu_item);
            View view = item.getActionView();
            ((TextView) view.findViewById(R.id.submenu_text)).setText(lists.get(i));
        }
        navigationView.invalidate();

        //Update task lists
        adapter_.refresh();

        //Update clear menu button : hide if no tasks to clear
        invalidateOptionsMenu();

        //Update add recipe button : hide if no recipe
        FloatingActionButton addRecipeButton = findViewById(R.id.NewRecipeButton);
        if ( getRecipes().recipes_.isEmpty() )
            addRecipeButton.hide();
        else
            addRecipeButton.show();

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

    public void onAddRecipe(View v) {
        Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
        startActivityForResult(intent, ActionType.ADD_RECIPE_ACTIVITY);
    }

    @Override
    public void onNewTask(View v){
        super.onNewTask(v);
    }
}
