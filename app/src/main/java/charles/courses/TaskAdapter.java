package charles.courses;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class TaskAdapter extends BaseExpandableListAdapter {
    Context context_;
    ArrayList<TaskData> original_data_ = null;
    ExpandableListView listView_ = null;
    protected ArrayList<Pair<String, ArrayList<TaskData>>> data_ = new ArrayList<>();

    TaskAdapter(Context context, ArrayList<TaskData> data) {
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
        CheckBox taskCheckBox = row.findViewById(R.id.TaskCheckBox);
        int nameColor = row.getResources().getColor(R.color.text);
        int qtyColor = row.getResources().getColor(R.color.colorAccent);
        int paintFlags = 0;
        if ( taskData.completed_ ){
            paintFlags = Paint.STRIKE_THRU_TEXT_FLAG;
            int gray = row.getResources().getColor(R.color.textGrayed);
            nameColor = gray;
            qtyColor = gray;
        }

        //Display the quantity in a different color
        String displayText = taskData.name_;
        int endName = displayText.length();
        int startQty = endName;
        if ( !taskData.qty_.isEmpty() ) {
            String spaces = "    ";
            String qty = taskData.qty_;

            //We display a "x" symbol, e.g. Beers x12,  if the quantity is a simple number
            if ( android.text.TextUtils.isDigitsOnly( qty ) ) {
                qty = "x" + qty;
            }
            startQty = endName + spaces.length();
            displayText += spaces + qty;
        }
        SpannableString ss =  new SpannableString( displayText );
        ss.setSpan(new ForegroundColorSpan(nameColor), 0, endName, 0 );
        ss.setSpan(new ForegroundColorSpan(qtyColor), startQty, displayText.length(), 0);
        taskName.setText( ss );
        taskCheckBox.setChecked(taskData.completed_);
        taskName.setPaintFlags(paintFlags);
        return row;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isLastChild, View view, ViewGroup parent) {

        //TaskGroupHolder viewHolder;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context_.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.task_group_view, null);
        }
        TextView textView = view.findViewById(R.id.TaskGroupName);
        textView.setText( ((String) getGroup( groupPosition )).toUpperCase() );

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

    //This functions recomputes all the data to display
    void refresh() {
        ArrayList<Pair<String, ArrayList<TaskData>>> prevData = new ArrayList<>(data_);

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
        sortGroups();

        //Expand groups of newly added elements
        if ( listView_ != null ) {
            for ( int i = 0; i < data_.size(); i++ ) {
                if ( prevData.size() <= i || data_.get( i ).second.size() > prevData.get( i ).second.size() ) {
                    listView_.expandGroup(i);
                }
            }
        }

        notifyDataSetChanged();
    }

    String toText() {
        String result = "";
        for ( int group = 0; group < getGroupCount(); group++ ) {
            result += "\n- " + getGroup(group);
            for ( int item = 0; item < getChildrenCount(group); item++ ) {
                TaskData data = (TaskData) getChild(group, item);
                result += "\n    * " + data.name_;
                if ( !data.qty_.isEmpty() ) {
                    result += " (" + data.qty_ + ")";
                }
            }
        }
        return result;
    }

    void sortGroups() {
        Collections.sort(data_, new Comparator<Pair<String,ArrayList<TaskData>>>() {
            @Override
            public int compare(Pair<String, ArrayList<TaskData>> group1, Pair<String, ArrayList<TaskData>> group2) {
                //Sorting while taking into account that abc is closer to ABC than to ZBC for a human (unlike ASCII comparison)
                //Same of accented characters é is expected to be between e and f, not after z
                Collator frCollator = Collator.getInstance(Locale.FRANCE);
                frCollator.setStrength(Collator.PRIMARY);
                return  frCollator.compare( group1.first, group2.first );
            }
        });
    }
}