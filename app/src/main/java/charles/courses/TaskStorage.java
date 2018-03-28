package charles.courses;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

class TaskStorage implements Serializable{
    TaskStorage( String defaultListName ) {
        defaultListName_ = defaultListName;
        newList( defaultListName );
    }

    void newList( String listName ) {
        storage_.put( listName, new Tasks());
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

        System.out.println( "Acces a la liste " + listName);
        ArrayList<TaskData> active = null;
        Tasks tasks = storage_.get( listName );
        if ( tasks != null ) {
            active = tasks.activeTasks_;
        }
        return active;
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
    }

    private final String defaultListName_;
    private TreeMap<String, Tasks> storage_ = new TreeMap<>();
}
