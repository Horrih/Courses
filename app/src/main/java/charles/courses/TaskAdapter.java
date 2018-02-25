package charles.courses;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

public class TaskAdapter extends ArrayAdapter<TaskData>{
    private Context context_;
    private int layoutResourceId_;
    private ArrayList<TaskData> data_ = null;

    TaskAdapter(Context context, int layoutResourceId, ArrayList<TaskData> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId_ = layoutResourceId;
        this.context_ = context;
        this.data_ = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        TaskHolder holder = null;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity) context_).getLayoutInflater();
            row = inflater.inflate(layoutResourceId_, parent, false);
            holder = new TaskHolder();
            holder.taskCheckBox_ = row.findViewById(R.id.TaskCheckBox);
            holder.taskName_ = row.findViewById(R.id.TaskName);
            holder.taskQty_ = row.findViewById(R.id.TaskQuantity);
            row.setTag(holder);
        }
        else
        {
            holder = (TaskHolder)row.getTag();
        }

        TaskData taskData = data_.get(position);
        holder.taskName_.setText(taskData.name_);
        holder.taskQty_.setText(taskData.qty_);
        holder.taskCheckBox_.setChecked(taskData.completed_);
        float transparency = 1;
        int backgroundColor = Color.WHITE;
        int paintFlags = 0;
        if ( taskData.completed_ ){
            backgroundColor = Color.LTGRAY;
            //transparency = 0.5f;
            paintFlags = Paint.STRIKE_THRU_TEXT_FLAG;
        }
        row.setBackgroundColor(backgroundColor);
        row.setAlpha(transparency);
        holder.taskName_.setPaintFlags(paintFlags);
        holder.taskQty_.setPaintFlags(paintFlags);
        return row;
    }

    static class TaskHolder
    {
        CheckBox taskCheckBox_;
        TextView taskName_;
        TextView taskQty_;
    }
}
