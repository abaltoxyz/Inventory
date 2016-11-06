package xyz.kbalto.inventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import xyz.kbalto.inventory.data.ProductContract.ProductEntry;

/**
 * An adapter for the ListView that uses a Cursor of product data as its data source.
 */


public class ProductCursorAdapter extends CursorAdapter {

    /** Superclass constructor
     */
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Creates a new blank list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false);
    }

    /**
     * Binds the product data in the current row pointed by the cursor to the given list item layout.
     */
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Find fields to populate.
        TextView nameView = (TextView) view.findViewById(R.id.item_product_name);
        ImageView imageView = (ImageView) view.findViewById(R.id.item_product_image);
        TextView descriptionView = (TextView) view.findViewById(R.id.item_product_description);
        final TextView quantityView = (TextView) view.findViewById(R.id.item_product_quantity);
        TextView priceView = (TextView) view.findViewById(R.id.item_product_price);

        // Find the columns of product attributes that we're interested in
        int idColumnIndex= cursor.getColumnIndexOrThrow(ProductEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PICTURE);
        int descriptionColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_DESCRIPTION);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int soldQuantityColumnIndex = cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_SOLD_QUANTITY);
        int soldProfitColumnIndex = cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_SOLD_PROFIT);

        Log.i("bindView", "priceColumnIndex= " + priceColumnIndex);
        Log.i("bindView", "soldQuantityColumnIndex= " + soldQuantityColumnIndex);
        Log.i("bindView", "soldProfitColumnIndex= " + soldProfitColumnIndex);

        // Read attributes from the cursor for the current product
        final int productId = cursor.getInt(idColumnIndex);
        String productName = cursor.getString(nameColumnIndex);
        String productImage = cursor.getString(imageColumnIndex);
        String productDescription = cursor.getString(descriptionColumnIndex);
        final int productQuantity = cursor.getInt(quantityColumnIndex);
        final int productPrice = cursor.getInt(priceColumnIndex);
        final int productSoldQuantity = cursor.getInt(soldQuantityColumnIndex);
        final int productSoldProfit = cursor.getInt(soldProfitColumnIndex);

        // Populate fields with extracted properties
        nameView.setText(productName);
        if (productImage == null || TextUtils.isEmpty(productImage)){
            // If image URI is null or empty, set placeholder image
            imageView.setImageResource(R.drawable.placeholder);
        } else {
            // Otherwise, set correct image
            imageView.setImageURI(Uri.parse(productImage)); //TODO: check if correct
        }
        if (TextUtils.isEmpty(productDescription)){
            // If product description is empty, set "No description available"
            descriptionView.setText(R.string.no_description);
        } else {
            // Otherwise, set correct description
            descriptionView.setText(productDescription);
        }
        quantityView.setText(Integer.toString(productQuantity));
        priceView.setText(Integer.toString(productPrice));

        Button sellButton = (Button) view.findViewById(R.id.sell_button);
        sellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentValues values  = new ContentValues();
                if (productQuantity > 0){
                    Integer itemId = productId;
                    Integer newProductQuantity = productQuantity - 1;
                    Integer newProductSold = productSoldQuantity + 1;
                    Integer newProductProfit = (productSoldProfit + 1) * productPrice;
                    values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, newProductQuantity);
                    values.put(ProductEntry.COLUMN_PRODUCT_SOLD_QUANTITY, newProductSold);
                    values.put(ProductEntry.COLUMN_PRODUCT_SOLD_PROFIT, newProductProfit);
                    Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, itemId);
                    context.getContentResolver().update(currentProductUri, values, null, null);
                }
                context.getContentResolver().notifyChange(ProductEntry.CONTENT_URI, null);
            }
        });
    }
}
