package charles.courses;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public class NewTaskActivity extends AppCompatActivity {

    protected Intent result_ = new Intent();
    protected HashMap<String, Integer> durationStringToDays_ = new HashMap<>();
    protected TaskData decodedTask_ = null;
    static private ArrayList<Pair<String, Integer>> durationValues_ = new ArrayList<>();

    static {
        durationValues_.add(new Pair<>( "Jours"   , TaskData.RecurrenceData.days ) );
        durationValues_.add(new Pair<>( "Semaines", TaskData.RecurrenceData.weeks ) );
        durationValues_.add(new Pair<>( "Mois"    , TaskData.RecurrenceData.months ) );
        durationValues_.add(new Pair<>( "Années"  , TaskData.RecurrenceData.years ) );
    }

    @Override
    @SuppressWarnings("unchecked")
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

        //By default, the new task is considered canceled so that back button returns without doing anything
        setResult( MainActivity.TaskAction.CANCELED, result_ );

        //Populating the recurrence spinner values
        initSpinnerValues();

        //We recover the task to modify if it exists
        Bundle bundle = getIntent().getExtras();
        if ( bundle != null ) {
            decodedTask_ = (TaskData) bundle.getSerializable(MainActivity.TaskDataMarker);
            initFromHistory( (ArrayList) bundle.getSerializable(MainActivity.TaskHistoryMarker) );
        }

        //If there is a task to modify, we use the task data to initialize the input widgets
        if ( decodedTask_ != null ) {
            setTitle( "Modifier tâche" );
            initFromTask( decodedTask_ );
        }
        else {
            setTitle( "Nouvelle tâche" );
            //FloatingActionButton deleteButton = findViewById(R.id.TaskDeleteButton);
            //deleteButton.hide();
        }

        //We gray out the recurrence part by default
        refreshRecurrenceDisplay();
    }

    private void initFromTask( TaskData task ) {
        AutoCompleteTextView input_task = findViewById(R.id.NewTaskInput);
        AutoCompleteTextView input_qty = findViewById(R.id.NewTaskQuantityInput);
        AutoCompleteTextView input_store = findViewById(R.id.NewTaskStoreInput);
        AutoCompleteTextView input_reason = findViewById(R.id.NewTaskReasonInput);
        input_task  .setText( task.name_ );
        input_qty   .setText( task.qty_ );
        input_store .setText( task.store_ );
        input_reason.setText( task.reason_ );

        //Recurrence data
        if ( task.recurrence_ != null ) {
            Switch recurrenceSwitch = findViewById(R.id.EnableRecurrenceSwitch);
            Spinner durationSpinner = findViewById(R.id.RecurrenceDurationSpinner);
            Spinner numberSpinner = findViewById(R.id.RecurrenceNumberSpinner);
            recurrenceSwitch.setChecked( true );
            int durationId = 0;
            for ( int i = 0; i < durationValues_.size(); i++ ) {
                if ( durationValues_.get(i).second == decodedTask_.recurrence_.period_ ) {
                    durationId = i;
                    break;
                }
            }
            durationSpinner.setSelection(durationId);
            numberSpinner.setSelection( decodedTask_.recurrence_.number_ - 1 );
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
        Set<String> stores = new TreeSet<>();
        Set<String> reasons = new TreeSet<>();
        for ( TaskData task : tasks ) {
            names.add( task.name_ );
            stores.add(task.store_);
            reasons.add(task.reason_);
        }
        ArrayAdapter<String> adapterNames   = new ArrayAdapter<>(this, R.layout.completion_item, new ArrayList<>(names));
        ArrayAdapter<String> adapterStores  = new ArrayAdapter<>(this, R.layout.completion_item, new ArrayList<>(stores));
        ArrayAdapter<String> adapterReasons = new ArrayAdapter<>(this, R.layout.completion_item, new ArrayList<>(reasons));
        AutoCompleteTextView namesInput  = findViewById(R.id.NewTaskInput);
        AutoCompleteTextView storesInput = findViewById(R.id.NewTaskStoreInput);
        AutoCompleteTextView reasonInput = findViewById(R.id.NewTaskReasonInput);
        namesInput.setAdapter(adapterNames);
        storesInput.setAdapter(adapterStores);
        reasonInput.setAdapter(adapterReasons);
        namesInput.setThreshold(1);
        storesInput.setThreshold(1);
        reasonInput.setThreshold(1);
    }

    public void onConfirm() {
        int action = decodedTask_ != null ? MainActivity.TaskAction.MODIFIED : MainActivity.TaskAction.CREATED;
        TaskData taskData = decodedTask_;
        if ( taskData == null ) {
            taskData = new TaskData();
        }

        AutoCompleteTextView input_task = findViewById(R.id.NewTaskInput);
        String name = input_task.getText().toString();
        if ( !name.isEmpty() ) {
            //Intanciation of a TaskData object if necessary
            taskData.name_ = name;

            //Input quantity
            AutoCompleteTextView input_qty = findViewById(R.id.NewTaskQuantityInput);
            taskData.qty_ = input_qty.getText().toString();

            //Store
            AutoCompleteTextView input_store = findViewById(R.id.NewTaskStoreInput);
            taskData.store_ = input_store.getText().toString();
            if ( taskData.store_.isEmpty() )
                taskData.store_ = "General";

            //Reason
            AutoCompleteTextView input_reason = findViewById(R.id.NewTaskReasonInput);
            taskData.reason_ = input_reason.getText().toString();
            if ( taskData.reason_.isEmpty() )
                taskData.reason_ = "General";

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
            Bundle bundle = new Bundle();
            bundle.putSerializable(MainActivity.TaskDataMarker, taskData);
            result_.putExtras(bundle);
            setResult( action, result_);
        }
        finish();
    }

    public void onActivateRecurrence(View view)
    {
        //No particular action : just refresh en color of the widgets
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
        if ( decodedTask_ == null ) {
            MenuItem item = menu.findItem(R.id.delete_task);
            item.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.validate_task) {
            onConfirm();
        } else if ( id == R.id.delete_task ){
            setResult( MainActivity.TaskAction.DELETED, result_ );
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
