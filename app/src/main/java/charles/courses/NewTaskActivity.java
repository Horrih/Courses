package charles.courses;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class NewTaskActivity extends AppCompatActivity {

    protected Intent result_ = new Intent();
    protected HashMap<String, Integer> durationStringToDays_ = new HashMap<>();
    protected TaskData decodedTask_ = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);

        //By default, the new task is considered canceled so that back button returns without doing anything
        setResult( MainActivity.TaskAction.CANCELED, result_ );

        //Populating the recurrence spinner values
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
        ArrayList<Pair<String, Integer>> durationValues = new ArrayList<>();
        durationValues.add(new Pair<>( "Jours"   , TaskData.RecurrenceData.days ) );
        durationValues.add(new Pair<>( "Semaines", TaskData.RecurrenceData.weeks ) );
        durationValues.add(new Pair<>( "Mois"    , TaskData.RecurrenceData.months ) );
        durationValues.add(new Pair<>( "Années"  , TaskData.RecurrenceData.years ) );
        ArrayList<String> durationSpinnerValues = new ArrayList<>();
        for ( Pair<String, Integer> pair : durationValues ) {
            durationSpinnerValues.add( pair.first );
            durationStringToDays_.put( pair.first, pair.second );
        }
        ArrayAdapter<String> adapterDuration = new ArrayAdapter<>(this, R.layout.spinner_style, durationSpinnerValues );
        adapterDuration.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(adapterDuration);
        durationSpinner.setSelection(1);

        //We recover the task to modify if it exists
        Bundle bundle = getIntent().getExtras();
        if ( bundle != null ) {
            decodedTask_ = (TaskData) bundle.getSerializable(MainActivity.TaskDataMarker);
        }

        //If there is a task to modify, we use the task data to initialize the input widgets
        if ( decodedTask_ != null ) {
            AutoCompleteTextView input_task = findViewById(R.id.NewTaskInput);
            AutoCompleteTextView input_qty = findViewById(R.id.NewTaskQuantityInput);
            AutoCompleteTextView input_store = findViewById(R.id.NewTaskStoreInput);
            AutoCompleteTextView input_reason = findViewById(R.id.NewTaskReasonInput);
            input_task  .setText( decodedTask_.name_ );
            input_qty   .setText( decodedTask_.qty_ );
            input_store .setText( decodedTask_.store_ );
            input_reason.setText( decodedTask_.reason_ );

            //Recurrence data
            if ( decodedTask_.recurrence_ != null ) {
                Switch recurrenceSwitch = findViewById(R.id.EnableRecurrenceSwitch);
                recurrenceSwitch.setChecked( true );
                int durationId = 0;
                for ( int i = 0; i < durationValues.size(); i++ ) {
                    if ( durationValues.get(i).second == decodedTask_.recurrence_.period_ ) {
                        durationId = i;
                        break;
                    }
                }
                durationSpinner.setSelection(durationId);
                numberSpinner.setSelection( decodedTask_.recurrence_.number_ - 1 );
            }
            setTitle( "Modifier tâche" );
        }
        else {
            setTitle( "Nouvelle tâche" );
            FloatingActionButton deleteButton = findViewById(R.id.TaskDeleteButton);
            deleteButton.hide();
        }

        //We gray out the recurrence part by default
        refreshRecurrenceDisplay();
    }

    public void onConfirm(View view) {
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

    public void onDelete(View view)
    {
        setResult( MainActivity.TaskAction.DELETED, result_ );
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
}
