package charles.courses;

import android.content.Intent;
import android.graphics.Color;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);

        //By default, the new task is considered canceled
        setResult( MainActivity.NewTaskAction.CANCELED, result_ );

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
        durationValues.add(new Pair<>( "Jours", 1 ) );
        durationValues.add(new Pair<>( "Semaines", 7 ) );
        durationValues.add(new Pair<>( "Mois", 30 ) );
        durationValues.add(new Pair<>( "Années", 365 ) );
        ArrayList<String> durationSpinnerValues = new ArrayList<>();
        for ( Pair<String, Integer> pair : durationValues ) {
            durationSpinnerValues.add( pair.first );
            durationStringToDays_.put( pair.first, pair.second );
        }
        ArrayAdapter<String> adapterDuration = new ArrayAdapter<>(this, R.layout.spinner_style, durationSpinnerValues );
        adapterDuration.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(adapterDuration);
        durationSpinner.setSelection(1);

        //We gray out the recurrence part by default
        refreshRecurrenceDisplay();
    }

    public void onConfirm(View view) {
        AutoCompleteTextView input_task = findViewById(R.id.NewTaskInput);
        String name = input_task.getText().toString();
        if ( !name.isEmpty() ) {
            //Intanciation of a TaskData object
            TaskData taskData = new TaskData();
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

            //Serialization to send result back to MainActivity
            Bundle bundle = new Bundle();
            bundle.putSerializable(MainActivity.TaskDataMarker, taskData);
            result_.putExtras(bundle);
            setResult( MainActivity.NewTaskAction.CREATED, result_);
        }
        finish();
    }

    public void onCancel(View view)
    {
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
