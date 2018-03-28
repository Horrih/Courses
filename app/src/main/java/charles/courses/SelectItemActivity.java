package charles.courses;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;

import java.io.Serializable;
import java.util.ArrayList;

public class SelectItemActivity extends AppCompatActivity {

    static class Input implements java.io.Serializable {
        String title_ = "";
        String selected_ = "";
        ArrayList<String> values_ = new ArrayList<>();
    }

    static class ValueChange implements Serializable {
        ValueChange( String oldValue, String newValue ) {
            oldValue_ = oldValue;
            newValue_ = newValue;
        }
        String oldValue_ = "";
        String newValue_ = "";
    }

    static class Result implements java.io.Serializable {
        String selected_ = "";
        ArrayList<String> addedItems_ = new ArrayList<>();
        ArrayList<String> removedItems_ = new ArrayList<>();
        ArrayList<ValueChange> modifiedItems_ = new ArrayList<>();
    }

    static String InputMarker = "InputMarker";
    static String ResultMarker = "ResultMarker";
    Input input_ = new Input();
    Result result_ = new Result();
    SelectItemAdapter adapter_ = null;
    ListView listView_ = null;
    EditText textEdited_ = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_item);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //Decode the choices
        Bundle bundle = getIntent().getExtras();
        if ( bundle != null ) {
            Input decodedItems = (Input) bundle.getSerializable(InputMarker);
            if ( decodedItems != null ) {
                input_ = decodedItems;
                result_.selected_ = decodedItems.selected_;
                setTitle( decodedItems.title_ );
            }
        }
        //Display the choices
        adapter_ = new SelectItemAdapter(this, new ArrayList<String>() );
        listView_ = findViewById(R.id.ItemsList);
        listView_.setAdapter(adapter_);

        //Initialize the return value
        updateResult();
    }

    static class SelectItemAdapter extends ArrayAdapter<String> {
        ArrayList<String> items_;
        SelectItemActivity activity_;
        EditText editedText_;

        SelectItemAdapter(SelectItemActivity activity, ArrayList<String> displayItems) {
            super(activity, 0, displayItems );
            activity_ = activity;
            items_ = displayItems;
            activity_.result_.selected_ = activity.input_.selected_;
            items_.addAll( activity_.input_.values_);
        }

        @Override
        public void notifyDataSetChanged() {

            super.notifyDataSetChanged();
        }

        //Returns the selected id from the selected string
        private int getSelectedId() {
            for ( int i = 0; i < items_.size(); i++ ) {
                if ( items_.get( i ).equals( activity_.result_.selected_ ) ) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup listView) {
            View v = convertView;
            if (v == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                v = inflater.inflate(R.layout.select_item, null);
            }
            final String item = getItem( position );

            //Check the appropriate selector
            final RadioButton selector = v.findViewById(R.id.item_selector);
            selector.setChecked( getSelectedId() == position );
            selector.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity_.result_.selected_ = item;
                    notifyDataSetChanged();
                }
            });

            //Display the item
            final EditText text = v.findViewById(R.id.item_text);
            if ( !text.getText().toString().equals( item ) ) {
                text.setText(item);
            }

            text.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {}

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    items_.set( position, s.toString() );
                    activity_.result_.selected_ = s.toString();
                }
            });
            if ( item.isEmpty() ) {
                editItem(text);
            }

            //Edit button : make it possible to edit items
            ImageButton edit_button = v.findViewById(R.id.edit_item);
            edit_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    editItem( text );
                }
            });

            //Remove button : make it possible to remove items
            ImageButton remove_button = v.findViewById(R.id.remove_item);
            remove_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Remove it from the display list
                    items_.remove(position);

                    //Remove it from the original list and add it to the removed ones in the result
                    if ( activity_.input_.values_.size() > position ) {
                        activity_.result_.removedItems_.add( activity_.input_.values_.get( position ));
                        activity_.input_.values_.remove(position);
                    }
                    notifyDataSetChanged();
                }
            });
            return v;
        }

        void editItem( EditText item ) {
            item.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            item.setSelection(item.getText().length());
            editedText_ = item;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_select_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.add_item ) {
            adapter_.items_.add( "" );
            result_.selected_ = "";
            adapter_.notifyDataSetChanged();
        } else if ( id == R.id.validate_items ){
            updateResult();
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    void updateResult() {
        //Compute the modified and added list
        for ( int i = 0; i < input_.values_.size(); i++ ) {
            String oldItem = input_.values_.get( i );
            String newItem = adapter_.getItem(i);
            if ( !oldItem.equals(newItem) ) {
                result_.modifiedItems_.add( new ValueChange( oldItem, newItem ) );
            }
        }

        for ( int i = input_.values_.size(); i < adapter_.getCount(); i++ ) {
            result_.addedItems_.add( adapter_.getItem(i));
        }

        //Serialization to send result back to MainActivity
        Bundle bundle = new Bundle();
        bundle.putSerializable(ResultMarker, result_);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult( 0, intent);
    }

    @Override
    protected void onPause() {
        //Hide the keyboard if there is an edit text begin edited
        if ( adapter_.editedText_ != null ){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(adapter_.editedText_.getWindowToken(), 0);
        }
        super.onPause();
    }
}
