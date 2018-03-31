package charles.courses;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
        result_.selected_ = input_.selected_;
        updateResult();
    }

    static class SelectItemAdapter extends ArrayAdapter<String> {
        ArrayList<String> items_;
        SelectItemActivity activity_;
        int selected_ = 0;
        EditText editedText_;

        SelectItemAdapter(SelectItemActivity activity, ArrayList<String> displayItems) {
            super(activity, 0, displayItems );
            activity_ = activity;
            items_ = displayItems;
            items_.addAll( activity_.input_.values_);
            for ( int i = 0; i < items_.size(); i++ ) {
                if ( items_.get( i ).equals( activity_.input_.selected_ ) ) {
                    selected_ = i;
                }
            }
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull final ViewGroup parentView) {
            View v = convertView;
            final ListView listView = (ListView) parentView;
            boolean mustCreate = v == null;
            if (mustCreate) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                v = inflater.inflate(R.layout.select_item, null);
            }

            //Check the appropriate selector
            final RadioButton selector = v.findViewById(R.id.item_selector);
            final ImageButton edit_button = v.findViewById(R.id.edit_item);
            final ImageButton remove_button = v.findViewById(R.id.remove_item);
            final EditText text = v.findViewById(R.id.item_text);
            selector.setTag( position );
            edit_button.setTag( position );
            remove_button.setTag( position );
            text.setTag(position);

            //Check the selector
            boolean isSelected = selected_ == position;
            boolean selectionChange = selector.isChecked() != isSelected;
            selector.setChecked( isSelected );

            //Display the item
            final String item = getItem( position );
            if ( !text.getText().toString().equals( item ) ) {
                text.setText(item);
            }

            System.out.println( "Getting view " + position + " for item " + item + " selectionchange=" + selectionChange + " selected = " + selected_ );
            if ( item.isEmpty() && !text.hasFocus() ) {
                System.out.println( "Requesting focus" );
                editItem(text, listView);
            }

            //Edit the listener if it's the first initialization
            if ( mustCreate ) {
                selector.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        selected_ = (int) selector.getTag();
                        notifyDataSetChanged();
                    }
                });

                text.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        items_.set((int) text.getTag(), s.toString());
                    }
                });
                text.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        //Detect if this is an actual focus change
                        System.out.println( "Vue " + text.getText().toString() + ( hasFocus ? " gagne" : " perd") + " le focus" );
                        if ( !hasFocus || editedText_ == text ) {
                            return;
                        }

                        //If the view is too far down, we scroll to it before displaying the keyboard so that the view does not get destroyed
                        int pos = (int) v.getTag();
                        listView.setSelection(pos);
                        editedText_ = text;
                        //Wait for focus to be settled before opening keyboard
                        text.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println( "Asking to show the keyboard");
                                //We show the keyboard
                                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(text, InputMethodManager.SHOW_IMPLICIT);

                                //Showing the keyboard will refresh the main view and lose the focus again : we request it again
                                text.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        System.out.println( "Asking to refresh focus after keyboard");
                                        text.requestFocus();
                                    }
                                }, 500);
                            }
                        }, 500);
                    }
                });
                //Edit button : make it possible to edit items
                edit_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        editItem(text, (ListView) listView);
                    }
                });

                //Remove button : make it possible to remove items
                remove_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Remove it from the display list
                        int current_pos = (int) remove_button.getTag();
                        items_.remove(current_pos);

                        //Remove it from the original list and add it to the removed ones in the result
                        if (activity_.input_.values_.size() > current_pos) {
                            activity_.result_.removedItems_.add(activity_.input_.values_.get(current_pos));
                            activity_.input_.values_.remove(current_pos);
                        }
                        notifyDataSetChanged();
                    }
                });
            }
            return v;
        }

        void editItem( final EditText item, final ListView list ) {
            item.postDelayed( new Runnable() {
                @Override
                public void run() {
                    editedText_ = null;
                    item.requestFocus();
                    item.setSelection(item.getText().length());
                }
            }, 200);
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
            adapter_.selected_ = adapter_.getCount() - 1;
            adapter_.notifyDataSetChanged();
        } else if ( id == R.id.validate_items ){
            updateResult();
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    void updateResult(){
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
        result_.selected_ = adapter_.getItem( adapter_.selected_ );

        //Deep copy the result
        Result result = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(result_);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            result = (Result) ois.readObject();
        } catch ( IOException|ClassNotFoundException ex ) {
            result = result_;
        }

        //Serialization to send result back to MainActivity
        Bundle bundle = new Bundle();
        bundle.putSerializable(ResultMarker, result);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult( 0, intent);
    }
}
