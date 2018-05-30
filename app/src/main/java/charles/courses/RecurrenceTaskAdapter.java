package charles.courses;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

public class RecurrenceTaskAdapter extends TaskAdapter {
    RecurrenceTaskAdapter(Context context, ArrayList<TaskData> data) {
        super( context, data, true );
    }

    @Override
    protected String getGroupString( TaskData data ) {
        Date nextDate = data.recurrence_.nextAvailableDate();
        double days = ( nextDate.getTime() - new Date().getTime() ) / ( 1000 * 3600 * 24 );
        if ( days < 1 ) {
            return context_.getResources().getString(R.string.tomorrow);
        } else if ( days < 7 ) {
            return context_.getResources().getString(R.string.this_week);
        } else if ( days < 31 ) {
            return context_.getResources().getString(R.string.this_month);
        } else if ( days < 366 ) {
            return context_.getResources().getString(R.string.this_year);
        } else {
            return context_.getResources().getString(R.string.next_year);
        }
    }

    @Override
    protected ArrayList<TaskData> getDisplayedTasks(){
        ArrayList<TaskData> toDisplay = new ArrayList<>();
        TreeMap<Date, TaskData> sortedTasks = new TreeMap<>();
        for ( TaskData data : original_data_ ) {
            //If a task is a recurring task,  we only display it if it is active
            if ( data.recurrence_ != null && !data.recurrence_.isActive() ) {
                sortedTasks.put( data.recurrence_.nextAvailableDate(), data );
            }
        }
        toDisplay.addAll( sortedTasks.values() );
        return toDisplay;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        //We will display the next occurrence date of the task in addition to the rest
        View view  = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
        TaskData task = (TaskData) view.getTag();
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE );
        String nextDate = df.format( task.recurrence_.nextAvailableDate() );

        //Setting the value
        TextView nextOccurrence = view.findViewById(R.id.TaskExpiryDate);
        nextOccurrence.setText( nextDate );
        return view;
    }

    @Override
    void sortGroups() {
        Collections.sort(data_, new Comparator<Pair<String,ArrayList<TaskData>>>() {
            //We display the groups by date : the first being the ones arriving next
            @Override
            public int compare(Pair<String, ArrayList<TaskData>> group1, Pair<String, ArrayList<TaskData>> group2) {
                Date date1 = group1.second.get(0).recurrence_.nextAvailableDate();
                Date date2 = group2.second.get(0).recurrence_.nextAvailableDate();
                return date1.compareTo( date2 );
            }
        });
    }
}
