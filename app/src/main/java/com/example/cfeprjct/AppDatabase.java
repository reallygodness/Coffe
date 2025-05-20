package com.example.cfeprjct;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.cfeprjct.DAOS.AddressDAO;
import com.example.cfeprjct.DAOS.CartItemDAO;
import com.example.cfeprjct.DAOS.CourierDAO;
import com.example.cfeprjct.DAOS.DeliveryDAO;
import com.example.cfeprjct.DAOS.DessertDAO;
import com.example.cfeprjct.DAOS.DishDAO;
import com.example.cfeprjct.DAOS.DrinkDAO;
import com.example.cfeprjct.DAOS.DrinkIngredientDAO;
import com.example.cfeprjct.DAOS.FavoriteDrinkDAO;
import com.example.cfeprjct.DAOS.IngredientDAO;
import com.example.cfeprjct.DAOS.OrderDAO;
import com.example.cfeprjct.DAOS.OrderStatusDAO;
import com.example.cfeprjct.DAOS.OrderedDessertDAO;
import com.example.cfeprjct.DAOS.OrderedDishDAO;
import com.example.cfeprjct.DAOS.OrderedDrinkDAO;
import com.example.cfeprjct.DAOS.PriceListDAO;
import com.example.cfeprjct.DAOS.ReviewDAO;
import com.example.cfeprjct.DAOS.VolumeDAO;
import com.example.cfeprjct.Entities.Address;
import com.example.cfeprjct.Entities.CartItem;
import com.example.cfeprjct.Entities.Courier;
import com.example.cfeprjct.Entities.Delivery;
import com.example.cfeprjct.Entities.FavoriteDrink;
import com.example.cfeprjct.Entities.Order;
import com.example.cfeprjct.Entities.OrderedDessert;
import com.example.cfeprjct.Entities.OrderedDish;
import com.example.cfeprjct.Entities.OrderedDrink;
import com.example.cfeprjct.Entities.OrderStatus;
import com.example.cfeprjct.Entities.Dessert;
import com.example.cfeprjct.Entities.Dish;
import com.example.cfeprjct.Entities.Drink;
import com.example.cfeprjct.Entities.DrinkIngredient;
import com.example.cfeprjct.Entities.Ingredient;
import com.example.cfeprjct.Entities.PriceList;
import com.example.cfeprjct.Entities.Review;
import com.example.cfeprjct.Entities.Volume;
import com.example.cfeprjct.User;

@Database(
        entities = {
                User.class,
                Address.class,
                Delivery.class,
                OrderedDrink.class,
                Order.class,
                FavoriteDrink.class,
                Ingredient.class,
                Courier.class,
                Drink.class,
                DrinkIngredient.class,
                Volume.class,
                Review.class,
                PriceList.class,
                OrderStatus.class,
                Dish.class,
                OrderedDish.class,
                Dessert.class,
                OrderedDessert.class,
                CartItem.class
        },
        version = 14,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract CartItemDAO cartItemDao();
    public abstract UserDAO userDAO();
    public abstract AddressDAO addressDAO();
    public abstract DeliveryDAO deliveryDAO();
    public abstract OrderedDrinkDAO orderedDrinkDAO();
    public abstract OrderDAO orderDAO();
    public abstract FavoriteDrinkDAO favoriteDrinkDAO();
    public abstract IngredientDAO ingredientDAO();
    public abstract CourierDAO courierDAO();
    public abstract DrinkDAO drinkDAO();
    public abstract DrinkIngredientDAO drinkIngredientDAO();
    public abstract VolumeDAO volumeDAO();
    public abstract ReviewDAO reviewDAO();
    public abstract PriceListDAO priceListDAO();
    public abstract OrderStatusDAO orderStatusDAO();
    public abstract DishDAO dishDAO();
    public abstract OrderedDishDAO orderedDishDAO();
    public abstract DessertDAO dessertDAO();
    public abstract OrderedDessertDAO orderedDessertDAO();

    /** Миграция 4 → 5 */
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `users_new` (" +
                    "`userId` TEXT NOT NULL, " +
                    "`firstName` TEXT, `lastName` TEXT, `email` TEXT, `phoneNumber` TEXT, " +
                    "`resetCode` TEXT, `password` TEXT, `profileImage` BLOB, " +
                    "PRIMARY KEY(`userId`))");
            db.execSQL("INSERT INTO `users_new` " +
                    "(userId, firstName, lastName, email, phoneNumber, resetCode, password, profileImage) " +
                    "SELECT userId, firstName, lastName, email, phoneNumber, resetCode, password, profileImage FROM users");
            db.execSQL("DROP TABLE users");
            db.execSQL("ALTER TABLE users_new RENAME TO users");
        }
    };

    /** Миграция 5 → 6 */
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            // … ваш SQL для price_list, Address, Order, Delivery, OrderedDrink, и т.д.
        }
    };

    /** Миграция 6 → 7 */
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `drinks` ADD COLUMN `imageUrl` TEXT");
        }
    };

    /** Миграция 7 → 8 */
    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `dishes` ADD COLUMN `imageUrl` TEXT");
            db.execSQL("ALTER TABLE `desserts` ADD COLUMN `imageUrl` TEXT");
        }
    };

    /** Миграция 8 → 9 */
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE volumes RENAME TO volumes_old");
            db.execSQL("CREATE TABLE IF NOT EXISTS volumes (" +
                    "volumeId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "size TEXT, " +
                    "ml INTEGER NOT NULL)");
            db.execSQL("INSERT INTO volumes (volumeId, size, ml) " +
                    "SELECT volumeId, volume, 0 FROM volumes_old");
            db.execSQL("DROP TABLE volumes_old");
        }
    };

    /** Миграция 9 → 10 */
    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `dishes`   ADD COLUMN `size` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `desserts` ADD COLUMN `size` INTEGER NOT NULL DEFAULT 0");
        }
    };

    /** Миграция 10 → 11: пересоздание orders и создание cart_items */
    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Переименование старой orders
            db.execSQL("ALTER TABLE `orders` RENAME TO `orders_old`");
            // Новая orders
            db.execSQL("CREATE TABLE IF NOT EXISTS `orders` (" +
                    "`orderId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`userId` TEXT, " +
                    "`totalPrice` REAL NOT NULL, " +
                    "`statusId` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`statusId`) REFERENCES `order_statuses`(`statusId`) " +
                    "ON UPDATE CASCADE ON DELETE RESTRICT" +
                    ")");
            db.execSQL("INSERT INTO `orders` (orderId, userId, totalPrice, statusId, createdAt) " +
                    "SELECT orderId, userId, totalAmount, orderStatusId, orderDate FROM orders_old");
            db.execSQL("DROP TABLE `orders_old`");

            // Справочник статусов
            db.execSQL("CREATE TABLE IF NOT EXISTS `order_statuses` (" +
                    "`statusId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`statusName` TEXT NOT NULL" +
                    ")");
            db.execSQL("INSERT INTO `order_statuses` (statusName) VALUES " +
                    "('В готовке'),('В доставке'),('Доставлен')");

            // Таблица корзины
            db.execSQL("CREATE TABLE IF NOT EXISTS `cart_items` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`productType` TEXT, " +
                    "`productId` INTEGER NOT NULL, " +
                    "`title` TEXT, " +
                    "`imageUrl` TEXT, " +
                    "`size` INTEGER NOT NULL, " +
                    "`unitPrice` REAL NOT NULL, " +
                    "`quantity` INTEGER NOT NULL" +
                    ")");
        }
    };

    /** Миграция 11 → 12: добавляем quantity в ordered_* (если нет) */
    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            addColumnIfNotExists(db, "ordered_drinks",   "quantity", "INTEGER NOT NULL DEFAULT 1");
            addColumnIfNotExists(db, "ordered_dishes",   "quantity", "INTEGER NOT NULL DEFAULT 1");
            addColumnIfNotExists(db, "ordered_desserts", "quantity", "INTEGER NOT NULL DEFAULT 1");
        }
        private void addColumnIfNotExists(SupportSQLiteDatabase db,
                                          String table,
                                          String column,
                                          String definition) {
            try (Cursor cursor = db.query("PRAGMA table_info(`" + table + "`)")) {
                boolean found = false;
                int nameIdx  = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (nameIdx >= 0 && column.equals(cursor.getString(nameIdx))) {
                        found = true; break;
                    }
                }
                if (!found) {
                    db.execSQL("ALTER TABLE `" + table + "` " +
                            "ADD COLUMN `" + column + "` " + definition);
                }
            }
        }
    };

    /** Миграция 12 → 13: полное пересоздание справочника и позиций, а также orders с дефолтным статусом */
    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // 1) Создаём справочник статусов заново (nullable statusName)
            db.execSQL("DROP TABLE IF EXISTS `order_statuses`");
            db.execSQL("CREATE TABLE IF NOT EXISTS `order_statuses` (" +
                    "`statusId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`statusName` TEXT" +    // <- Убрали NOT NULL
                    ")");

            // 2) Заполняем начальными значениями
            db.execSQL("INSERT INTO `order_statuses` (statusName) VALUES " +
                    "('В готовке'),('В доставке'),('Доставлен'),('Доставлен и оплачен')");

            // 3) Пересоздаём таблицы позиций заказа
            db.execSQL("DROP TABLE IF EXISTS `ordered_drinks`");
            db.execSQL("CREATE TABLE IF NOT EXISTS `ordered_drinks` (" +
                    "`orderedDrinkId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`orderId` INTEGER NOT NULL, " +
                    "`drinkId` INTEGER NOT NULL, " +
                    "`quantity` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`orderId`) REFERENCES `orders`(`orderId`) " +
                    "ON UPDATE CASCADE ON DELETE CASCADE" +
                    ")");
            // аналогично для ordered_dishes и ordered_desserts
            db.execSQL("DROP TABLE IF EXISTS `ordered_dishes`");
            db.execSQL("CREATE TABLE IF NOT EXISTS `ordered_dishes` (" +
                    "`orderedDishId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`orderId` INTEGER NOT NULL, " +
                    "`dishId` INTEGER NOT NULL, " +
                    "`quantity` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`orderId`) REFERENCES `orders`(`orderId`) " +
                    "ON UPDATE CASCADE ON DELETE CASCADE" +
                    ")");
            db.execSQL("DROP TABLE IF EXISTS `ordered_desserts`");
            db.execSQL("CREATE TABLE IF NOT EXISTS `ordered_desserts` (" +
                    "`orderedDessertId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`orderId` INTEGER NOT NULL, " +
                    "`dessertId` INTEGER NOT NULL, " +
                    "`quantity` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`orderId`) REFERENCES `orders`(`orderId`) " +
                    "ON UPDATE CASCADE ON DELETE CASCADE" +
                    ")");

            // 4) Пересоздаём таблицу orders
            db.execSQL("ALTER TABLE `orders` RENAME TO `orders_old`");
            db.execSQL("CREATE TABLE IF NOT EXISTS `orders` (" +
                    "`orderId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`userId` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`totalPrice` REAL NOT NULL, " +
                    "`statusId` INTEGER NOT NULL DEFAULT 1, " +
                    "FOREIGN KEY(`statusId`) REFERENCES `order_statuses`(`statusId`) " +
                    "ON UPDATE CASCADE ON DELETE RESTRICT" +
                    ")");
            db.execSQL("INSERT INTO `orders` (orderId, userId, createdAt, totalPrice, statusId) " +
                    "SELECT orderId, userId, createdAt, totalPrice, statusId FROM orders_old");
            db.execSQL("DROP TABLE orders_old");
        }
    };

    static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Добавляем колонку size в каждую из трёх таблиц
            db.execSQL("ALTER TABLE `ordered_drinks`   ADD COLUMN `size` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `ordered_dishes`   ADD COLUMN `size` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `ordered_desserts` ADD COLUMN `size` INTEGER NOT NULL DEFAULT 0");
        }
    };



    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "app_database"
                    )
                    .addMigrations(
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7,
                            MIGRATION_7_8,
                            MIGRATION_8_9,
                            MIGRATION_9_10,
                            MIGRATION_10_11,
                            MIGRATION_11_12,
                            MIGRATION_12_13,
                            MIGRATION_13_14
                    ).addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // seed statuses
                            db.execSQL("INSERT INTO `order_statuses` (statusName) VALUES " +
                                    "('В готовке'),('В доставке'),('Доставлен')");
                        }
                    })
                    .build();
        }
        return instance;
    }
}
