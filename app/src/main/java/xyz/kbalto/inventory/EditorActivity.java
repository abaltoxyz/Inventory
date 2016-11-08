package xyz.kbalto.inventory;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import xyz.kbalto.inventory.data.ProductContract.ProductEntry;

import static xyz.kbalto.inventory.data.ProductProvider.LOG_TAG;

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
    private Uri mProductPhotoUri;

    private ImageView mImageViewTakePicture;
    private static final int MY_PERMISSIONS_REQUEST = 2;
    private static final String FILE_PROVIDER_AUTHORITY = "xyz.kbalto.inventory.fileprovider";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String CAMERA_DIR = "/dcim/";
    private Bitmap mBitmap;


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

        mImageViewTakePicture = (ImageView) findViewById(R.id.take_picture);
        mImageViewTakePicture.setEnabled(false);

        requestPermissions();
        mImageViewTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture(view);
            }
        });

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
        mSoldQuantityView.setOnTouchListener(mTouchListener);
        mSoldProfitView.setOnTouchListener(mTouchListener);
    }

    public void requestPermissions(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            // should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                // show an explanation asynchronously
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST);
            }
        } else {
            mImageViewTakePicture.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // permissions were granted
                    mImageViewTakePicture.setEnabled(true);
                } else {
                    // permission denied, disable functionality
                }
                return;
            }
        }
    }

    public void takePicture(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File f = createImageFile();
            Log.d("takePicture()", "File: " + f.getAbsolutePath());
            mProductPhotoUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, f);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mProductPhotoUri);
            // Solution taken from http://stackoverflow.com/a/18332000/3346625
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, mProductPhotoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.i(LOG_TAG, "Received an \"Activity Result\"");
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Log.i(LOG_TAG, "Uri: " + mProductPhotoUri.toString());
            mBitmap = getBitmapFromUri(mProductPhotoUri);
            mProductImage.setImageBitmap(mBitmap);
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e){
            Log.e(LOG_TAG, "Failed to load image", e);
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e){
                e.printStackTrace();
                Log.e(LOG_TAG, "Error closing ParcelFile Descriptor");
            }
        }
    }

    private File createImageFile() throws IOException {
        // create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = new File(Environment.getExternalStorageDirectory()
                    + CAMERA_DIR
                    + getString(R.string.app_name));

            Log.d(LOG_TAG, "Dir: " + storageDir);

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d(LOG_TAG, "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
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

        // Set up the product's ContentValues
        ContentValues productValues = new ContentValues();
        productValues.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        productValues.put(ProductEntry.COLUMN_PRODUCT_DESCRIPTION, descriptionString);
        productValues.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
        productValues.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
        productValues.put(ProductEntry.COLUMN_PRODUCT_PICTURE, mProductPhotoUri.toString());

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
            Uri pictureUri = Uri.parse(pictureString);

            // Update views on the screen.
            mNameEditText.setText(name);
            mDescriptionEditText.setText(description);
            mQuantityPicker.setValue(quantity);
            mPriceEditText.setText(Integer.toString(price));
            if (mCurrentProductUri == null && pictureString == null || TextUtils.isEmpty(pictureString)){
                // If the product's image doesn't exist, set placeholder image.
                mProductImage.setImageResource(R.drawable.placeholder);
            } else {
                mProductImage.setImageURI(pictureUri);
            }
            mSoldQuantityView.setText(Integer.toString(soldQuantity));
            String soldProfitWithCurrency = getString(R.string.currency_sign) + Integer.toString(soldProfit);
            mSoldProfitView.setText(soldProfitWithCurrency);
        }

    }

    /**
     * Resets the loader.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
