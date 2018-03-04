package charles.courses;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class TaskAdapter extends BaseExpandableListAdapter {
    PageAdapter parentAdapter_;
    private Context context_;
    private ArrayList<TaskData> original_data_ = null;
    private TreeMap<String, ArrayList<TaskData>> data_ = new TreeMap<>();

    TaskAdapter(PageAdapter parentAdapter, Context context, ArrayList<TaskData> data) {
        this.parentAdapter_ = parentAdapter;
        this.context_ = context;
        this.original_data_ = data;
        refresh();
    }

    private Map.Entry<String, ArrayList<TaskData>> getEntry( int n ) {
        int i = 0;
        for(Map.Entry<String,ArrayList<TaskData>> entry : data_.entrySet() ) {
            if ( i == n ) {
                return entry;
            }
            else {
                i++;
            }
        }
        return data_.firstEntry();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return getEntry(groupPosition).getKey();
    }

    @Override
    public Object getChild( int groupPosition, int childPosition ) {
        return getEntry(groupPosition).getValue().get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return getEntry(groupPosition).getValue().size();
    }

    public int getTaskGroup( TaskData data ) {
        int idGroup = 0;
        for(String groupName : data_.keySet() ) {
            if ( data.store_.equals( groupName ) ) {
                return idGroup;
            }
            else {
                idGroup++;
            }
        }
        return 0;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        View row = convertView;
        if(row == null)
        {
            LayoutInflater inflater = ((Activity) context_).getLayoutInflater();
            row = inflater.inflate(R.layout.task_view, parent, false);
        }

        TaskData taskData = getEntry(groupPosition).getValue().get(childPosition);
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
        for ( TaskData data : getEntry(groupPosition).getValue()) {
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
        for ( TaskData data : original_data_ ) {
            data_.put( getGroupString( data ), new ArrayList<TaskData>() );
        }
        for ( TaskData data : original_data_ ) {
            data_.get( getGroupString( data ) ).add( data );
        }
        notifyDataSetChanged();
    }
}