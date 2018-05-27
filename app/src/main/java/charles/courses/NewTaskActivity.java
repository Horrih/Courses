package charles.courses;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public class NewTaskActivity extends AppCompatActivity {

    class TaskAction { static final int CREATED = 0, MODIFIED = 1, CANCELED = 2, DELETED = 3; }
    static String TaskDataMarker = "TaskData";
    static class Input implements Serializable
    {
        TaskData task_ = null;
        ArrayList<TaskData> history_ = new ArrayList<>();
        ArrayList<String> stores_ = new ArrayList<>();
    }

    static class Output implements Serializable
    {
        TaskData task_ = new TaskData();
        ArrayList<String> stores_ = new ArrayList<>();
    }

    private HashMap<String, Integer> durationStringToDays_ = new HashMap<>();
    private ArrayList<Pair<String, Integer>> durationValues_ = new ArrayList<>();
    private Input input_ = new Input();
    private Output output_ = new Output();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //Initialize durations
        durationValues_.add(new Pair<>( getResources().getString(R.string.days)  , TaskData.RecurrenceData.days ) );
        durationValues_.add(new Pair<>( getResources().getString(R.string.weeks) , TaskData.RecurrenceData.weeks ) );
        durationValues_.add(new Pair<>( getResources().getString(R.string.months), TaskData.RecurrenceData.months ) );
        durationValues_.add(new Pair<>( getResources().getString(R.string.years) , TaskData.RecurrenceData.years ) );

        //By default, the new task is considered canceled so that back button returns without doing anything
        setResult( TaskAction.CANCELED, new Intent() );

        //Populating the recurrence spinner values
        initSpinnerValues();

        //We recover the input data if it exists
        Bundle bundle = getIntent().getExtras();
        if ( bundle != null ) {
            input_ = (Input) bundle.getSerializable(TaskDataMarker);
        }

        //Initialize the result list of stores to the input
        output_.stores_.addAll(input_.stores_);
        Spinner storeSpinner = findViewById(R.id.NewTaskStoreInput);
        ArrayAdapter<String> adapterStores = new ArrayAdapter<>(this, R.layout.spinner_style, output_.stores_ );
        adapterStores.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        storeSpinner.setAdapter(adapterStores);

        //We initialize the various widgets from the history of tasks
        initFromHistory( input_.history_ );

        //If there is a task to modify, we use the task data to initialize the input widgets
        if ( input_.task_ != null ) {
            setTitle( getResources().getString(R.string.title_activity_modify_task) );
            initFromTask(input_.task_ , true);
        }
        else {
            setTitle(getResources().getString(R.string.title_activity_new_task));
        }

        //We reload the state of the recurrence widgets, after the state has been reloaded in case of orientation change
        toolbar.post(new Runnable() {
            @Override
            public void run() {
                refreshRecurrenceDisplay()  ;
            }
        });
    }

    private void initFromTask( TaskData task, boolean initRecurrenceData ) {
        setStore(task.store_);
        AutoCompleteTextView input_task = findViewById(R.id.NewTaskInput);
        AutoCompleteTextView input_qty = findViewById(R.id.NewTaskQuantityInput);
        AutoCompleteTextView input_reason = findViewById(R.id.NewTaskReasonInput);
        input_task.setText( task.name_ );
        input_qty .setText( task.qty_ );
        if ( !task.reason_.equals(getResources().getString(R.string.default_category))) {
            input_reason.setText( task.reason_ );
        }

        //Recurrence data
        if ( task.recurrence_ != null && initRecurrenceData) {
            Switch recurrenceSwitch = findViewById(R.id.EnableRecurrenceSwitch);
            Spinner durationSpinner = findViewById(R.id.RecurrenceDurationSpinner);
            Spinner numberSpinner = findViewById(R.id.RecurrenceNumberSpinner);
            recurrenceSwitch.setChecked( true );
            int durationId = 0;
            for ( int i = 0; i < durationValues_.size(); i++ ) {
                if ( durationValues_.get(i).second == task.recurrence_.period_ ) {
                    durationId = i;
                    break;
                }
            }
            durationSpinner.setSelection(durationId);
            numberSpinner.setSelection( task.recurrence_.number_ - 1 );
        }
    }

    private void initSpinnerValues() {
        //Number spinner
        Spinner numberSpinner = findViewById(R.id.RecurrenceNumberSpinner);
        ArrayList<String> numberSpinnerValues = new ArrayList<>();
        for ( Integer i = 1; i <= 31; i++){
            numberSpinnerValues.add( i.toString() );
        }
        ArrayAdapter<String> adapterNumber = new ArrayAdapter<>(this, R.layout.spinner_style, numberSpinnerValues );
        adapterNumber.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        numberSpinner.setAdapter(adapterNumber);

        //Duration spinner
        Spinner durationSpinner = findViewById(R.id.RecurrenceDurationSpinner);
        ArrayList<String> durationSpinnerValues = new ArrayList<>();
        for ( Pair<String, Integer> pair : durationValues_ ) {
            durationSpinnerValues.add( pair.first );
            durationStringToDays_.put( pair.first, pair.second );
        }
        ArrayAdapter<String> adapterDuration = new ArrayAdapter<>(this, R.layout.spinner_style, durationSpinnerValues );
        adapterDuration.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(adapterDuration);
        durationSpinner.setSelection(1);
    }

    private void initFromHistory( ArrayList<TaskData> tasks ) {
        Set<String> names = new TreeSet<>();
        Set<String> reasons = new TreeSet<>();
        for ( TaskData task : tasks ) {
            names.add( task.name_ );
            reasons.add(task.reason_);
        }
        if ( !tasks.isEmpty() ) {
            setStore(tasks.get(0).store_);
        }
        ArrayAdapter<String> adapterNames   = new ArrayAdapter<>(this, R.layout.completion_item, new ArrayList<>(names));
        ArrayAdapter<String> adapterReasons = new ArrayAdapter<>(this, R.layout.completion_item, new ArrayList<>(reasons));
        AutoCompleteTextView namesInput  = findViewById(R.id.NewTaskInput);
        AutoCompleteTextView quantityInput  = findViewById(R.id.NewTaskQuantityInput);
        AutoCompleteTextView reasonInput = findViewById(R.id.NewTaskReasonInput);
        loseFocusOnActionsDone(namesInput);
        loseFocusOnActionsDone(quantityInput);
        loseFocusOnActionsDone(reasonInput);
        namesInput.setAdapter(adapterNames);
        reasonInput.setAdapter(adapterReasons);
        namesInput.setThreshold(1);
        reasonInput.setThreshold(1);
        namesInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String completedText = ((TextView) view).getText().toString();
                for ( TaskData task : input_.history_ ) {
                    if ( task.name_.equals( completedText ) ) {
                        initFromTask(task, false);
                    }
                }
            }
        });
    }

    public void onConfirm() {
        int action = input_.task_ != null ? TaskAction.MODIFIED : TaskAction.CREATED;
        TaskData taskData = output_.task_;

        AutoCompleteTextView input_task = findViewById(R.id.NewTaskInput);
        String name = input_task.getText().toString();
        if ( !name.isEmpty() ) {
            //Intanciation of a TaskData object if necessary
            taskData.name_ = name;

            //Input quantity
            AutoCompleteTextView input_qty = findViewById(R.id.NewTaskQuantityInput);
            taskData.qty_ = input_qty.getText().toString();

            //Store
            taskData.store_ = getStore();

            //Reason
            AutoCompleteTextView input_reason = findViewById(R.id.NewTaskReasonInput);
            taskData.reason_ = input_reason.getText().toString();
            if ( taskData.reason_.isEmpty() )
                taskData.reason_ = getResources().getString(R.string.default_category);

            Switch recurrenceSwitch = findViewById(R.id.EnableRecurrenceSwitch);
            if ( recurrenceSwitch.isChecked() ) {
                Spinner periodSpinner = findViewById(R.id.RecurrenceDurationSpinner);
                Spinner numberSpinner = findViewById(R.id.RecurrenceNumberSpinner);
                int period = durationStringToDays_.get( periodSpinner.getSelectedItem().toString() );
                int number = Integer.parseInt(numberSpinner.getSelectedItem().toString());
                if ( taskData.recurrence_ == null ) {
                    taskData.recurrence_ = new TaskData.RecurrenceData();
                }
                taskData.recurrence_.period_ = period;
                taskData.recurrence_.number_ = number;
            }

            //Serialization to send result back to MainActivity
            Intent result = new Intent();
            Bundle bundle = new Bundle();
            bundle.putSerializable(TaskDataMarker, output_);
            result.putExtras(bundle);
            setResult( action, result);
        }
        finish();
    }

    public void onActivateRecurrence(View view)
    {
        //No particular action : just refresh the color of the widgets
        refreshRecurrenceDisplay();
    }

    //This functions refreshes the color of the Recurrence widgets : gray if switch is disabled
    public void refreshRecurrenceDisplay()
    {
        //We recover the state of the button
        Switch switchButton = findViewById(R.id.EnableRecurrenceSwitch);
        boolean enabled = switchButton.isChecked();

        //We update the color of the recurrence widgets
        Spinner numberSpinner = findViewById(R.id.RecurrenceNumberSpinner);
        Spinner durationSpinner = findViewById(R.id.RecurrenceDurationSpinner);
        TextView recurrenceTextView = findViewById(R.id.ActivateRecurrenceTextView);
        TextView durationTextView = findViewById(R.id.RecurrenceTimeTextView);
        recurrenceTextView.setEnabled(enabled);
        durationTextView.setEnabled(enabled);
        numberSpinner.setEnabled(enabled);
        durationSpinner.setEnabled(enabled);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_task, menu);

        //We hide the delete menu item if we are not modifying an existing task
        if ( input_.task_ == null ) {
            MenuItem item = menu.findItem(R.id.delete_task);
            item.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.validate_task) {
            onConfirm();
        } else if ( id == R.id.delete_task ){
            setResult( TaskAction.DELETED, new Intent() );
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bundle bundle = data.getExtras();
        if ( bundle != null ) {
            SelectItemActivity.Result result = (SelectItemActivity.Result) bundle.getSerializable( SelectItemActivity.ResultMarker );
            output_.stores_.clear();
            output_.stores_.addAll(result.updatedList_);
            setStore(result.selected_);
            Spinner storeSpinner = findViewById(R.id.NewTaskStoreInput);
            ((ArrayAdapter<String>)storeSpinner.getAdapter()).notifyDataSetChanged();
        } else {
            System.out.println( "Error : new task activity action " + resultCode + " without bundled task" );
        }
    }

    private String getStore() {
        Spinner stores = findViewById(R.id.NewTaskStoreInput);
        String store = getResources().getString(R.string.default_category);
        if ( !output_.stores_.isEmpty() ) {
            store = output_.stores_.get(stores.getSelectedItemPosition());
        }
        return store;
    }

    private void setStore(String store) {
        Spinner stores = findViewById(R.id.NewTaskStoreInput);
        for ( int id = 0; id < output_.stores_.size(); id++ ) {
            if ( output_.stores_.get(id).equals( store ) ) {
                stores.setSelection(id);
                return;
            }
        }
    }

    public void onModifyStores(View view) {
        Intent intent = new Intent(this, SelectItemActivity.class);
        Bundle bundle = new Bundle();
        SelectItemActivity.Input items = new SelectItemActivity.Input();
        items.title_ = getResources().getString(R.string.choose_store_activity);
        items.values_ = output_.stores_;
        items.selected_ = getStore();
        bundle.putSerializable(SelectItemActivity.InputMarker, items);
        intent.putExtras(bundle);
        startActivityForResult(intent,0);
    }

    void loseFocusOnActionsDone(final AutoCompleteTextView text) {
        text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if( actionId == EditorInfo.IME_ACTION_DONE){
                    text.post(new Runnable() {
                        @Override
                        public void run() {
                            text.clearFocus();
                        }
                    });
                }
                return false;
            }
        });
    }
}
