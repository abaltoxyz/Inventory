package xyz.kbalto.inventory;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import xyz.kbalto.inventory.data.ProductContract.ProductEntry;

/**
 * Allows the user to create a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    /** EditText field to enter the product's name */
    private EditText mNameEditText;
    /** EditText field to enter the product's description */
    private EditText mDescriptionEditText;
    /** NumberPicker field to enter the product's quantity */
    private NumberPicker mQuantityPicker;
    /** EditText field to enter the product's price */
    private EditText mPriceEditText;
    /** ImageView for the product's image */
    private ImageView mProductImage;
    /** TextView for the product's sold quantity amount */
    private TextView mSoldQuantityView;
    /** TextView for the product's sold profit amount */
    private TextView mSoldProfitView;
    /** Identifier for the product data loader */
    private static final int EXISTING_PRODUCT_LOADER = 1;
    /** Content URI for existing product (null if new) */
    private Uri mCurrentProductUri;
    /** Boolean flag that keeps track of whether the product has been edited (true) or not (false) */
    private boolean mProductHasChanged;

    /**
     * Listens for any user touches on a View, implying that they are modifying it.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent to know if the user is creating a product or editing an existing one
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();
        if (mCurrentProductUri == null){
            // Set title for "New product" and invalidate the options menu so that the "Delete" menu item can be hidden
            setTitle(R.string.editor_activity_new_product);
            invalidateOptionsMenu();
        } else {
            // Set title for "Edit product" and load the product data from the database.
            setTitle(R.string.editor_activity_edit_product);
            // Initialize loader
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that will need to be modified
        mNameEditText = (EditText) findViewById(R.id.product_name);
        mDescriptionEditText = (EditText) findViewById(R.id.product_description);
        mPriceEditText = (EditText) findViewById(R.id.product_price);
        mProductImage = (ImageView) findViewById(R.id.product_image);
        mSoldQuantityView = (TextView) findViewById(R.id.product_sold_quantity);
        mSoldProfitView = (TextView) findViewById(R.id.product_sold_profit);

        // Set up cool wrapper EditText hints animation. Tutorial from https://code.tutsplus.com/tutorials/creating-a-login-screen-using-textinputlayout--cms-24168
        final TextInputLayout nameWrapper = (TextInputLayout) findViewById(R.id.name_wrapper);
        nameWrapper.setHint(getString(R.string.product_name));
        final TextInputLayout descriptionWrapper = (TextInputLayout) findViewById(R.id.description_wrapper);
        descriptionWrapper.setHint(getString(R.string.product_description));

        // Set up the NumberPicker for product quantity which will support up to 100 items of each product.
        mQuantityPicker = (NumberPicker) findViewById(R.id.number_picker);
        mQuantityPicker.setMinValue(0);
        mQuantityPicker.setMaxValue(100);

        // Set touch listeners
        mNameEditText.setOnTouchListener(mTouchListener);
        mDescriptionEditText.setOnTouchListener(mTouchListener);
        mQuantityPicker.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mProductImage.setOnTouchListener(mTouchListener);
        mSoldQuantityView.setOnTouchListener(mTouchListener);
        mSoldProfitView.setOnTouchListener(mTouchListener);
    }

    /**
     * Shows a confirmation for discarding changes.
     */
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener){
        // Set up the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.discard_changes_message);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int id){
                // User clicked the "Keep editing" button, so dismiss the dialog and continue editing
                if (dialog != null){
                    dialog.dismiss();
                }
            }
        });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Handles the "Back" pressed state so that the user can confirm that they want to discard changes.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press.
        if (!mProductHasChanged){
            super.onBackPressed();
            return;
        }
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        // Show dialog saying that there are unsaved changes.
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /**
     * Adds a product to the inventory database using the user's input as the product's data.
     */
    private void saveProduct(){
        String nameString = mNameEditText.getText().toString();
        String descriptionString = mDescriptionEditText.getText().toString();
        String priceString = mPriceEditText.getText().toString();
        int quantity = mQuantityPicker.getValue();
        // TODO: save image

        // If fields are empty, don't add to database
        if (mCurrentProductUri == null
                && TextUtils.isEmpty(nameString)
                && TextUtils.isEmpty(descriptionString)
                && TextUtils.isEmpty(priceString)){
            Toast.makeText(this, "fill inputs and try again!", Toast.LENGTH_SHORT).show(); // TODO: fix this
            return;
        }

        // If the price is not provided, don't parse the string and use 0 by default
        int price = 0;
        if (!TextUtils.isEmpty(priceString)){
            price = Integer.parseInt(priceString);
        }
        int soldQuantity = 0;
        int soldProfit = 0;

        // Set up the product's ContentValues
        ContentValues productValues = new ContentValues();
        productValues.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        productValues.put(ProductEntry.COLUMN_PRODUCT_DESCRIPTION, descriptionString);
        productValues.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
        productValues.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
        productValues.put(ProductEntry.COLUMN_PRODUCT_SOLD_QUANTITY, soldQuantity);
        productValues.put(ProductEntry.COLUMN_PRODUCT_SOLD_PROFIT, soldProfit);

        // Update or add the product to the database
        if (mCurrentProductUri == null){
            // ADD product to the database
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, productValues);
            if (newUri == null){
                // There was an error
                Toast.makeText(this, R.string.add_product_failure, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.add_product_success, Toast.LENGTH_SHORT).show();
            }
        } else {
            // UPDATE product in the database. null for selection and selectionArgs
            int rowsAffected = getContentResolver().update(mCurrentProductUri, productValues, null, null);
            if (rowsAffected == 0){
                // There was an error
                Toast.makeText(this, R.string.update_product_failure, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.update_product_success, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Shows a confirmation for deleting a product.
     */
    private void showDeleteConfirmationDialog() {
        // Set up the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_product_message);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss and continue editing.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Deletes a single product from the database.
     */
    private void deleteProduct(){
        // Only perform deletion if the product exists.
        if (mCurrentProductUri != null){
            // DELETE the product from the database. null for selection and selectionArgs.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
            if (rowsDeleted == 0){
                // There was an error
                Toast.makeText(this, R.string.delete_product_failure, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.delete_product_success, Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    /**
     * Inflates the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * Hides the "Delete" menu item from the overflow menu, if invalidateOptionsMenu() is called.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Hide the "Delete" menu item if this is a new product.
        if (mCurrentProductUri == null){
            MenuItem menuItem = menu.findItem(R.id.action_delete_product);
            menuItem.setVisible(false);
        }
        return true;
    }

    /**
     * Handles menu items behavior
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_save:
                saveProduct();
                finish();
                return true;
            case R.id.action_order_more_products:
                // TODO: create order intent
                return true;
            case R.id.action_delete_product:
                showDeleteConfirmationDialog();
                return true;
            // Respond to click on the "Back" arrow button in the app bar.
            case android.R.id.home:
                // If the product hasn't changed, proceed as normal.
                if (!mProductHasChanged){
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }
                // Otherwise show "Discard changes?" dialog.
                DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked on "Discard", abandon activity
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    }
                };
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Executes the ContentProvider's query method on a background thread,
     */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Specify the projection, all the columns in this case
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_DESCRIPTION,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_PICTURE,
                ProductEntry.COLUMN_PRODUCT_SOLD_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_SOLD_PROFIT
        };
        return new CursorLoader(this,
                mCurrentProductUri,         // Query the content URI for the current product
                projection,                 // Columns to show (All)
                null,                       // No selection clause
                null,                       // No selection arguments
                null);                      // Default sort order
    }

    /**
     * Handles the UI after the loader is finished so the current product data is displayed.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Move to the first row of the cursor and read data from it.
        if (cursor.moveToFirst()){
            // Find the column indices.
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int descriptionColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_DESCRIPTION);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int pictureColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PICTURE);
            int soldQuantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SOLD_QUANTITY);
            int soldProfitColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SOLD_PROFIT);

            // Extract current product's data.
            String name = cursor.getString(nameColumnIndex);
            String description = cursor.getString(descriptionColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            String pictureString = cursor.getString(pictureColumnIndex);
            int soldQuantity = cursor.getInt(soldQuantityColumnIndex);
            int soldProfit = cursor.getInt(soldProfitColumnIndex);

            // Update views on the screen.
            mNameEditText.setText(name);
            mDescriptionEditText.setText(description);
            mQuantityPicker.setValue(quantity);
            //mQuantityEditText.setText(Integer.toString(quantity));
            mPriceEditText.setText(Integer.toString(price));
            if (pictureString == null || TextUtils.isEmpty(pictureString)){
                // If the product's image doesn't exist, set placeholder image.
                mProductImage.setImageResource(R.drawable.placeholder);
            } else {
                //TODO: set image.
            }
            mSoldQuantityView.setText(Integer.toString(soldQuantity));
            mSoldProfitView.setText(Integer.toString(soldProfit));
        }

    }

    /**
     * Resets the loader.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
