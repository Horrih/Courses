package charles.courses;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

class PageAdapter extends FragmentPagerAdapter {
    private ArrayList<TaskAdapter> adapters_ = new ArrayList<>();
    private Context context_;
    private ArrayList<TaskData> items_;

    PageAdapter(FragmentManager fm, Context context, ArrayList<TaskData> items) {
        super(fm);
        context_ = context;
        items_ = items;
    }

    void refresh() {
        for ( TaskAdapter adapter : adapters_ ) {
            adapter.refresh();
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public Fragment getItem(int position) {
        PageFragment fragment = PageFragment.newInstance(position);
        if ( adapters_.size() <= position) {
            TaskAdapter newAdapter;
            if ( position == 1 ) {
                newAdapter = new TaskAdapter(this, context_, items_) {
                    @Override
                    protected String getGroupString( TaskData data ) {
                        return data.reason_;
                    }
                };
            }
            else if ( position == 2 ){
                newAdapter = new TaskAdapter(this, context_, items_) {
                    @Override
                    protected String getGroupString( TaskData data ) {
                        Date nextDate = data.recurrence_.nextAvailableDate();
                        double days = ( nextDate.getTime() - new Date().getTime() ) / ( 1000 * 3600 * 24 );
                        if ( days < 2 ) {
                            return "Demain";
                        } else if ( days < 8 ) {
                            return "Dans la semaine";
                        } else if ( days < 32 ) {
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
                        System.out.println( "Debut tri" );
                        for ( TaskData value : sortedTasks.values() ) {
                            System.out.println( "Tri Date : " + value.recurrence_.nextAvailableDate() );
                            toDisplay.add( value );
                        }
                        return toDisplay;
                    }
                };
            }
            else {
                newAdapter = new TaskAdapter(this, context_, items_);
            }
            adapters_.add( newAdapter );
        }
        fragment.adapter_ = adapters_.get( position );
        return fragment;
    }

    public static class PageFragment extends Fragment {
        int fragmentPosition_ = 0;
        public TaskAdapter adapter_;

        static PageFragment newInstance(int position) {
            PageFragment f = new PageFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putInt("position", position);

            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            fragmentPosition_ = getArguments().getInt("position");
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_pager_list, container, false);
            ExpandableListView listView = view.findViewById( R.id.TaskPage );
            listView.setAdapter(adapter_);
            listView.setEmptyView(view.findViewById(android.R.id.empty));
            listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                    TaskData taskData = (TaskData) v.getTag();
                    taskData.completed_  = !taskData.completed_;
                    adapter_.parentAdapter_.refresh();
                    if ( taskData.completed_ && taskData.recurrence_ != null) {
                        Context context = v.getContext();

                        taskData.recurrence_.lastCompletionDate_ = new Date();
                        DateFormat dateFormat = new SimpleDateFormat("EEEE dd MMM yyyy", Locale.FRANCE);
                        String date = dateFormat.format(taskData.recurrence_.nextAvailableDate());
                        CharSequence text = "Cette tâche récurrente se réactivera automatiquement le " + date + " à minuit";
                        int duration = Toast.LENGTH_LONG;
                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                    }
                    return true;
                }
            });
            return view;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        }
    }
}
