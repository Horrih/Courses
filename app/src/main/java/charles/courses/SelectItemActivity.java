package charles.courses;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
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
    RecyclerView listView_ = null;

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
        adapter_ = new SelectItemAdapter(this );
        listView_ = findViewById(R.id.ItemsList);
        listView_.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        listView_.setItemAnimator(new DefaultItemAnimator());
        listView_.setAdapter(adapter_);

        //Default result
        updateResult();
    }

    @Override
    public void finish() {
        updateResult();
        super.finish();
    }

    void updateResult() {
        //Compute the modified and added list
        for ( int i = 0; i < input_.values_.size(); i++ ) {
            String oldItem = input_.values_.get( i );
            String newItem = adapter_.items_.get(i);
            if ( !oldItem.equals(newItem) ) {
                result_.modifiedItems_.add( new ValueChange( oldItem, newItem ) );
            }
        }

        for ( int i = input_.values_.size(); i < adapter_.getItemCount(); i++ ) {
            result_.addedItems_.add( adapter_.items_.get(i));
        }
        result_.selected_ = adapter_.items_.get( adapter_.selected_ );

        //Serialization to send result back to MainActivity
        Bundle bundle = new Bundle();
        bundle.putSerializable(ResultMarker, result_);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult( 0, intent);
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
            adapter_.newItem();
        }
        return super.onOptionsItemSelected(item);
    }

    class SelectItemAdapter extends RecyclerView.Adapter<SelectItemAdapter.SelectItemViewHolder> {
        final ArrayList<String> items_ = new ArrayList<>();
        SelectItemActivity activity_;
        int selected_ = 0;

        class SelectItemViewHolder extends RecyclerView.ViewHolder{
            RadioButton selector_;
            EditText text_;
            ImageButton editButton_;
            ImageButton removeButton_;
            SelectItemViewHolder(final View itemView) {
                super(itemView);
                selector_     = itemView.findViewById(R.id.item_selector);
                text_         = itemView.findViewById(R.id.item_text);
                editButton_   = itemView.findViewById(R.id.edit_item);
                removeButton_ = itemView.findViewById(R.id.remove_item);
                text_.addTextChangedListener( new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        items_.set( getAdapterPosition(), charSequence.toString() );
                    }

                    @Override public void afterTextChanged(Editable editable) {}
                });
                selector_.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selected_ = getAdapterPosition();
                        finish();
                    }
                });
                removeButton_.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Remove it from the lists
                        int positionRemoved = getAdapterPosition();
                        if ( positionRemoved < activity_.input_.values_.size() ) {
                            activity_.result_.removedItems_.add(activity_.input_.values_.get( positionRemoved ));
                            activity_.input_.values_.remove(positionRemoved);
                        }
                        items_.remove(positionRemoved);
                        notifyItemRemoved(positionRemoved);

                        //Update the selected item if it was after, after a delay to have the remove item animation
                        if ( positionRemoved <= selected_ && selected_ > 0 ) {
                            listView_.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    selected_ = selected_ - 1;
                                    notifyItemChanged(selected_);
                                }
                            }, 200);
                        }
                    }
                });

                editButton_.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        InputMethodManager mgr = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        text_.requestFocus();
                        text_.setSelection( text_.getText().toString().length() );
                        mgr.showSoftInput(text_, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        }

        SelectItemAdapter(SelectItemActivity activity){
            activity_ = activity;
            items_.addAll(activity.input_.values_);
            for ( int i = 0; i < items_.size(); i++ ) {
                if ( items_.get(i).equals( activity.input_.selected_ ) ) {
                    selected_ = i;
                }
            }
        }

        @Override
        public int getItemCount() {
            return items_.size();
        }

        @Override @NonNull
        public SelectItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.select_item,parent, false);
            return new SelectItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final SelectItemViewHolder holder, int position) {
            holder.text_.setText(items_.get(position));
            holder.selector_.setChecked( position == selected_ );
        }

        void newItem() {
            items_.add( "" );
            final int lastPosition = adapter_.getItemCount() - 1;
            adapter_.notifyItemInserted(lastPosition);

            //Post scroll event to the last item after update is done
            listView_.post( new Runnable() {
                @Override public void run() {
                    final LinearLayoutManager layout = (LinearLayoutManager) listView_.getLayoutManager();
                    layout.scrollToPosition( lastPosition );

                    //Post click event after the scroll has been completed
                    listView_.post( new Runnable() {
                        @Override public void run() {
                            int firstVisible = layout.findFirstVisibleItemPosition();
                            int lastVisible = layout.findLastVisibleItemPosition();
                            System.out.println("First " + firstVisible + " last " + lastVisible + " pour position " + lastPosition);
                            View view = listView_.getChildAt(lastVisible - firstVisible);
                            view.findViewById(R.id.edit_item).callOnClick();
                        }
                    });
                }
            });
        }
    }
}
