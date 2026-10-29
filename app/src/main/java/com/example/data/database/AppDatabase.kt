package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.InventoryDao
import com.example.data.model.*

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        BrandEntity::class,
        UnitEntity::class,
        GodownEntity::class,
        ItemEntity::class,
        StockLedgerEntity::class,
        SupplierEntity::class,
        PurchaseBillEntity::class,
        PurchaseBillItemEntity::class,
        CustomerEntity::class,
        SalesBillEntity::class,
        SalesBillItemEntity::class,
        CompanyProfileEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sampad_santra_inventory.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
