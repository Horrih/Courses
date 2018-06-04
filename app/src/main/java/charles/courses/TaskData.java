package charles.courses;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

class  TaskData implements java.io.Serializable
{
    String name_ = "";
    String qty_ = "";
    Boolean completed_ = false;
    String store_ = "";
    String reason_ = "";
    RecurrenceData recurrence_ = null;

    TaskData() {
        this( "");
    }

    TaskData( String name ) {
        this( name, "");
    }

    TaskData( String name, String store ) {
        this( name, store, "" );
    }

    TaskData( String name, String store, String reason) {
        this(name, store, reason, "");
    }
    TaskData( String name, String store, String reason, String qty ) {
        name_ = name;
        qty_ = qty;
        store_ = store;
        reason_ = reason;
    }

    static class RecurrenceData implements java.io.Serializable
    {
        static final ArrayList<Integer> periods = new ArrayList<>();
        static final int days = 10;
        static final int weeks = 20;
        static final int months = 30;
        static final int years = 40;

        static {
            periods.add( days );
            periods.add( weeks );
            periods.add( months );
            periods.add( years );
        }

        Date nextAvailableDate() {
            Date now = new Date();
            Calendar calendar = Calendar.getInstance();
            boolean firstDayOfWeek = false;

            //If last completion time is not available, we set the next date to yesterday so that it is always considered as available
            if ( lastCompletionDate_ == null ) {
                calendar.add( Calendar.DATE, -1 );
                calendar.setTime( now );
            }
            //Else : we add the number of period configured
            else {
                calendar.setTime( lastCompletionDate_ );
                if ( period_ == days ) {
                    calendar.add( Calendar.DATE, number_ );
                }
                else  if ( period_ == weeks ) {
                    calendar.add( Calendar.DATE, 7 * number_ );
                    firstDayOfWeek = true;
                }
                else if ( period_ == months ) {
                    calendar.add( Calendar.MONTH, number_ );
                    firstDayOfWeek = true;
                }
                else if ( period_ == years ) {
                    calendar.add( Calendar.YEAR, number_ );
                    firstDayOfWeek = true;
                }
                else {
                    System.out.println( "Error : duration type " + period_ + " is unknown" );
                }
            }
            if ( firstDayOfWeek ) {
                //We want monday to be the first day of week
                int dayOfWeek = ( calendar.get( Calendar.DAY_OF_WEEK ) + 5 ) % 7;
                calendar.add( Calendar.DATE, - dayOfWeek );
            }
            calendar.set( calendar.get( Calendar.YEAR ), calendar.get( Calendar.MONTH ), calendar.get( Calendar.DATE ), 0, 0, 0 );
            return calendar.getTime();
        }

        boolean isActive() {
            return !waitingNextOccurence_;
        }

        int period_ = days;
        int number_ = 0;
        Date lastCompletionDate_ = null;
        boolean waitingNextOccurence_ = false;
    }
}
