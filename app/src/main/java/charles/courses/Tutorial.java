package charles.courses;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.ExpandableListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.util.Date;

//This class handles the tutorial that the user will encounter on first use
public class Tutorial implements OnShowcaseEventListener {
    enum Step {
        START,
        WELCOME,
        NEW_TASK_BUTTON,
        NEW_TASK_ACTIVITY,
        NEW_TASK_NAME,
        NEW_TASK_QUANTITY,
        NEW_TASK_REASON,
        NEW_TASK_STORE,
        NEW_TASK_ENABLE_RECURRENCE,
        NEW_TASK_DURATION_RECURRENCE,
        NEW_TASK_FINISH,
        CROSS_TASK,
        MODIFY_TASK,
        REMOVE_COMPLETED,
        SHARE_TASKS,
        TAB1,
        GO_TAB2,
        TAB2,
        GO_TAB3,
        TAB3,
        OPEN_NAVIGATION_DRAWER,
        MULTI_LIST,
        CREATE_RECIPE,
        ADD_RECIPE,
        END,
        CLEANUP,
        NONE
    }
    Step step_ = Step.NONE;
    String activeList_ = "";
    private Activity activity_ = null;
    private MainActivity mainActivity_ = null;

    private void delayedStep() {
        delayedStep(0);
    }

    private void delayedStep(int delayMs){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                nextTutorialStep(activity_);
            }
        }, delayMs);
    }

    void nextTutorialStep(final Activity activity) {
        if (step_ == Step.NONE)
            return;

        activity_ = activity;
        final Resources res = activity.getResources();
        if (step_ == Step.START) {
            //Initialize main activity and a tutorial list and recipe
            mainActivity_ = (MainActivity) activity_;
            String newListName = res.getString(R.string.tutorial);
            activeList_ = activity.getCurrentList();
            activity.getStorage().currentList_ = newListName;
            activity.getStorage().tasks_.newList(newListName);
            activity.getStorage().recipes_.recipes_.put(res.getString(R.string.tutorial), new RecipeStorage.Recipe(""));

            //Dialog box to choose if tutorial should end or proceed
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(activity);
            builder.setTitle(res.getString(R.string.tuto_launch_title))
                    .setMessage(res.getString(R.string.tuto_launch_field))
                    .setPositiveButton(R.string.tuto_proceed_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            delayedStep();
                        }
                    })
                    .setNegativeButton(R.string.tuto_proceed_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Bypass all the steps and go to the end of the tutorial to show how to replay it
                            step_ = Step.END;

                            //Open the tutorial drawer and go to the next step
                            DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
                            drawer.openDrawer(Gravity.START);
                            drawer.post(new Runnable() {
                                @Override
                                public void run() {
                                    nextTutorialStep(activity);
                                }
                            });
                        }
                    }).show();
        } else if ( step_ == Step.WELCOME ) {
            showcase(activity, null, res.getString(R.string.tuto_welcome_title), res.getString(R.string.tuto_welcome_field));
        } else if (step_ == Step.NEW_TASK_BUTTON) {
            FloatingActionButton button = activity.findViewById(R.id.NewTaskButton);
            showcase(activity, button, res.getString(R.string.tuto_add_task_title), res.getString(R.string.tuto_add_task_field));
        } else if (step_ == Step.NEW_TASK_ACTIVITY) {
            activity.onNewTask(activity.findViewById(R.id.NewTaskButton));
        } else if (step_ == Step.NEW_TASK_NAME) {
            AutoCompleteTextView name = activity.findViewById(R.id.NewTaskInput);
            AutoCompleteTextView qty = activity.findViewById(R.id.NewTaskQuantityInput);
            AutoCompleteTextView reason = activity.findViewById(R.id.NewTaskReasonInput);
            Spinner store = activity.findViewById(R.id.NewTaskStoreInput);
            name.setText(activity.getResources().getString(R.string.tuto_add_task_name));
            qty.setText(activity.getResources().getString(R.string.tuto_add_task_qty));
            reason.setText(activity.getResources().getString(R.string.tuto_add_task_reason));
            store.setSelection(2);
            name.setFocusable(false);
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            showcase(activity, name, res.getString(R.string.tuto_choose_name_title), res.getString(R.string.tuto_choose_name_field));
        } else if (step_ == Step.NEW_TASK_QUANTITY) {
            AutoCompleteTextView button = activity.findViewById(R.id.NewTaskQuantityInput);
            showcase(activity, button, res.getString(R.string.tuto_choose_qty_title), res.getString(R.string.tuto_choose_qty_field));
        } else if (step_ == Step.NEW_TASK_REASON) {
            AutoCompleteTextView button = activity.findViewById(R.id.NewTaskReasonInput);
            showcase(activity, button, res.getString(R.string.tuto_choose_reason_title), res.getString(R.string.tuto_choose_reason_field));
        } else if (step_ == Step.NEW_TASK_STORE) {
            Spinner button = activity.findViewById(R.id.NewTaskStoreInput);
            showcase(activity, button, res.getString(R.string.tuto_choose_store_title), res.getString(R.string.tuto_choose_store_field));
        } else if (step_ == Step.NEW_TASK_ENABLE_RECURRENCE) {
            Switch button = activity.findViewById(R.id.EnableRecurrenceSwitch);
            button.setChecked(true);
            ((NewTaskActivity) activity).refreshRecurrenceDisplay();
            showcase(activity, button, res.getString(R.string.tuto_recurrence_enable_title), res.getString(R.string.tuto_recurrence_enable_field));
        } else if (step_ == Step.NEW_TASK_DURATION_RECURRENCE) {
            Spinner number = activity.findViewById(R.id.RecurrenceNumberSpinner);
            Spinner duration = activity.findViewById(R.id.RecurrenceDurationSpinner);
            number.setSelection(1);
            duration.setSelection(2);
            showcase(activity, number, res.getString(R.string.tuto_recurrence_time_title), res.getString(R.string.tuto_recurrence_time_field));
        } else if (step_ == Step.NEW_TASK_FINISH) {
            String pizza = res.getString(R.string.tuto_task_reason_pizza);
            String party = res.getString(R.string.tuto_task_reason_party);
            String fresh = res.getString(R.string.fresh_section);
            String fruits = res.getString(R.string.fruits_vegetables);
            String cans = res.getString(R.string.cans);

            //Pizza tasks
            activity.getTasks().add(new TaskData(res.getString(R.string.tuto_task_name_flour), cans, pizza, "200g"));
            activity.getTasks().add(new TaskData(res.getString(R.string.tuto_task_name_yeast), cans, pizza, "15g"));
            activity.getTasks().add(new TaskData(res.getString(R.string.tuto_task_name_mozza), fresh, pizza, "125g"));
            activity.getTasks().add(new TaskData(res.getString(R.string.tuto_task_name_tomato), cans, pizza, "200g"));
            TaskData task = new TaskData(res.getString(R.string.tuto_task_name_salad), fruits, pizza, "100g");
            task.completed_ = true;
            activity.getTasks().add(task);

            //Party tasks
            activity.getTasks().add(new TaskData(res.getString(R.string.tuto_task_name_tortilla), cans, party, "8"));
            activity.getTasks().add(new TaskData(res.getString(R.string.tuto_task_name_maize), cans, party, "200g"));
            activity.getTasks().add(new TaskData(res.getString(R.string.tuto_task_name_peppers), fruits, party, "1"));
            activity.getTasks().add(new TaskData(res.getString(R.string.tuto_task_name_guacamole), cans, party));
            activity.getTasks().add(new TaskData(res.getString(R.string.tuto_task_name_chips), cans, party));

            //Recurrent tasks
            recurrentTask(res.getString(R.string.tuto_task_name_water), 5);
            recurrentTask(res.getString(R.string.tuto_task_name_milk), 25);

            //Return to the main activity
            activity.finish();
            activity_ = mainActivity_;
            delayedStep(1000);
        } else if (step_ == Step.CROSS_TASK) {
            ExpandableListView tasks = mainActivity_.taskPager_.getFocusedChild().findViewById(R.id.TaskPage);
            View task = tasks.getChildAt( 9).findViewById(R.id.TaskName);
            showcase(activity, task, res.getString(R.string.tuto_cross_task_title), res.getString(R.string.tuto_cross_task_field));
        } else if (step_ == Step.MODIFY_TASK) {
            ExpandableListView tasks = mainActivity_.taskPager_.getFocusedChild().findViewById(R.id.TaskPage);
            View task = tasks.getChildAt(9).findViewById(R.id.TaskName);
            showcase(activity, task, res.getString(R.string.tuto_modify_task_title), res.getString(R.string.tuto_modify_task_field));
        } else if (step_ == Step.REMOVE_COMPLETED) {
            showcase(activity, activity.findViewById(R.id.clear_tasks), res.getString(R.string.tuto_remove_crossed_title), res.getString(R.string.tuto_remove_crossed_field));
        } else if ( step_ == Step.SHARE_TASKS ) {
            showcase(activity, activity.findViewById(R.id.share_tasks), res.getString(R.string.tuto_share_tasks_title), res.getString(R.string.tuto_share_tasks_field));
        } else if ( step_ == Step.TAB1 ) {
            TabLayout tabs = activity.findViewById(R.id.sliding_tabs);
            View tabButton = ((ViewGroup)tabs.getChildAt(0)).getChildAt(0);
            showcase(activity, tabButton, res.getString(R.string.tuto_tab1_title), res.getString(R.string.tuto_tab1_field));
        } else if ( step_ == Step.GO_TAB2 ) {
            TabLayout tabs = activity.findViewById(R.id.sliding_tabs);
            tabs.getTabAt(1).select();
            delayedStep();
        } else if ( step_ == Step.TAB2 ) {
            TabLayout tabs = activity.findViewById(R.id.sliding_tabs);
            View tabButton = ((ViewGroup) tabs.getChildAt(0)).getChildAt(1);
            showcase(activity, tabButton, res.getString(R.string.tuto_tab2_title), res.getString(R.string.tuto_tab2_field));
        } else if ( step_ == Step.GO_TAB3 ) {
            TabLayout tabs = activity.findViewById(R.id.sliding_tabs);
            tabs.getTabAt(2).select();
            delayedStep();
        } else if ( step_ == Step.TAB3 ) {
            TabLayout tabs = activity.findViewById(R.id.sliding_tabs);
            View tabButton = ((ViewGroup)tabs.getChildAt(0)).getChildAt(2);
            showcase(activity, tabButton, res.getString(R.string.tuto_tab3_title), res.getString(R.string.tuto_tab3_field));
        } else if ( step_ == Step.OPEN_NAVIGATION_DRAWER ) {
            TabLayout tabs = activity.findViewById(R.id.sliding_tabs);
            tabs.getTabAt(0).select();
            DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
            drawer.openDrawer(Gravity.START);
            delayedStep();
        } else if ( step_ == Step.MULTI_LIST ) {
            NavigationView navigationView = activity_.findViewById(R.id.NavigationView);
            View view = ((ViewGroup) navigationView.getChildAt(0)).getChildAt(1);
            showcase(activity_, view, res.getString(R.string.tuto_multi_list_title), res.getString(R.string.tuto_multi_list_field));
        } else if ( step_ == Step.CREATE_RECIPE ) {
            NavigationView navigationView = activity_.findViewById(R.id.NavigationView);
            View view = ((ViewGroup) navigationView.getChildAt(0)).getChildAt(2);
            showcase(activity_, view, res.getString(R.string.tuto_recipe_title), res.getString(R.string.tuto_recipe_field));
        } else if ( step_ == Step.ADD_RECIPE ) {
            FloatingActionButton addRecipe = activity.findViewById(R.id.NewRecipeButton);
            showcase(activity_, addRecipe, res.getString(R.string.tuto_add_recipe_title), res.getString(R.string.tuto_add_recipe_field));
        } else if ( step_ == Step.END ) {
            NavigationView navigationView = activity_.findViewById(R.id.NavigationView);
            View view = ((ViewGroup) navigationView.getChildAt(0)).getChildAt(3);
            showcase(activity_, view, res.getString(R.string.tuto_end_title), res.getString(R.string.tuto_end_field));;
        } else if ( step_ == Step.CLEANUP ) {
            DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
            drawer.closeDrawer(Gravity.START);
            String tutorial = res.getString(R.string.tutorial);
            activity_.getStorage().tasks_.removeList(tutorial);
            activity_.getStorage().recipes_.recipes_.remove(tutorial);
            if ( activeList_.equals(tutorial) )
                activeList_ = activity.getStorage().tasks_.getLists().get(0);
            activity_.getStorage().currentList_ = activeList_;
            mainActivity_.taskUpdate();
            activity_ = null;
            mainActivity_ = null;
            activeList_ = "";
        }
        step_ = Step.values()[step_.ordinal() + 1];
    }

    private ShowcaseView showcase( Activity activity, View view, String title, String text ) {
        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.CENTER_HORIZONTAL);
        int margin = ((Number) (activity.getResources().getDisplayMetrics().density * 12)).intValue();
        lps.setMargins(margin, margin, margin, margin);
        Target target = ( view == null ? ViewTarget.NONE : new ViewTarget(view) );
        final ShowcaseView sv = new ShowcaseView.Builder(activity)
                .withNewStyleShowcase()
                .setTarget(target)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(R.style.CustomShowCase)
                .blockAllTouches()
                .setShowcaseEventListener(this)
                .build();
        sv.setButtonPosition(lps);
        sv.setButtonText( activity.getResources().getString( step_ != Step.END ? R.string.tuto_next : R.string.tuto_end ) );
        return sv;
    }

    private TaskData recurrentTask(String text, int nbDays) {
        TaskData task = new TaskData( text);
        task.recurrence_ = new TaskData.RecurrenceData();
        task.recurrence_.waitingNextOccurence_ = true;
        task.recurrence_.lastCompletionDate_ = new Date();
        task.recurrence_.lastCompletionDate_ = task.recurrence_.nextAvailableDate();
        task.recurrence_.number_ = nbDays;
        //Wait a bit to have different timestamps on each task
        try
        {
            Thread.sleep(1);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
        activity_.getTasks().add(task);
        return task;
    }

    @Override
    public void onShowcaseViewShow(ShowcaseView showcaseView) {}

    @Override
    public void onShowcaseViewHide(ShowcaseView showcaseView) {}

    @Override
    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
        delayedStep();
    }

    @Override
    public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {}
}
