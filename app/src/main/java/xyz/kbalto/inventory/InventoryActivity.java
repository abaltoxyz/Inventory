package xyz.kbalto.inventory;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import xyz.kbalto.inventory.data.ProductContract.ProductEntry;

/**
 * Displays the list of products that were added and stored in the app.
 */
public class InventoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Loader identifier */
    private static final int URI_LOADER = 0;
    /** Product cursor adapter */
    ProductCursorAdapter mProductAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Set up the ListView and its Empty View
        ListView productListView = (ListView) findViewById(R.id.products_list);
        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        // Create the CursorAdapter instance and set it to the ListView
        mProductAdapter = new ProductCursorAdapter(this, null);
        productListView.setAdapter(mProductAdapter);

        // Set an onItemClickListener that passes the content URI to the EditorActivity if the product will be edited.
        productListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Set up an intent to go to the EditorActivity
                Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
                // Look for the product content URI using the id that was passed from the onItemClickListener
                Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);
                // Set the URI on the data field of the intent and send it through the EditorActivity to be handled there
                intent.setData(currentProductUri);
                startActivity(intent);
            }
        });

        // Initialize the loader for query of products.
        getLoaderManager().initLoader(URI_LOADER, null, this);
    }

    /**
     * Shows confirmation dialog when tapping on "Delete all products"
     */
    private void showDeleteConfirmationDialog(){
        // Set up the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_products_message);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User decided to delete all products
                deleteAllProducts();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Dismiss the dialog
                if (dialogInterface != null){
                    dialogInterface.dismiss();
                }
            }
        });
        // Create and show the dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }



    /**
     * Deletes all products from the database.
     */
    private void deleteAllProducts() {
        int rowsDeleted = getContentResolver().delete(ProductEntry.CONTENT_URI, null, null);
        // Show a toast message depending on whether or not the delete was successful.
        if (rowsDeleted == 0){
            // If no rows were deleted, there was an error. Display message.
            Toast.makeText(this, R.string.delete_products_failure, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.delete_products_success, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Inflates the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_inventory, menu);
        return true;
    }

    /**
     * Handles menu items behavior
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_delete_all_products:
                if (mProductAdapter.isEmpty()){
                    // If the adapter is empty, there are no products to delete
                    Toast.makeText(this, R.string.delete_no_products_error, Toast.LENGTH_SHORT).show();
                    return true;
                } else {
                    // Show confirmation and delete all products (or cancel)
                    showDeleteConfirmationDialog();
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates a loader that executes the query method on a background thread.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        // Specify column data to show using projection.
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_PICTURE,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_DESCRIPTION,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_SOLD_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_SOLD_PROFIT
        };
        return new CursorLoader(this,
                ProductEntry.CONTENT_URI,       // Content URI to query
                projection,                     // Columns to include
                null,                           // No selection clause
                null,                           // No selection arguments
                null);                          // Default sort order
    }

    /**
     * When the loader is finished,
     * updates the CursorAdapter with the new cursor containing updated data.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mProductAdapter.swapCursor(cursor);
    }

    /**
     * Resets loader and cursor data.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mProductAdapter.swapCursor(null);
    }
}
