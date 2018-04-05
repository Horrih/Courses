package charles.courses;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

class PageAdapter extends FragmentPagerAdapter {
    ArrayList<TaskAdapter> adapters_ = new ArrayList<>();
    private ArrayList<TaskData> tasks_ = new ArrayList<>();
    private MainActivity activity_;

    PageAdapter(MainActivity activity) {
        super(activity.getSupportFragmentManager());
        activity_ = activity;
        updateTasks();
        adapters_.add( new TaskAdapter( activity, tasks_ ) );
        adapters_.add( new TaskAdapter( activity, tasks_ ) {
            @Override
            protected String getGroupString( TaskData data ) {
                return data.reason_;
            }
        });
        adapters_.add( new RecurrenceTaskAdapter( activity, tasks_ ) );
    }

    private void updateTasks() {
        tasks_.clear();
        tasks_.addAll( activity_.getTasks() );
    }

    void refresh() {
        updateTasks();
        for ( TaskAdapter adapter : adapters_ ) {
            adapter.refresh();
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position) {
            case 0:
                return activity_.getResources().getString(R.string.store);
            case 1:
                return activity_.getResources().getString(R.string.reason);
            case 2:
                return activity_.getResources().getString(R.string.coming_up);
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public Fragment getItem(int position) {
        return PageFragment.newInstance(position);
    }

    public static class PageFragment extends Fragment {
        int tabNumber_ = 0;

        static PageFragment newInstance(int position) {
            PageFragment f = new PageFragment();

            // Supply position as an argument.
            Bundle args = new Bundle();
            args.putInt("position", position);
            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            tabNumber_ = getArguments().getInt("position");
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            //We create the view
            View view = inflater.inflate(R.layout.fragment_pager_list, container, false);

            //Attach the appropriate adapter
            MainActivity parentActivity = (MainActivity) getActivity();
            final PageAdapter pageAdapter = parentActivity.adapter_;
            TaskAdapter adapter = pageAdapter.adapters_.get( tabNumber_ );
            ExpandableListView listView = view.findViewById( R.id.TaskPage );
            listView.setAdapter(adapter);

            //On startup, we expect the groups to be expanded
            for ( int i = 0; i < adapter.getGroupCount(); i++ ) {
                listView.expandGroup(i);
            }
            listView.setEmptyView(view.findViewById(android.R.id.empty));
            adapter.listView_ = listView;

            //Make short click complete task
            listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                    TaskData taskData = (TaskData) v.getTag();
                    taskData.completed_  = !taskData.completed_;
                    ((MainActivity)getContext()).taskUpdate();
                    if ( taskData.completed_ && taskData.recurrence_ != null) {
                        Context context = v.getContext();

                        taskData.recurrence_.lastCompletionDate_ = new Date();
                        DateFormat dateFormat = new SimpleDateFormat("EEEE dd MMM yyyy", Locale.FRANCE);
                        String date = dateFormat.format(taskData.recurrence_.nextAvailableDate());
                        CharSequence text = getResources().getString(R.string.reactivation_message) + " " + date;
                        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                        toast.show();
                    }
                    return true;
                }
            });

            //Make long click open the modify task activity
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                        TaskData task = (TaskData) view.getTag();
                        ((MainActivity)getContext()).launchTaskActivity(task);
                        return true;
                    }
                    return false;
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
