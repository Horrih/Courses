package charles.courses;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddRecipeActivity extends EditRecipesActivity {

    static String OutputMarker = "Output";
    static class Output implements Serializable{
        ArrayList<TaskData> tasks_ = new ArrayList<>();
    }
    private Output output_ = new Output();
    private int nbPeople_ = 1;

    @Override
    boolean isRecipeModificationEnabled() {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_recipe, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.validate_recipe)
            validateRecipe();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        //Serialization to send result task back to the parent activity
        Bundle bundle = new Bundle();
        bundle.putSerializable(OutputMarker, output_);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult( 0, intent);
        super.finish();
    }

    void validateRecipe() {
        output_.tasks_ = chosenRecipe_.tasks_;
        finish();
    }

    @Override
    void initFromRecipe( String recipeName ) {
        super.initFromRecipe(recipeName);
        nbPeople_ = chosenRecipe_.nbPeople_;
        final Spinner nbPeople = findViewById(R.id.nb_people_spinner);
        nbPeople.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int newNbPeople = i + 1;
                updateQuantities( nbPeople_, newNbPeople );
                nbPeople_ = newNbPeople;
            }
            @Override public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    @Override
    String getRecipeActivityTitle() {
        return getResources().getString(R.string.title_activity_choose_recipe);
    }

    //Updates the quantity of each task proportionally to the new number of people
    void updateQuantities( int prevNbPeople, int newNbPeople ) {
        if ( prevNbPeople == newNbPeople )
            return;

        for ( TaskData task : chosenRecipe_.tasks_ ) {
            //Find the first number in the string : multiply it by the ratio newNumber / prevNumber
            Matcher m = Pattern.compile("([^0-9]*)([0-9]+)(.*)").matcher(task.qty_);
            if (m.matches())
                task.qty_ = m.group(1) + (int) Math.ceil(( Double.parseDouble(m.group(2) ) * newNbPeople ) / prevNbPeople ) + m.group(3);
        }

        //Update the displayed quantities
        adapter_.notifyDataSetChanged();
    }

    @Override
    void onPrevArrowClicked() {
        finish();
    }
}
