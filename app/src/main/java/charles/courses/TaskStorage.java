package charles.courses;

import android.content.res.Resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

class TaskStorage implements Serializable{
    TaskStorage(Resources resources) {
        defaultListName_ = resources.getString(R.string.default_list_name);
        defaultStoreNames_.add( resources.getString(R.string.fruits_vegetables));
        defaultStoreNames_.add( resources.getString(R.string.fresh_section));
        defaultStoreNames_.add( resources.getString(R.string.cans));
        defaultStoreNames_.add( resources.getString(R.string.frozen_food));
        defaultStoreNames_.add( resources.getString(R.string.cleaning));
        newList( defaultListName_ );
    }
    void newList( String listName ) {
        storage_.put( listName, new Tasks());
        setStores( listName, defaultStoreNames_ );
     }

    void removeList( String listName ) {
        storage_.remove(listName);
        if ( storage_.isEmpty() ) {
            newList( defaultListName_ );
        }
    }

    void renameList( String oldName, String newName ) {
        Tasks tasks = storage_.get( oldName );
        storage_.remove( oldName );
        storage_.put( newName, tasks );
    }

    ArrayList<TaskData> getTasks(String listName) {
        ArrayList<TaskData> active = null;
        Tasks tasks = storage_.get( listName );
        if ( tasks != null ) {
            active = tasks.activeTasks_;
        }
        return active;
    }

    ArrayList<String> getStores(String listName) {
        ArrayList<String> stores = new ArrayList<>();
        Tasks tasks = storage_.get( listName );
        if ( tasks != null ) {
            stores.addAll( tasks.stores_ );
        }
        return stores;
    }

    void setStores( String listName, ArrayList<String> stores ) {
        Tasks tasks = storage_.get( listName );
        if ( tasks != null ) {
            tasks.stores_.clear();
            tasks.stores_.addAll(stores);
            if ( tasks.stores_.isEmpty() ) {
                tasks.stores_.addAll( defaultStoreNames_);
            }
        }
    }

    ArrayList<TaskData> getHistory(String listName) {
        ArrayList<TaskData> history = null;
        Tasks tasks = storage_.get( listName );
        if ( tasks != null ) {
            history = tasks.history_;
        }
        return history;
    }

    ArrayList<String> getLists() {
        ArrayList<String> result = new ArrayList<>();
        result.addAll( storage_.keySet() );
        return result;
    }

    static class Tasks implements Serializable {
        ArrayList<TaskData> activeTasks_ = new ArrayList<>();
        ArrayList<TaskData> history_ = new ArrayList<>();
        ArrayList<String> stores_ = new ArrayList<>();
    }

    private final String defaultListName_;
    private TreeMap<String, Tasks> storage_ = new TreeMap<>();
    private ArrayList<String> defaultStoreNames_ = new ArrayList<>();
}
