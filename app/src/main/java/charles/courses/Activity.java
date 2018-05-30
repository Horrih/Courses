package charles.courses;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ListView;

import java.util.ArrayList;

public class Activity extends AppCompatActivity {
    TaskData modifiedTask_ = null;

    //Returns the data holder containing all the persisted data
    ApplicationWithStorage getStorage() {
        return (ApplicationWithStorage) getApplication();
    }

    //Returns the tasks of the current list
    ArrayList<TaskData> getTasks() {
        return getStorage().tasks_.getTasks(getStorage().currentList_);
    }

    ArrayList<String> getStores() {
        return getStorage().tasks_.getStores(getStorage().currentList_);
    }

    String getCurrentList() {
        return getStorage().currentList_;
    }

    RecipeStorage getRecipes() {
        return getStorage().recipes_;
    }

    //Called on NewTaskButton click
    public void onNewTask(View view) {
        launchTaskActivity(null);
    }

    //Launches a new/modify task activity
    public void launchTaskActivity(TaskData task) {
        Intent intent = new Intent(Activity.this, NewTaskActivity.class);
        Bundle bundle = new Bundle();
        NewTaskActivity.Input input = new NewTaskActivity.Input();
        input.task_ = task;
        input.reason_ = getReason();
        input.enableRecurrence_ = isRecurrenceEnabled();
        if ( task != null )
            modifiedTask_ = task;

        bundle.putSerializable(NewTaskActivity.TaskDataMarker, input);
        intent.putExtras(bundle);
        startActivityForResult(intent, MainActivity.ActionType.TASK_ACTIVITY);
    }

    String getReason() {
        return "";
    }

    boolean isRecurrenceEnabled() {
        return true;
    }

    //Handle a task change and updates the containing list
    TaskData onTaskResult(int resultCode, Intent data, ArrayList<TaskData> list ) {
        TaskData task = null;
        if ( resultCode == NewTaskActivity.TaskAction.DELETED )
            list.remove(modifiedTask_);
        else if ( resultCode == NewTaskActivity.TaskAction.CREATED || resultCode == NewTaskActivity.TaskAction.MODIFIED ) {
            Bundle bundle = data.getExtras();
            if ( bundle != null ) {
                NewTaskActivity.Output result = (NewTaskActivity.Output) bundle.getSerializable( NewTaskActivity.TaskDataMarker );
                task = result.task_;
                if ( resultCode == NewTaskActivity.TaskAction.MODIFIED )
                    list.set( list.indexOf(modifiedTask_), task);
                else
                    list.add( task );
            }
        }
        modifiedTask_ = null;
        return task;
    }

    void enableTaskModification( ListView listView ) {
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                    TaskData task = (TaskData) view.getTag();
                    launchTaskActivity(task);
                    return true;
                }
                return false;
            }
        });
    }
}
