package xyz.kbalto.inventory.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * A centralized place to store all of the data-related constants.
 */

public class ProductContract {
    public static final String CONTENT_AUTHORITY = "xyz.kbalto.inventory";
    public static final Uri BASE_CONTENT_URI
            // content://xyz.kbalto.inventory
            = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_PRODUCTS = "products";
    /** MIME type constants */
    public static final String CONTENT_LIST_TYPE
            // vnd.android.cursor.dir/xyz.kbalto.inventory/products
            = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;
    public static final String CONTENT_ITEM_TYPE
            // vnd.android.cursor.item/xyz.kbalto.inventory/products
            = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

    /** Private empty constructor */
    private ProductContract() {
        // TODO: make nice throw implementation
    }

    /**
     * Inner entry class for the table "products"
     */
    public static abstract class ProductEntry implements BaseColumns{
        public static final Uri CONTENT_URI
                // content://xyz.kbalto.inventory/products
                = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);
        public static final String TABLE_NAME = "products";

        /** Column names for the database */
        // ID for each product.
        public static final String _ID = BaseColumns._ID;
        // Name of the product. SQL Type = TEXT
        public static final String COLUMN_PRODUCT_NAME = "name";
        // Description of the product. SQL Type = TEXT
        public static final String COLUMN_PRODUCT_DESCRIPTION = "description";
        // Price of the product. SQL Type = INTEGER DEFAULT 0
        public static final String COLUMN_PRODUCT_PRICE = "price";
        // Quantity of the product. SQL Type = INTEGER DEFAULT 0
        public static final String COLUMN_PRODUCT_QUANTITY = "quantity";
        // Picture of the product. SQL Type = TEXT (URI String)
        public static final String COLUMN_PRODUCT_PICTURE = "picture";
        // Product sold quantity. SQL Type = INTEGER
        public static final String COLUMN_PRODUCT_SOLD_QUANTITY = "soldQuantity";
        // Product sold profit ($ earned). SQL Type = INTEGER
        public static final String COLUMN_PRODUCT_SOLD_PROFIT = "soldProfit";
    }
}
