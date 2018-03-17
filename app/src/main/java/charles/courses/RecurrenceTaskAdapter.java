package charles.courses;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

public class RecurrenceTaskAdapter extends TaskAdapter {
    RecurrenceTaskAdapter(Context context, ArrayList<TaskData> data) {
        super( context, data );
    }

    @Override
    protected String getGroupString( TaskData data ) {
        Date nextDate = data.recurrence_.nextAvailableDate();
        double days = ( nextDate.getTime() - new Date().getTime() ) / ( 1000 * 3600 * 24 );
        if ( days < 1 ) {
            return "Demain";
        } else if ( days < 7 ) {
            return "Dans la semaine";
        } else if ( days < 31 ) {
            return "Dans le mois";
        } else if ( days < 366 ) {
            return "Dans l'année";
        } else {
            return "Dans plus d'un an";
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
        //We will display the date instead of quantity
        View view  = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
        TaskData task = (TaskData) view.getTag();
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE );
        String nextDate = df.format( task.recurrence_.nextAvailableDate() );

        //Setting the value
        TextView nextOccurrence = view.findViewById(R.id.TaskQuantity);
        nextOccurrence.setText( nextDate );
        return view;
    }
}
