package charles.courses;

import android.app.Application;
import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;

public class ApplicationWithStorage extends Application {
    String currentList_ = "";
    TaskStorage tasks_ = null;
    RecipeStorage recipes_ = null;
    private String backupFile_ = "CoursesBackup.save";

    @Override
    public void onCreate() {
        //Reload data from previous executions
        loadBackup();
        super.onCreate();
    }


    //Store all tasks and recipes to a file
    void backupData() {
        try (
                FileOutputStream outputStream = openFileOutput( backupFile_, Context.MODE_PRIVATE);
                ObjectOutputStream out = new ObjectOutputStream(outputStream);
        )
        {
            out.writeObject(currentList_);
            out.writeObject(tasks_);
            out.writeObject(recipes_);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBackup() {
        try (
                FileInputStream inputStream = openFileInput( backupFile_ );
                ObjectInputStream in = new ObjectInputStream(inputStream);
        )
        {
            currentList_ = (String) in.readObject();
            tasks_ = (TaskStorage) in.readObject();
            recipes_ = (RecipeStorage) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ( currentList_ == null || currentList_.isEmpty() )
            currentList_ = getResources().getString(R.string.default_list_name);

        if ( tasks_ == null )
            tasks_ = new TaskStorage( getResources() );

        if ( recipes_ == null )
            recipes_ = new RecipeStorage();

        for ( String listName : tasks_.getLists() )
            tasks_.getHistory(listName).removeAll(Collections.singleton(null));
    }
}
