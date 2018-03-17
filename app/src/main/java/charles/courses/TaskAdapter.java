package charles.courses;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

public class TaskAdapter extends BaseExpandableListAdapter {
    private Context context_;
    ArrayList<TaskData> original_data_ = null;
    private ArrayList<Pair<String, ArrayList<TaskData>>> data_ = new ArrayList<>();

    TaskAdapter(PageAdapter parentAdapter, Context context, ArrayList<TaskData> data) {
        this.context_ = context;
        this.original_data_ = data;
        refresh();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return data_.get( groupPosition ).first;
    }

    protected ArrayList<TaskData> getDisplayedTasks() {
        ArrayList<TaskData> toDisplay = new ArrayList<>();
        for ( TaskData data : original_data_ ) {
            //If a task is a recurring task,  we only display it if it is active
            if ( data.recurrence_ == null || data.recurrence_.isActive() ) {
                toDisplay.add( data );
            }
        }
        return toDisplay;
    }

    @Override
    public Object getChild( int groupPosition, int childPosition ) {
        return data_.get(groupPosition).second.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return data_.get(groupPosition).second.size();
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        View row = convertView;
        if(row == null)
        {
            LayoutInflater inflater = ((Activity) context_).getLayoutInflater();
            row = inflater.inflate(R.layout.task_view, parent, false);
        }

        TaskData taskData = data_.get(groupPosition).second.get(childPosition);
        row.setTag(taskData);
        TextView taskName = row.findViewById(R.id.TaskName);
        TextView taskQty  = row.findViewById(R.id.TaskQuantity);
        CheckBox taskCheckBox = row.findViewById(R.id.TaskCheckBox);
        taskName.setText(taskData.name_);
        taskQty.setText(taskData.qty_);
        taskCheckBox.setChecked(taskData.completed_);
        int backgroundColor = Color.WHITE;
        int paintFlags = 0;
        if ( taskData.completed_ ){
            backgroundColor = Color.LTGRAY;
            paintFlags = Paint.STRIKE_THRU_TEXT_FLAG;
        }
        row.setBackgroundColor(backgroundColor);
        taskName.setPaintFlags(paintFlags);
        taskQty.setPaintFlags(paintFlags);
        return row;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isLastChild, View view,
                             ViewGroup parent) {

        //TaskGroupHolder viewHolder;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.task_group_view, null);
        }
        TextView textView = view.findViewById(R.id.TaskGroupName);
        textView.setText( (String) getGroup( groupPosition ) );

        TextView completedView = view.findViewById(R.id.TaskGroupCompleted);
        int totalCompleted = 0;
        for ( TaskData data : data_.get(groupPosition).second ) {
            if ( data.completed_ ) {
                totalCompleted++;
            }
        }
        String displayCompleted = totalCompleted + "/" + getChildrenCount( groupPosition );
        completedView.setText( displayCompleted );
        return view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public int getGroupCount() {
        return data_.size();
    }

    @Override
    public long getGroupId( int groupPosition ) {
        return groupPosition;
    }

    @Override
    public boolean hasStableIds(){
        return true;
    }

    protected String getGroupString( TaskData data ) {
        return data.store_;
    }

    void refresh() {
        //Hard refresh on every change
        data_.clear();

        //Filter only tasks to display
        ArrayList<TaskData> toDisplay = getDisplayedTasks();

        //Create groups and children
        for ( TaskData task : toDisplay ) {
            Pair<String, ArrayList<TaskData>> foundGroup = null;
            String groupName = getGroupString( task );

            //Find existing group with same name
            for ( Pair<String, ArrayList<TaskData>> pair : data_ ){
                if ( pair.first.equals( groupName ) ) {
                    foundGroup = pair;
                    break;
                }
            }
            //If not create it
            if ( foundGroup == null ) {
                foundGroup = new Pair<>( groupName, new ArrayList<TaskData>() );
                data_.add( foundGroup );
            }

            //Add the task to the group
            foundGroup.second.add( task );
        }
        notifyDataSetChanged();
    }
}