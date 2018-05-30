package charles.courses;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.TreeMap;

public class EditRecipesActivity extends Activity {
    protected RecipeStorage.Recipe chosenRecipe_ = new RecipeStorage.Recipe( "" );
    protected boolean hideValidateButton_ = true;
    protected TaskAdapter adapter_ = null;
    static class ActivityType { static int TASK_ACTIVITY = 0, SELECT_RECIPE = 1;}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipes);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPrevArrowClicked();
            }
        });

        //We let the user choose the recipe he wants to edit
        chooseRecipe();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ( requestCode == ActivityType.TASK_ACTIVITY ) {
            onTaskResult(resultCode, data, chosenRecipe_.tasks_);

            //Refresh the task display
            adapter_.refresh();

            //Hide or show the validate recipe menu according to the changes
            invalidateOptionsMenu();

        } else if ( requestCode == ActivityType.SELECT_RECIPE ) {
            Bundle bundle = data.getExtras();
            TreeMap<String, RecipeStorage.Recipe> recipes = getRecipes().recipes_;
            if (bundle != null) {
                SelectItemActivity.Result result = (SelectItemActivity.Result) bundle.getSerializable(SelectItemActivity.ResultMarker);
                for (String removed : result.removedItems_)
                    recipes.remove(removed);

                for (SelectItemActivity.ValueChange modified : result.modifiedItems_) {
                    RecipeStorage.Recipe recipe = recipes.get(modified.oldValue_);
                    recipe.name_ = modified.newValue_;
                    recipes.put(modified.newValue_, recipe);
                    recipes.remove(modified.oldValue_);
                }

                for (String added : result.addedItems_)
                    recipes.put(added, new RecipeStorage.Recipe(added));

                if (result.selected_.isEmpty()) {
                    finish();
                    return;
                }

                initFromRecipe(result.selected_);
            } else {
                System.out.println("Error : activity action " + resultCode + " without bundled task");
                finish();
            }
        }
    }

    void initFromRecipe( String recipeName ) {
        setTitle(recipeName);
        chosenRecipe_.copyFrom( getRecipes().recipes_.get(recipeName) );

        //Initialize the spinner representing the number of people
        final Spinner nbPeople = findViewById(R.id.nb_people_spinner);
        ArrayList<String> numberSpinnerValues = new ArrayList<>();
        for ( Integer i = 1; i <= 10; i++)
            numberSpinnerValues.add( i.toString() );

        ArrayAdapter<String> adapterNumber = new ArrayAdapter<>(this, R.layout.spinner_style, numberSpinnerValues );
        adapterNumber.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        nbPeople.setAdapter(adapterNumber);
        nbPeople.setSelection( chosenRecipe_.nbPeople_ - 1 );
        nbPeople.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                chosenRecipe_.nbPeople_ = i + 1;
            }
            @Override public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        //Initialize the main view from the tasks
        ExpandableListView listView = findViewById( R.id.TaskPage );
        adapter_ = new TaskAdapter(this, chosenRecipe_.tasks_, false);
        listView.setAdapter(adapter_);
        adapter_.listView_ = listView;
        //On startup, we expect the groups to be expanded
        for ( int i = 0; i < adapter_.getGroupCount(); i++ ) {
            listView.expandGroup(i);
        }
        TextView emptyView = findViewById(android.R.id.empty);
        emptyView.setText(R.string.no_ingredients);
        listView.setEmptyView(emptyView);

        //Make long click open the modify task activity
        enableTaskModification(listView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_recipe, menu);
        menu.findItem(R.id.validate_recipe).setVisible(showValidateButton());
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.validate_recipe)
            validateRecipe();

        return super.onOptionsItemSelected(item);
    }

    void validateRecipe() {
        //Save the changes
        getStorage().recipes_.recipes_.put(chosenRecipe_.name_, chosenRecipe_);

        //Choose the next recipe to change
        chooseRecipe();
    }

    void chooseRecipe() {
        Intent intent = new Intent(EditRecipesActivity.this, SelectItemActivity.class);
        Bundle bundleOut = new Bundle();
        SelectItemActivity.Input items = new SelectItemActivity.Input();
        items.title_ = getRecipeActivityTitle();
        items.values_.addAll( getRecipes().recipes_.keySet() );
        items.enableItemsModifications_ = isRecipeModificationEnabled();
        bundleOut.putSerializable(SelectItemActivity.InputMarker, items);
        intent.putExtras(bundleOut);
        startActivityForResult(intent, ActivityType.SELECT_RECIPE);
    }

    boolean showValidateButton() {
        if ( chosenRecipe_ == null )
            return false;

        RecipeStorage.Recipe old = getRecipes().recipes_.get(chosenRecipe_.name_);
        RecipeStorage.Recipe current = chosenRecipe_;
        boolean change = old.nbPeople_ != current.nbPeople_ || old.tasks_.size() != current.tasks_.size();
        for (int i = 0; i < old.tasks_.size() && !change; i++) {
            TaskData newTask = current.tasks_.get(i);
            TaskData oldTask = old.tasks_.get(i);
            change |= !newTask.name_.equals(oldTask.name_);
            change |= !newTask.qty_.equals(oldTask.qty_);
        }
        return change;
    }

    @Override
    String getReason() {
        return chosenRecipe_.name_;
    }

    String getRecipeActivityTitle() {
        return getResources().getString(R.string.title_activity_edit_recipes);
    }

    @Override
    boolean isRecurrenceEnabled() {
        return false;
    }

    boolean isRecipeModificationEnabled() {
        return true;
    }

    void onPrevArrowClicked() {
        chooseRecipe();
    }
}
