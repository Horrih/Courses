package charles.courses;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

//Stores the various recipes defined by the user. Must be serializable to simplify storage to a file
class RecipeStorage implements Serializable {
    static class Recipe implements Serializable {
        int nbPeople_ = 1;
        String name_ = "";
        ArrayList<TaskData> tasks_ = new ArrayList<>();
        Recipe(String name) {
            name_ = name;
        }
        void copyFrom(Recipe recipe) {
            nbPeople_ = recipe.nbPeople_;
            name_ = recipe.name_;
            //Perform a deep copy of the recipe to avoid modifying the original recipe
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject( recipe.tasks_ );
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                ObjectInputStream ois = new ObjectInputStream(bais);
                tasks_ = (ArrayList<TaskData>) ois.readObject();
            } catch ( Exception e ) {
                System.out.println( "Serialization error in AddRecipeActivity");
            }
        }
    }

    TreeMap<String, Recipe> recipes_ = new TreeMap<>();
}
