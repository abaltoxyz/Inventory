package xyz.kbalto.inventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import xyz.kbalto.inventory.data.ProductContract.ProductEntry;

/**
 * TODO: include Class description
 */

public class ProductDbHelper extends SQLiteOpenHelper {
    // Constant for database name
    private static final String DATABASE_NAME = "inventory.db";
    // Constant for database version
    private static final int DATABASE_VERSION = 1;
    // Constant for SQL command used to create the table
    public static final String SQL_CREATE_ENTRIES =
            // CREATE TABLE products (_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            // name TEXT NOT NULL, description TEXT, price INTEGER DEFAULT 0,
            // quantity INTEGER NOT NULL DEFAULT 0, picture TEXT, soldQuantity INTEGER, soldProfit INTEGER);
            "CREATE TABLE "
                    + ProductEntry.TABLE_NAME + " ("
                    + ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, "
                    + ProductEntry.COLUMN_PRODUCT_DESCRIPTION + " TEXT, "
                    + ProductEntry.COLUMN_PRODUCT_PRICE + " INTEGER DEFAULT 0, "
                    + ProductEntry.COLUMN_PRODUCT_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                    + ProductEntry.COLUMN_PRODUCT_PICTURE + " TEXT, "
                    + ProductEntry.COLUMN_PRODUCT_SOLD_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                    + ProductEntry.COLUMN_PRODUCT_SOLD_PROFIT + " INTEGER NOT NULL DEFAULT 0);";
    // Constant for SQL command used to delete the table TODO: find out if needed.

    /** Constructor method. Cursor factory is set to null in order to use default. */
    public ProductDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Creates the SQL database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("onCreateDB", SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: find out if needs implementation.
    }
}
