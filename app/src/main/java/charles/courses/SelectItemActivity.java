package charles.courses;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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

import java.util.ArrayList;

public class SelectItemActivity extends AppCompatActivity {

    static class Items implements java.io.Serializable {
        String title_ = "";
        String selected_ = "";
        ArrayList<String> values_ = new ArrayList<>();
    }

    static String ItemsMarker = "ItemsMarker";
    Items items_ = new Items();
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

        //Initialize the return value
        setResult( items_ );

        //Decode the choices
        Bundle bundle = getIntent().getExtras();
        if ( bundle != null ) {
            Items decodedItems = (Items) bundle.getSerializable(ItemsMarker);
            if ( decodedItems != null ) {
                items_ = decodedItems;
            }
        }
        setTitle( items_.title_ );

        //Display the choices
        adapter_ = new SelectItemAdapter(this, items_.selected_, items_.values_);
        listView_ = findViewById(R.id.ItemsList);
        listView_.setAdapter(adapter_);
    }

    static class SelectItemAdapter extends ArrayAdapter<String> {
        ArrayList<String> items_;
        String selectedItem_ = "";

        SelectItemAdapter(Context context, String selected, ArrayList<String> items) {
            super(context, 0, items);
            items_ = items;
            selectedItem_ = selected;
        }

        @Override
        public void notifyDataSetChanged() {

            super.notifyDataSetChanged();
        }

        //Returns the selected id from the selected string
        private int getSelectedId() {
            for ( int i = 0; i < items_.size(); i++ ) {
                if ( items_.get( i ).equals( selectedItem_  ) ) {
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

            //Check the appropriate selector
            final RadioButton selector = v.findViewById(R.id.item_selector);
            final String item = getItem( position );
            selector.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedItem_ = item;
                    notifyDataSetChanged();
                }
            });
            boolean checked = getSelectedId() == position;
            System.out.println( "Checked = " + checked  + " selected = " + getSelectedId() + " pos = " +position );
            selector.setChecked( getSelectedId() == position );

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
                    selectedItem_ = s.toString();
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
                    items_.remove(position);
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
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_select_item, menu);
        for(int i = 0; i < menu.size(); i++){
            Drawable drawable = menu.getItem(i).getIcon();
            if(drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(getResources().getColor(R.color.background), PorterDuff.Mode.SRC_ATOP);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.add_item ) {
            items_.values_.add( "" );
            adapter_.selectedItem_ = "";
            adapter_.notifyDataSetChanged();
        } else if ( id == R.id.validate_items ){
            setResult( items_ );
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    void setResult( Items items ) {
        //Serialization to send result back to MainActivity
        Bundle bundle = new Bundle();
        bundle.putSerializable(ItemsMarker, items);
        Intent result = new Intent();
        result.putExtras(bundle);
        setResult( 0, result);
    }
}
