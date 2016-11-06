package xyz.kbalto.inventory.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import xyz.kbalto.inventory.data.ProductContract.ProductEntry;

/**
 * ContentProvider for the Inventory app.
 */

public class ProductProvider extends ContentProvider {
    /** URI matcher code for the content URI for the products table. */
    private static final int PRODUCTS = 100;
    /** URI matcher code for the content URI for a single product in the products table. */
    private static final int PRODUCT_ID = 101;
    /** Database helper object. */
    private ProductDbHelper mDbHelper;
    /** Tag for Log messages */
    public static final String LOG_TAG = ProductProvider.class.getSimpleName();

    /**
     * Matches a content URI to a corresponding code
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    // Static initializer
    static {
        // Match the content URI of the form "content://xyz.kbalto.inventory/products" to the integer PRODUCTS
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS, PRODUCTS);
        // Match the content URI of the form "content://xyz.kbalto.inventory/products/#" to the integer PRODUCT_ID
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    /**
     * Initializes the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        mDbHelper = new ProductDbHelper(getContext());
        return true;
    }

    /**
     * Performs the query.
     * @param uri the given URI.
     * @param projection columns to include in the resulting cursor.
     * @param selection clause (given attribute key).
     * @param selectionArgs given attribute value.
     * @param sortOrder order to sort the Cursor rows.
     * @return the queried Cursor.
     */
    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Get a readable database (no need to modify it)
        SQLiteDatabase readableDatabase = mDbHelper.getReadableDatabase();
        // Cursor that will hold the result of the query
        Cursor cursor;

        // Match the Uri to a specific code
        int match = sUriMatcher.match(uri);
        switch (match){
            case PRODUCTS:
                // Perform the query on the whole table
                cursor = readableDatabase.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PRODUCT_ID:
                // Extract the id from the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                // Perform the query on the single product on the table
                cursor = readableDatabase.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                // Throw exception if the Uri does not match any case.
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set a notification URI on the Cursor to know what content URI the Cursor was created for. If data changes, update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match){
            case PRODUCTS:
                return ProductContract.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductContract.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    /**
     * Calls insertProduct() to add new data into the provider with the given contentValues.
     * @return the new inserted product URI.
     */
    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match){
            // Only proceed if matching the PRODUCTS code because the product needs to be inserted in the table.
            case PRODUCTS:
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Helper method that adds the product to the database.
     * @return the new inserted product URI.
     */
    private Uri insertProduct(Uri uri, ContentValues values) {
        /** "Sanity checking" the data before trying to add it to the database. */
        // Check if product name is OK (not null)
        String name = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Product requires a name");
        }

        // No need to check for product description, it can be null.

        // Check if "The Price is Right!" (price can be null -as the seller can decide that later- but it can't be negative)
        Integer price = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_PRICE);
        if (price != null && price < 0){
            throw new IllegalArgumentException("Product requires a valid price");
        }

        // Check if product quantity is OK (can't be null or negative)
        Integer quantity = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        if (quantity == null || quantity < 0){
            throw new IllegalArgumentException("Product requires a valid quantity");
        }

        // No need to check for product picture, it can be null.
        // No need to check for product sold quantity, it can be null.
        // No need to check for product sold profit, it can be null.

        /** Actually add the product to the database */
        // Get a writable database, as it needs to be modified.
        SQLiteDatabase writableDatabase = mDbHelper.getWritableDatabase();
        // Insert the product and store its returned id.
        long id = writableDatabase.insert(ProductEntry.TABLE_NAME, null, values);
        if (id == -1){
            // -1 indicates SQL insert operation error.
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify listeners that the data has changed for the content URI.
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new product content URI.
        return ContentUris.withAppendedId(uri, id);
    }



    /**
     * Calls updateProduct() to update the data at the given selection and selections arguments with the new ContentValues.
     * @param uri to update.
     * @param contentValues new values.
     * @param selection attribute key.
     * @param selectionArgs attribute value.
     * @return integer with the amount of rows affected.
     */
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return updateProduct(uri, contentValues, selection, selectionArgs);
            case PRODUCT_ID:
                // Extract id from the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateProduct(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Helper method that updates the data at the given selection and selections arguments with the new ContentValues.
     * @param uri to update.
     * @param contentValues new values.
     * @param selection attribute key.
     * @param selectionArgs attribute value.
     * @return integer with the amount of rows affected.
     */
    private int updateProduct(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs){
        /** Check whether the values existed previously or not. */
        // Check for name
        if (contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_NAME)){
            String name = contentValues.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
            if (name == null){
                throw new IllegalArgumentException("Product requires a name");
            }
        }

        // No need to check for description

        // Check for product price
        if (contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_PRICE)){
            Integer price = contentValues.getAsInteger(ProductEntry.COLUMN_PRODUCT_PRICE);
            if (price != null && price < 0){
                throw new IllegalArgumentException("Product requires a valid price");
            }
        }

        // Check for product quantity
        if (contentValues.containsKey(ProductEntry.COLUMN_PRODUCT_QUANTITY)){
            Integer quantity = contentValues.getAsInteger(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            if (quantity == null || quantity < 0){
                throw new IllegalArgumentException("Product requires a valid quantity");
            }
        }

        // No need to check for product picture
        // No need to check for product sold quantity
        // No need to check for product sold profit

        /** Actually update the product with the new values */
        // Get a writable database (needs to be modified)
        SQLiteDatabase writableDatabase = mDbHelper.getWritableDatabase();
        // Perform the update operation and store the number of rows affected
        int rowsUpdated = writableDatabase.update(ProductEntry.TABLE_NAME, contentValues, selection, selectionArgs);
        if (rowsUpdated != 0){
            // Notify the listeners that the data has changed
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    /**
     * Deletes an object or the whole table from the database.
     * @param uri the content URI.
     * @param selection attribute key.
     * @param selectionArgs attribute value.
     * @return number of rows deleted.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rowsDeleted;

        // Get a writable database (needs to be modified)
        SQLiteDatabase writableDatabase = mDbHelper.getWritableDatabase();
        // Match the content URI
        final int match = sUriMatcher.match(uri);
        switch (match){
            case PRODUCTS:
                // Delete all rows that match the selection and selection arguments and store the number of rows affected
                rowsDeleted = writableDatabase.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                if (rowsDeleted != 0){
                    // If there are rows deleted, notify the listeners
                    getContext().getContentResolver().notifyChange(uri, null);
                    return rowsDeleted;
                }
            case PRODUCT_ID:
                // Get the ID from the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                // Delete a single row given by the ID in the URI and store the number of rows affected
                rowsDeleted = writableDatabase.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                if (rowsDeleted != 0){
                    // If there are rows deleted, notify the listeners
                    getContext().getContentResolver().notifyChange(uri, null);
                    return rowsDeleted;
                }
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }
}
