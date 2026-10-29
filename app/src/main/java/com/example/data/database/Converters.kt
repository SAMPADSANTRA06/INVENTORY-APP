package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.*

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (e: Exception) {
        UserRole.STAFF
    }

    @TypeConverter
    fun fromMovementType(value: MovementType): String = value.name

    @TypeConverter
    fun toMovementType(value: String): MovementType = try {
        MovementType.valueOf(value)
    } catch (e: Exception) {
        MovementType.ADJUSTMENT_ADD
    }

    @TypeConverter
    fun fromPaymentMode(value: PaymentMode): String = value.name

    @TypeConverter
    fun toPaymentMode(value: String): PaymentMode = try {
        PaymentMode.valueOf(value)
    } catch (e: Exception) {
        PaymentMode.CASH
    }

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String = value.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus = try {
        PaymentStatus.valueOf(value)
    } catch (e: Exception) {
        PaymentStatus.PAID
    }

    @TypeConverter
    fun fromBillStatus(value: BillStatus): String = value.name

    @TypeConverter
    fun toBillStatus(value: String): BillStatus = try {
        BillStatus.valueOf(value)
    } catch (e: Exception) {
        BillStatus.COMPLETED
    }

    @TypeConverter
    fun fromValuationMethod(value: ValuationMethod): String = value.name

    @TypeConverter
    fun toValuationMethod(value: String): ValuationMethod = try {
        ValuationMethod.valueOf(value)
    } catch (e: Exception) {
        ValuationMethod.WEIGHTED_AVERAGE
    }
}
